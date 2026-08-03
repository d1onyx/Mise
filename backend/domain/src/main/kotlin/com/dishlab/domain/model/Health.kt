package com.dishlab.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class MealLogType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class PhotoNutritionJobStatus { PENDING, PROCESSING, COMPLETED, FAILED }

data class HealthNutritionLog(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val mealType: MealLogType,
    val title: String,
    val calories: Double,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val incomplete: Boolean = false,
    val warning: String? = null,
    val deleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class WaterLog(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val amountMl: Int,
    val createdAt: Instant = Instant.now(),
)

data class WeightLog(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val weightKg: Double,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class PhotoNutritionJob(
    val id: UUID,
    val userId: UUID,
    val imageUrl: String,
    val status: PhotoNutritionJobStatus = PhotoNutritionJobStatus.PENDING,
    val requiresReview: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class NutritionTotals(
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val incompleteCount: Int,
    val waterMl: Int,
    val latestWeightKg: Double?,
)
