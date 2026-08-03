package com.dishlab.api.dto

import com.dishlab.application.service.CompleteCookingSessionInput
import com.dishlab.application.service.CompleteCookingSessionResult
import com.dishlab.application.service.CookingSessionDetails
import com.dishlab.application.service.CookingSessionInput
import com.dishlab.application.service.CookingSessionUpdateInput
import com.dishlab.application.service.CookingTimerInput
import com.dishlab.application.service.CookingTimerUpdateInput
import com.dishlab.domain.model.CookingSession
import com.dishlab.domain.model.CookingSessionStatus
import com.dishlab.domain.model.CookingTimer
import com.dishlab.domain.model.NutritionLog
import com.dishlab.domain.model.PantryTransaction
import com.dishlab.domain.model.TimerStatus
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class StartCookingSessionRequest(
    val recipeId: String? = null,
    val servings: Int? = null,
)

@Serializable
data class UpdateCookingSessionRequest(
    val currentStepPosition: Int? = null,
    val status: String? = null,
    val notes: String? = null,
)

@Serializable
data class CompleteCookingSessionRequest(
    val deductInventory: Boolean = true,
    val createNutritionLog: Boolean = false,
    val notes: String? = null,
)

@Serializable
data class CreateTimerRequest(
    val sessionId: String? = null,
    val label: String? = null,
    val durationSeconds: Int? = null,
    val stepPosition: Int? = null,
)

@Serializable
data class UpdateTimerRequest(
    val remainingSeconds: Int? = null,
    val status: String? = null,
    val label: String? = null,
)

@Serializable
data class CookingSessionResponse(
    val id: String,
    val userId: String,
    val recipeId: String,
    val recipeVersionId: String,
    val recipeTitle: String,
    val servings: Int,
    val currentStepPosition: Int,
    val status: String,
    val startedAt: String,
    val completedAt: String? = null,
    val notes: String? = null,
    val timers: List<CookingTimerResponse> = emptyList(),
    val nutritionLog: NutritionLogResponse? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CookingTimerResponse(
    val id: String,
    val sessionId: String,
    val userId: String,
    val label: String,
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val stepPosition: Int? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class NutritionLogResponse(
    val id: String,
    val userId: String,
    val recipeId: String,
    val cookingSessionId: String,
    val recipeTitle: String,
    val servings: Int,
    val loggedAt: String,
    val logDate: String,
)

@Serializable
data class CompleteCookingSessionResponse(
    val session: CookingSessionResponse,
    val inventoryTransactions: List<PantryTransactionResponse>,
    val nutritionLog: NutritionLogResponse? = null,
)

@Serializable
data class DeleteTimerResponse(val id: String, val deleted: Boolean)

fun StartCookingSessionRequest.toInput(): CookingSessionInput = CookingSessionInput(
    recipeId = recipeId?.let { UUID.fromString(it) },
    servings = servings,
)

fun UpdateCookingSessionRequest.toInput(): CookingSessionUpdateInput = CookingSessionUpdateInput(
    currentStepPosition = currentStepPosition,
    status = status?.let { CookingSessionStatus.valueOf(it.uppercase()) },
    notes = notes,
)

fun CompleteCookingSessionRequest.toInput(): CompleteCookingSessionInput = CompleteCookingSessionInput(
    deductInventory = deductInventory,
    createNutritionLog = createNutritionLog,
    notes = notes,
)

fun CreateTimerRequest.toInput(): CookingTimerInput = CookingTimerInput(
    sessionId = sessionId?.let { UUID.fromString(it) },
    label = label,
    durationSeconds = durationSeconds,
    stepPosition = stepPosition,
)

fun UpdateTimerRequest.toInput(): CookingTimerUpdateInput = CookingTimerUpdateInput(
    remainingSeconds = remainingSeconds,
    status = status?.let { TimerStatus.valueOf(it.uppercase()) },
    label = label,
)

fun CookingSessionDetails.toResponse(): CookingSessionResponse = session.toResponse(timers, nutritionLog)

fun CookingSession.toResponse(timers: List<CookingTimer> = emptyList(), nutritionLog: NutritionLog? = null): CookingSessionResponse = CookingSessionResponse(
    id = id.toString(),
    userId = userId.toString(),
    recipeId = recipeId.toString(),
    recipeVersionId = recipeVersionId.toString(),
    recipeTitle = recipeTitle,
    servings = servings,
    currentStepPosition = currentStepPosition,
    status = status.name,
    startedAt = startedAt.toString(),
    completedAt = completedAt?.toString(),
    notes = notes,
    timers = timers.map { it.toResponse() },
    nutritionLog = nutritionLog?.toResponse(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CookingTimer.toResponse(): CookingTimerResponse = CookingTimerResponse(
    id = id.toString(),
    sessionId = sessionId.toString(),
    userId = userId.toString(),
    label = label,
    durationSeconds = durationSeconds,
    remainingSeconds = remainingSeconds,
    stepPosition = stepPosition,
    status = status.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun NutritionLog.toResponse(): NutritionLogResponse = NutritionLogResponse(
    id = id.toString(),
    userId = userId.toString(),
    recipeId = recipeId.toString(),
    cookingSessionId = cookingSessionId.toString(),
    recipeTitle = recipeTitle,
    servings = servings,
    loggedAt = loggedAt.toString(),
    logDate = logDate.toString(),
)

fun CompleteCookingSessionResult.toResponse(): CompleteCookingSessionResponse = CompleteCookingSessionResponse(
    session = session.toResponse(nutritionLog = nutritionLog),
    inventoryTransactions = inventoryTransactions.map { it.toResponse() },
    nutritionLog = nutritionLog?.toResponse(),
)
