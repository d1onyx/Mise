package com.dishlab.application.service

import com.dishlab.domain.error.ForbiddenError
import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.CookingSession
import com.dishlab.domain.model.CookingSessionStatus
import com.dishlab.domain.model.CookingTimer
import com.dishlab.domain.model.NutritionLog
import com.dishlab.domain.model.RecipeIngredient
import com.dishlab.domain.model.TimerStatus
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface CookingRepository {
    fun saveSession(session: CookingSession): CookingSession
    fun findSession(sessionId: UUID): CookingSession?
    fun saveTimer(timer: CookingTimer): CookingTimer
    fun findTimer(timerId: UUID): CookingTimer?
    fun deleteTimer(timerId: UUID): CookingTimer?
    fun listTimers(sessionId: UUID): List<CookingTimer>
    fun saveNutritionLog(log: NutritionLog): NutritionLog
    fun findNutritionLogBySession(sessionId: UUID): NutritionLog?
}

class InMemoryCookingRepository : CookingRepository {
    private val sessions = ConcurrentHashMap<UUID, CookingSession>()
    private val timers = ConcurrentHashMap<UUID, CookingTimer>()
    private val nutritionLogs = ConcurrentHashMap<UUID, NutritionLog>()

    override fun saveSession(session: CookingSession): CookingSession {
        val saved = session.copy(updatedAt = Instant.now())
        sessions[saved.id] = saved
        return saved
    }

    override fun findSession(sessionId: UUID): CookingSession? = sessions[sessionId]

    override fun saveTimer(timer: CookingTimer): CookingTimer {
        val saved = timer.copy(updatedAt = Instant.now())
        timers[saved.id] = saved
        return saved
    }

    override fun findTimer(timerId: UUID): CookingTimer? = timers[timerId]

    override fun deleteTimer(timerId: UUID): CookingTimer? = timers.remove(timerId)

    override fun listTimers(sessionId: UUID): List<CookingTimer> = timers.values.filter { it.sessionId == sessionId }.sortedBy { it.createdAt }

    override fun saveNutritionLog(log: NutritionLog): NutritionLog {
        nutritionLogs[log.id] = log
        return log
    }

    override fun findNutritionLogBySession(sessionId: UUID): NutritionLog? = nutritionLogs.values.firstOrNull { it.cookingSessionId == sessionId }
}

data class CookingSessionInput(
    val recipeId: UUID? = null,
    val servings: Int? = null,
)

data class CookingSessionUpdateInput(
    val currentStepPosition: Int? = null,
    val status: CookingSessionStatus? = null,
    val notes: String? = null,
)

data class CookingTimerInput(
    val sessionId: UUID? = null,
    val label: String? = null,
    val durationSeconds: Int? = null,
    val stepPosition: Int? = null,
)

data class CookingTimerUpdateInput(
    val remainingSeconds: Int? = null,
    val status: TimerStatus? = null,
    val label: String? = null,
)

data class CompleteCookingSessionInput(
    val deductInventory: Boolean = true,
    val createNutritionLog: Boolean = false,
    val notes: String? = null,
)

data class CookingSessionDetails(
    val session: CookingSession,
    val timers: List<CookingTimer>,
    val nutritionLog: NutritionLog? = null,
)

data class CompleteCookingSessionResult(
    val session: CookingSession,
    val inventoryTransactions: List<com.dishlab.domain.model.PantryTransaction>,
    val nutritionLog: NutritionLog?,
)

