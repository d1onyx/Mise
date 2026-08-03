package com.dishlab.application.service

import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.PantryExpiryStatus
import com.dishlab.domain.model.PantryItem
import com.dishlab.domain.model.PantryTransaction
import com.dishlab.domain.model.PantryTransactionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface PantryRepository {
    fun saveItem(item: PantryItem): PantryItem
    fun findItem(userId: UUID, itemId: UUID): PantryItem?
    fun listItems(userId: UUID): List<PantryItem>
    fun saveTransaction(transaction: PantryTransaction): PantryTransaction
    fun listTransactions(userId: UUID): List<PantryTransaction>
}

class InMemoryPantryRepository : PantryRepository {
    private val items = ConcurrentHashMap<UUID, PantryItem>()
    private val transactions = ConcurrentHashMap<UUID, MutableList<PantryTransaction>>()

    override fun saveItem(item: PantryItem): PantryItem {
        val saved = item.copy(updatedAt = Instant.now())
        items[saved.id] = saved
        return saved
    }

    override fun findItem(userId: UUID, itemId: UUID): PantryItem? = items[itemId]?.takeIf { it.userId == userId }

    override fun listItems(userId: UUID): List<PantryItem> = items.values.filter { it.userId == userId }.sortedBy { it.name.lowercase() }

    override fun saveTransaction(transaction: PantryTransaction): PantryTransaction {
        transactions.computeIfAbsent(transaction.userId) { mutableListOf() }.add(transaction)
        return transaction
    }

    override fun listTransactions(userId: UUID): List<PantryTransaction> = transactions[userId].orEmpty().sortedByDescending { it.createdAt }
}

data class PantryItemInput(
    val ingredientId: UUID? = null,
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val location: String? = null,
    val category: String? = null,
    val expiryDate: LocalDate? = null,
)

data class PantryItemFilter(
    val location: String? = null,
    val category: String? = null,
    val expiry: String? = null,
    val search: String? = null,
    val includeDeleted: Boolean = false,
)

data class PantryItemListResult(val items: List<PantryItem>, val total: Int)

data class PantryActionResult(val item: PantryItem, val transaction: PantryTransaction, val depleted: Boolean)

data class ExpiringPantryGroups(
    val expired: List<PantryItem>,
    val today: List<PantryItem>,
    val soon: List<PantryItem>,
    val week: List<PantryItem>,
)

