package com.dishlab.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class CookingSessionStatus { IN_PROGRESS, PAUSED, COMPLETED, CANCELLED }
enum class TimerStatus { RUNNING, PAUSED, DONE, CANCELLED }

data class CookingSession(
    val id: UUID,
    val userId: UUID,
    val recipeId: UUID,
    val recipeVersionId: UUID,
    val recipeTitle: String,
    val servings: Int,
    val currentStepPosition: Int = 1,
    val status: CookingSessionStatus = CookingSessionStatus.IN_PROGRESS,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val notes: String? = null,
    val createdAt: Instant = startedAt,
    val updatedAt: Instant = createdAt,
)

data class CookingTimer(
    val id: UUID,
    val sessionId: UUID,
    val userId: UUID,
    val label: String,
    val durationSeconds: Int,
    val remainingSeconds: Int = durationSeconds,
    val stepPosition: Int? = null,
    val status: TimerStatus = TimerStatus.RUNNING,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class NutritionLog(
    val id: UUID,
    val userId: UUID,
    val recipeId: UUID,
    val cookingSessionId: UUID,
    val recipeTitle: String,
    val servings: Int,
    val loggedAt: Instant = Instant.now(),
    val logDate: LocalDate = LocalDate.now(),
)
