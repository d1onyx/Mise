package com.dishlab.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class PantryTransactionType { ADD, UPDATE, CONSUME, WASTE, FREEZE, DELETE }
enum class PantryExpiryStatus { EXPIRED, TODAY, SOON, WEEK, FRESH, UNKNOWN }

data class PantryItem(
    val id: UUID,
    val userId: UUID,
    val ingredientId: UUID? = null,
    val name: String,
    val quantity: Double,
    val unit: String,
    val location: String,
    val category: String,
    val expiryDate: LocalDate? = null,
    val deleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
) {
    val depleted: Boolean get() = quantity <= 0.0

    fun expiryStatus(today: LocalDate = LocalDate.now()): PantryExpiryStatus = when {
        expiryDate == null -> PantryExpiryStatus.UNKNOWN
        expiryDate.isBefore(today) -> PantryExpiryStatus.EXPIRED
        expiryDate.isEqual(today) -> PantryExpiryStatus.TODAY
        !expiryDate.isAfter(today.plusDays(2)) -> PantryExpiryStatus.SOON
        !expiryDate.isAfter(today.plusDays(7)) -> PantryExpiryStatus.WEEK
        else -> PantryExpiryStatus.FRESH
    }
}

data class PantryTransaction(
    val id: UUID,
    val userId: UUID,
    val itemId: UUID,
    val actorUserId: UUID,
    val type: PantryTransactionType,
    val quantityDelta: Double,
    val unit: String,
    val reason: String? = null,
    val recipeId: UUID? = null,
    val createdAt: Instant = Instant.now(),
)