class PantryService(
    private val currentUserResolver: CurrentUserResolver,
    private val pantry: PantryRepository,
    private val productValidationProvider: ProductValidationProvider = NoOpProductValidationProvider(),
) {
    fun list(firebaseUid: String, filter: PantryItemFilter): PantryItemListResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val today = LocalDate.now()
        val items = pantry.listItems(user.id)
            .filter { filter.includeDeleted || !it.deleted }
            .filter { filter.location.isNullOrBlank() || it.location.equals(filter.location, ignoreCase = true) }
            .filter { filter.category.isNullOrBlank() || it.category.equals(filter.category, ignoreCase = true) }
            .filter { filter.search.isNullOrBlank() || it.name.contains(filter.search, ignoreCase = true) }
            .filter { matchesExpiry(it, filter.expiry, today) }
        return PantryItemListResult(items, items.size)
    }

    suspend fun add(firebaseUid: String, input: PantryItemInput): PantryItem {
        val user = currentUserResolver.resolve(firebaseUid)
        val name = validateName(input.name)
        if (!productValidationProvider.validate(name)) {
            throw ValidationError(details = mapOf("name" to "Назва має бути реальним харчовим продуктом або інгредієнтом"))
        }
        val item = PantryItem(
            id = UUID.randomUUID(),
            userId = user.id,
            ingredientId = input.ingredientId,
            name = name,
            quantity = validateQuantity(input.quantity),
            unit = validateRequired(input.unit, "unit"),
            location = validateRequired(input.location, "location"),
            category = validateRequired(input.category, "category"),
            expiryDate = input.expiryDate,
        )
        val saved = pantry.saveItem(item)
        record(user.id, saved, PantryTransactionType.ADD, saved.quantity, "manual_add", null)
        return saved
    }

    suspend fun update(firebaseUid: String, itemId: UUID, input: PantryItemInput): PantryItem {
        val user = currentUserResolver.resolve(firebaseUid)
        val current = findExisting(user.id, itemId)
        val newName = input.name?.let {
            val validated = validateName(it)
            if (!productValidationProvider.validate(validated)) {
                throw ValidationError(details = mapOf("name" to "Назва має бути реальним харчовим продуктом або інгредієнтом"))
            }
            validated
        }
        val updated = pantry.saveItem(
            current.copy(
                ingredientId = input.ingredientId ?: current.ingredientId,
                name = newName ?: current.name,
                quantity = input.quantity?.let { validateQuantity(it) } ?: current.quantity,
                unit = input.unit?.let { validateRequired(it, "unit") } ?: current.unit,
                location = input.location?.let { validateRequired(it, "location") } ?: current.location,
                category = input.category?.let { validateRequired(it, "category") } ?: current.category,
                expiryDate = input.expiryDate ?: current.expiryDate,
            ),
        )
        record(user.id, updated, PantryTransactionType.UPDATE, updated.quantity - current.quantity, "manual_update", null)
        return updated
    }

    fun tombstone(firebaseUid: String, itemId: UUID): PantryItem {
        val user = currentUserResolver.resolve(firebaseUid)
        val item = pantry.saveItem(findExisting(user.id, itemId).copy(deleted = true))
        record(user.id, item, PantryTransactionType.DELETE, 0.0, "tombstone", null)
        return item
    }

    fun consume(firebaseUid: String, itemId: UUID, quantity: Double, reason: String?, recipeId: UUID?): PantryActionResult =
        reduce(firebaseUid, itemId, quantity, PantryTransactionType.CONSUME, reason, recipeId)

    fun waste(firebaseUid: String, itemId: UUID, quantity: Double, reason: String?): PantryActionResult =
        reduce(firebaseUid, itemId, quantity, PantryTransactionType.WASTE, reason, null)

    fun freeze(firebaseUid: String, itemId: UUID, extraDays: Long): PantryActionResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val current = findExisting(user.id, itemId)
        val baseDate = current.expiryDate ?: LocalDate.now()
        val updated = pantry.saveItem(current.copy(location = "freezer", expiryDate = baseDate.plusDays(extraDays.coerceIn(1, 365))))
        val transaction = record(user.id, updated, PantryTransactionType.FREEZE, 0.0, "freeze", null)
        return PantryActionResult(updated, transaction, updated.depleted)
    }

    fun expiring(firebaseUid: String): ExpiringPantryGroups {
        val user = currentUserResolver.resolve(firebaseUid)
        val active = pantry.listItems(user.id).filter { !it.deleted && !it.depleted }
        return ExpiringPantryGroups(
            expired = active.filter { it.expiryStatus() == PantryExpiryStatus.EXPIRED },
            today = active.filter { it.expiryStatus() == PantryExpiryStatus.TODAY },
            soon = active.filter { it.expiryStatus() == PantryExpiryStatus.SOON },
            week = active.filter { it.expiryStatus() == PantryExpiryStatus.WEEK },
        )
    }

    fun transactions(firebaseUid: String): List<PantryTransaction> {
        val user = currentUserResolver.resolve(firebaseUid)
        return pantry.listTransactions(user.id)
    }

    private fun reduce(
        firebaseUid: String,
        itemId: UUID,
        quantity: Double,
        type: PantryTransactionType,
        reason: String?,
        recipeId: UUID?,
    ): PantryActionResult {
        val user = currentUserResolver.resolve(firebaseUid)
        if (quantity <= 0.0) throw ValidationError(details = mapOf("quantity" to "Quantity must be positive"))
        val current = findExisting(user.id, itemId)
        if (quantity > current.quantity) throw ValidationError(details = mapOf("quantity" to "Quantity cannot exceed current item quantity"))
        val updated = pantry.saveItem(current.copy(quantity = current.quantity - quantity))
        val transaction = record(user.id, updated, type, -quantity, reason, recipeId)
        return PantryActionResult(updated, transaction, updated.depleted)
    }

    private fun findExisting(userId: UUID, itemId: UUID): PantryItem =
        pantry.findItem(userId, itemId)?.takeIf { !it.deleted } ?: throw NotFoundError("Позицію інвентарю не знайдено")

    private fun record(actorUserId: UUID, item: PantryItem, type: PantryTransactionType, delta: Double, reason: String?, recipeId: UUID?): PantryTransaction =
        pantry.saveTransaction(
            PantryTransaction(
                id = UUID.randomUUID(),
                userId = item.userId,
                itemId = item.id,
                actorUserId = actorUserId,
                type = type,
                quantityDelta = delta,
                unit = item.unit,
                reason = reason,
                recipeId = recipeId,
            ),
        )

    private fun matchesExpiry(item: PantryItem, expiry: String?, today: LocalDate): Boolean = when (expiry?.trim()?.lowercase()) {
        null, "" -> true
        "expired" -> item.expiryStatus(today) == PantryExpiryStatus.EXPIRED
        "today" -> item.expiryStatus(today) == PantryExpiryStatus.TODAY
        "soon", "1-2", "1-2-days" -> item.expiryStatus(today) == PantryExpiryStatus.SOON
        "week" -> item.expiryStatus(today) in setOf(PantryExpiryStatus.SOON, PantryExpiryStatus.WEEK)
        else -> true
    }

    private fun validateName(value: String?): String {
        val clean = value?.trim().orEmpty()
        if (clean.length !in 2..120) throw ValidationError(details = mapOf("name" to "Name must be between 2 and 120 characters"))
        return clean
    }

    private fun validateQuantity(value: Double?): Double {
        val quantity = value ?: throw ValidationError(details = mapOf("quantity" to "Quantity is required"))
        if (quantity <= 0.0) throw ValidationError(details = mapOf("quantity" to "Quantity must be positive"))
        return quantity
    }

    private fun validateRequired(value: String?, field: String): String {
        val clean = value?.trim().orEmpty()
        if (clean.isBlank()) throw ValidationError(details = mapOf(field to "$field is required"))
        return clean
    }
}