class CookingService(
    private val currentUserResolver: CurrentUserResolver,
    private val recipeService: RecipeService,
    private val pantryService: PantryService,
    private val repository: CookingRepository,
) {
    fun start(firebaseUid: String, input: CookingSessionInput): CookingSessionDetails {
        val actor = currentUserResolver.resolve(firebaseUid)
        val recipeId = input.recipeId ?: throw ValidationError(details = mapOf("recipeId" to "Recipe is required"))
        val recipe = recipeService.get(firebaseUid, recipeId)
        val session = repository.saveSession(
            CookingSession(
                id = UUID.randomUUID(),
                userId = actor.id,
                recipeId = recipe.id,
                recipeVersionId = recipe.currentVersion.id,
                recipeTitle = recipe.currentVersion.title,
                servings = validateServings(input.servings ?: recipe.currentVersion.servings),
                currentStepPosition = recipe.currentVersion.steps.minOfOrNull { it.position } ?: 1,
            ),
        )
        return details(firebaseUid, session.id)
    }

    fun details(firebaseUid: String, sessionId: UUID): CookingSessionDetails {
        val currentUser = currentUserResolver.resolve(firebaseUid)
        val session = findSession(sessionId)
        if (session.userId != currentUser.id) throw ForbiddenError()
        return CookingSessionDetails(session, repository.listTimers(session.id), repository.findNutritionLogBySession(session.id))
    }

    fun update(firebaseUid: String, sessionId: UUID, input: CookingSessionUpdateInput): CookingSessionDetails {
        val currentUser = currentUserResolver.resolve(firebaseUid)
        val current = findSession(sessionId)
        if (current.userId != currentUser.id) throw ForbiddenError()
        val updated = repository.saveSession(
            current.copy(
                currentStepPosition = input.currentStepPosition?.let { validateStep(it) } ?: current.currentStepPosition,
                status = input.status ?: current.status,
                notes = input.notes ?: current.notes,
            ),
        )
        return CookingSessionDetails(updated, repository.listTimers(updated.id), repository.findNutritionLogBySession(updated.id))
    }

    fun complete(firebaseUid: String, sessionId: UUID, input: CompleteCookingSessionInput): CompleteCookingSessionResult {
        val currentUser = currentUserResolver.resolve(firebaseUid)
        val current = findSession(sessionId)
        if (current.userId != currentUser.id) throw ForbiddenError()
        val recipe = recipeService.get(firebaseUid, current.recipeId)
        val transactions = if (input.deductInventory) deductInventory(firebaseUid, current, recipe.currentVersion.ingredients, recipe.currentVersion.servings) else emptyList()
        val completed = repository.saveSession(
            current.copy(
                status = CookingSessionStatus.COMPLETED,
                completedAt = Instant.now(),
                notes = input.notes ?: current.notes,
            ),
        )
        val nutritionLog = if (input.createNutritionLog) {
            repository.findNutritionLogBySession(completed.id) ?: repository.saveNutritionLog(
                NutritionLog(
                    id = UUID.randomUUID(),
                    userId = completed.userId,
                    recipeId = completed.recipeId,
                    cookingSessionId = completed.id,
                    recipeTitle = completed.recipeTitle,
                    servings = completed.servings,
                ),
            )
        } else {
            repository.findNutritionLogBySession(completed.id)
        }
        return CompleteCookingSessionResult(completed, transactions, nutritionLog)
    }

    fun createTimer(firebaseUid: String, input: CookingTimerInput): CookingTimer {
        val actor = currentUserResolver.resolve(firebaseUid)
        val sessionId = input.sessionId ?: throw ValidationError(details = mapOf("sessionId" to "Session is required"))
        val session = findSession(sessionId)
        if (session.userId != actor.id) throw ForbiddenError()
        val duration = validateSeconds(input.durationSeconds, "durationSeconds")
        return repository.saveTimer(
            CookingTimer(
                id = UUID.randomUUID(),
                sessionId = session.id,
                userId = actor.id,
                label = validateLabel(input.label),
                durationSeconds = duration,
                remainingSeconds = duration,
                stepPosition = input.stepPosition,
            ),
        )
    }

    fun updateTimer(firebaseUid: String, timerId: UUID, input: CookingTimerUpdateInput): CookingTimer {
        val currentUser = currentUserResolver.resolve(firebaseUid)
        val current = findTimer(timerId)
        if (current.userId != currentUser.id) throw ForbiddenError()
        return repository.saveTimer(
            current.copy(
                label = input.label?.let { validateLabel(it) } ?: current.label,
                remainingSeconds = input.remainingSeconds?.let { validateSeconds(it, "remainingSeconds") } ?: current.remainingSeconds,
                status = input.status ?: current.status,
            ),
        )
    }

    fun deleteTimer(firebaseUid: String, timerId: UUID): CookingTimer {
        val currentUser = currentUserResolver.resolve(firebaseUid)
        val current = findTimer(timerId)
        if (current.userId != currentUser.id) throw ForbiddenError()
        return repository.deleteTimer(timerId) ?: throw NotFoundError("Таймер не знайдено")
    }

    private fun deductInventory(
        firebaseUid: String,
        session: CookingSession,
        ingredients: List<RecipeIngredient>,
        recipeServings: Int,
    ): List<com.dishlab.domain.model.PantryTransaction> {
        val transactions = mutableListOf<com.dishlab.domain.model.PantryTransaction>()
        ingredients.forEach { ingredient ->
            var remaining = ingredient.amount * session.servings / recipeServings.coerceAtLeast(1).toDouble()
            val candidates = pantryService.list(firebaseUid, PantryItemFilter(includeDeleted = false)).items
                .filter { !it.depleted && sameIngredient(it.ingredientId, it.name, ingredient.ingredientId, ingredient.name) && it.unit.equals(ingredient.unit, ignoreCase = true) }
                .sortedBy { it.expiryDate }
            val available = candidates.sumOf { it.quantity }
            if (available + 0.000001 < remaining) throw ValidationError(details = mapOf("inventory" to "Not enough ${ingredient.name}"))
            candidates.forEach { item ->
                if (remaining > 0.0) {
                    val amount = item.quantity.coerceAtMost(remaining)
                    val result = pantryService.consume(
                        firebaseUid = firebaseUid,
                        itemId = item.id,
                        quantity = amount,
                        reason = "cooking_session_completed",
                        recipeId = session.recipeId,
                    )
                    transactions.add(result.transaction)
                    remaining -= amount
                }
            }
        }
        return transactions
    }

    private fun findSession(sessionId: UUID): CookingSession = repository.findSession(sessionId) ?: throw NotFoundError("Кулінарну сесію не знайдено")
    private fun findTimer(timerId: UUID): CookingTimer = repository.findTimer(timerId) ?: throw NotFoundError("Таймер не знайдено")

    private fun sameIngredient(leftId: UUID?, leftName: String, rightId: UUID?, rightName: String): Boolean =
        if (leftId != null && rightId != null) leftId == rightId else leftName.equals(rightName, ignoreCase = true)

    private fun validateServings(value: Int): Int {
        if (value !in 1..100) throw ValidationError(details = mapOf("servings" to "Servings must be between 1 and 100"))
        return value
    }

    private fun validateStep(value: Int): Int {
        if (value < 1) throw ValidationError(details = mapOf("currentStepPosition" to "Step position must be positive"))
        return value
    }

    private fun validateLabel(value: String?): String {
        val clean = value?.trim().orEmpty()
        if (clean.length !in 2..120) throw ValidationError(details = mapOf("label" to "Label must be between 2 and 120 characters"))
        return clean
    }

    private fun validateSeconds(value: Int?, field: String): Int {
        val seconds = value ?: throw ValidationError(details = mapOf(field to "$field is required"))
        if (seconds !in 1..86_400) throw ValidationError(details = mapOf(field to "$field must be between 1 and 86400"))
        return seconds
    }
}
