package com.dishlab.api.dto

import com.dishlab.application.service.DashboardResult
import com.dishlab.application.service.NutritionLogInput
import com.dishlab.application.service.NutritionLogsResult
import com.dishlab.application.service.PhotoNutritionJobInput
import com.dishlab.application.service.WaterLogInput
import com.dishlab.application.service.WeightLogInput
import com.dishlab.application.service.WeightLogsResult
import com.dishlab.domain.model.HealthNutritionLog
import com.dishlab.domain.model.MealLogType
import com.dishlab.domain.model.NutritionTotals
import com.dishlab.domain.model.PhotoNutritionJob
import com.dishlab.domain.model.WaterLog
import com.dishlab.domain.model.WeightLog
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class NutritionLogRequest(
    val date: String? = null,
    val mealType: String? = null,
    val title: String? = null,
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val incomplete: Boolean? = null,
    val warning: String? = null,
)

@Serializable
data class HealthNutritionLogResponse(
    val id: String,
    val userId: String,
    val date: String,
    val mealType: String,
    val title: String,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val incomplete: Boolean,
    val warning: String? = null,
    val deleted: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class NutritionLogsResponse(val items: List<HealthNutritionLogResponse>, val total: Int)

@Serializable
data class DeleteNutritionLogResponse(val id: String, val deleted: Boolean)

@Serializable
data class NutritionTotalsResponse(
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val incompleteCount: Int,
    val waterMl: Int,
    val latestWeightKg: Double? = null,
)

@Serializable
data class NutritionDashboardResponse(
    val day: NutritionTotalsResponse,
    val week: NutritionTotalsResponse,
    val month: NutritionTotalsResponse,
    val year: NutritionTotalsResponse,
)

@Serializable
data class WaterLogRequest(val amountMl: Int? = null, val date: String? = null)

@Serializable
data class WaterLogResponse(
    val id: String,
    val userId: String,
    val date: String,
    val amountMl: Int,
    val createdAt: String,
)

@Serializable
data class WeightLogRequest(val weightKg: Double? = null, val date: String? = null, val note: String? = null)

@Serializable
data class WeightLogResponse(
    val id: String,
    val userId: String,
    val date: String,
    val weightKg: Double,
    val note: String? = null,
    val createdAt: String,
)

@Serializable
data class WeightLogsResponse(val items: List<WeightLogResponse>, val total: Int)

@Serializable
data class PhotoNutritionJobRequest(val imageUrl: String? = null, val consentAccepted: Boolean = false)

@Serializable
data class PhotoNutritionJobResponse(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val status: String,
    val requiresReview: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

fun NutritionLogRequest.toInput(): NutritionLogInput = NutritionLogInput(
    date = date?.let { LocalDate.parse(it) },
    mealType = mealType?.let { MealLogType.valueOf(it.uppercase()) },
    title = title,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    incomplete = incomplete,
    warning = warning,
)

fun WaterLogRequest.toInput(): WaterLogInput = WaterLogInput(amountMl = amountMl, date = date?.let { LocalDate.parse(it) })
fun WeightLogRequest.toInput(): WeightLogInput = WeightLogInput(weightKg = weightKg, date = date?.let { LocalDate.parse(it) }, note = note)
fun PhotoNutritionJobRequest.toInput(): PhotoNutritionJobInput = PhotoNutritionJobInput(imageUrl = imageUrl, consentAccepted = consentAccepted)

fun HealthNutritionLog.toResponse(): HealthNutritionLogResponse = HealthNutritionLogResponse(
    id = id.toString(),
    userId = userId.toString(),
    date = date.toString(),
    mealType = mealType.name,
    title = title,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    incomplete = incomplete,
    warning = warning,
    deleted = deleted,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun NutritionLogsResult.toResponse(): NutritionLogsResponse = NutritionLogsResponse(items.map { it.toResponse() }, total)

fun NutritionTotals.toResponse(): NutritionTotalsResponse = NutritionTotalsResponse(
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    incompleteCount = incompleteCount,
    waterMl = waterMl,
    latestWeightKg = latestWeightKg,
)

fun DashboardResult.toResponse(): NutritionDashboardResponse = NutritionDashboardResponse(
    day = day.toResponse(),
    week = week.toResponse(),
    month = month.toResponse(),
    year = year.toResponse(),
)

fun WaterLog.toResponse(): WaterLogResponse = WaterLogResponse(
    id = id.toString(),
    userId = userId.toString(),
    date = date.toString(),
    amountMl = amountMl,
    createdAt = createdAt.toString(),
)

fun WeightLog.toResponse(): WeightLogResponse = WeightLogResponse(
    id = id.toString(),
    userId = userId.toString(),
    date = date.toString(),
    weightKg = weightKg,
    note = note,
    createdAt = createdAt.toString(),
)

fun WeightLogsResult.toResponse(): WeightLogsResponse = WeightLogsResponse(items.map { it.toResponse() }, total)

fun PhotoNutritionJob.toResponse(): PhotoNutritionJobResponse = PhotoNutritionJobResponse(
    id = id.toString(),
    userId = userId.toString(),
    imageUrl = imageUrl,
    status = status.name,
    requiresReview = requiresReview,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
