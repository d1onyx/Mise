package com.dishlab.api.dto

import com.dishlab.application.service.ExpiringPantryGroups
import com.dishlab.application.service.PantryActionResult
import com.dishlab.application.service.PantryItemInput
import com.dishlab.application.service.PantryItemListResult
import com.dishlab.domain.model.PantryItem
import com.dishlab.domain.model.PantryTransaction
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class PantryItemRequest(
    val ingredientId: String? = null,
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val location: String? = null,
    val category: String? = null,
    val expiryDate: String? = null,
)

@Serializable
data class PantryItemResponse(
    val id: String,
    val userId: String,
    val ingredientId: String? = null,
    val name: String,
    val quantity: Double,
    val unit: String,
    val location: String,
    val category: String,
    val expiryDate: String? = null,
    val expiryStatus: String,
    val depleted: Boolean,
    val deleted: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class PantryItemsResponse(val items: List<PantryItemResponse>, val total: Int)

@Serializable
data class PantryTransactionResponse(
    val id: String,
    val userId: String,
    val itemId: String,
    val actorUserId: String,
    val type: String,
    val quantityDelta: Double,
    val unit: String,
    val reason: String? = null,
    val recipeId: String? = null,
    val createdAt: String,
)

@Serializable
data class PantryTransactionsResponse(val items: List<PantryTransactionResponse>, val total: Int)

@Serializable
data class PantryActionRequest(
    val quantity: Double,
    val reason: String? = null,
    val recipeId: String? = null,
)

@Serializable
data class FreezeInventoryRequest(val extraDays: Long = 90)

@Serializable
data class PantryActionResponse(
    val item: PantryItemResponse,
    val transaction: PantryTransactionResponse,
    val depleted: Boolean,
)

@Serializable
data class DeletePantryItemResponse(val id: String, val deleted: Boolean)

@Serializable
data class ExpiringPantryResponse(
    val expired: List<PantryItemResponse>,
    val today: List<PantryItemResponse>,
    val soon: List<PantryItemResponse>,
    val week: List<PantryItemResponse>,
)

fun PantryItemRequest.toInput(): PantryItemInput = PantryItemInput(
    ingredientId = ingredientId?.let { UUID.fromString(it) },
    name = name,
    quantity = quantity,
    unit = unit,
    location = location,
    category = category,
    expiryDate = expiryDate?.let { LocalDate.parse(it) },
)

fun PantryItem.toResponse(): PantryItemResponse = PantryItemResponse(
    id = id.toString(),
    userId = userId.toString(),
    ingredientId = ingredientId?.toString(),
    name = name,
    quantity = quantity,
    unit = unit,
    location = location,
    category = category,
    expiryDate = expiryDate?.toString(),
    expiryStatus = expiryStatus().name,
    depleted = depleted,
    deleted = deleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun PantryItemListResult.toResponse(): PantryItemsResponse = PantryItemsResponse(items.map { it.toResponse() }, total)

fun PantryTransaction.toResponse(): PantryTransactionResponse = PantryTransactionResponse(
    id = id.toString(),
    userId = userId.toString(),
    itemId = itemId.toString(),
    actorUserId = actorUserId.toString(),
    type = type.name,
    quantityDelta = quantityDelta,
    unit = unit,
    reason = reason,
    recipeId = recipeId?.toString(),
    createdAt = createdAt.toString(),
)

fun PantryActionResult.toResponse(): PantryActionResponse = PantryActionResponse(
    item = item.toResponse(),
    transaction = transaction.toResponse(),
    depleted = depleted,
)

fun ExpiringPantryGroups.toResponse(): ExpiringPantryResponse = ExpiringPantryResponse(
    expired = expired.map { it.toResponse() },
    today = today.map { it.toResponse() },
    soon = soon.map { it.toResponse() },
    week = week.map { it.toResponse() },
)
