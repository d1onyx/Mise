package com.dishlab.application.service

import com.dishlab.domain.error.ForbiddenError
import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.HealthNutritionLog
import com.dishlab.domain.model.MealLogType
import com.dishlab.domain.model.NutritionTotals
import com.dishlab.domain.model.PhotoNutritionJob
import com.dishlab.domain.model.WaterLog
import com.dishlab.domain.model.WeightLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface HealthRepository {
    fun saveNutritionLog(log: HealthNutritionLog): HealthNutritionLog
    fun findNutritionLog(userId: UUID, logId: UUID): HealthNutritionLog?
    fun listNutritionLogs(userId: UUID): List<HealthNutritionLog>
    fun saveWaterLog(log: WaterLog): WaterLog
    fun listWaterLogs(userId: UUID): List<WaterLog>
    fun saveWeightLog(log: WeightLog): WeightLog
    fun listWeightLogs(userId: UUID): List<WeightLog>
    fun savePhotoJob(job: PhotoNutritionJob): PhotoNutritionJob
    fun findPhotoJob(userId: UUID, jobId: UUID): PhotoNutritionJob?
}

class InMemoryHealthRepository : HealthRepository {
    private val nutrition = ConcurrentHashMap<UUID, HealthNutritionLog>()
    private val water = ConcurrentHashMap<UUID, WaterLog>()
    private val weights = ConcurrentHashMap<UUID, WeightLog>()
    private val photoJobs = ConcurrentHashMap<UUID, PhotoNutritionJob>()

    override fun saveNutritionLog(log: HealthNutritionLog): HealthNutritionLog {
        val saved = log.copy(updatedAt = Instant.now())
        nutrition[saved.id] = saved
        return saved
    }

    override fun findNutritionLog(userId: UUID, logId: UUID): HealthNutritionLog? = nutrition[logId]?.takeIf { it.userId == userId }

    override fun listNutritionLogs(userId: UUID): List<HealthNutritionLog> = nutrition.values.filter { it.userId == userId }.sortedByDescending { it.date }

    override fun saveWaterLog(log: WaterLog): WaterLog {
        water[log.id] = log
        return log
    }

    override fun listWaterLogs(userId: UUID): List<WaterLog> = water.values.filter { it.userId == userId }.sortedByDescending { it.date }

    override fun saveWeightLog(log: WeightLog): WeightLog {
        weights[log.id] = log
        return log
    }

    override fun listWeightLogs(userId: UUID): List<WeightLog> = weights.values.filter { it.userId == userId }.sortedByDescending { it.date }

    override fun savePhotoJob(job: PhotoNutritionJob): PhotoNutritionJob {
        val saved = job.copy(updatedAt = Instant.now())
        photoJobs[saved.id] = saved
        return saved
    }

    override fun findPhotoJob(userId: UUID, jobId: UUID): PhotoNutritionJob? = photoJobs[jobId]?.takeIf { it.userId == userId }
}

data class NutritionLogInput(
    val date: LocalDate? = null,
    val mealType: MealLogType? = null,
    val title: String? = null,
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val incomplete: Boolean? = null,
    val warning: String? = null,
)

data class NutritionLogsResult(val items: List<HealthNutritionLog>, val total: Int)
data class DashboardResult(val day: NutritionTotals, val week: NutritionTotals, val month: NutritionTotals, val year: NutritionTotals)

data class WaterLogInput(val amountMl: Int? = null, val date: LocalDate? = null)
data class WeightLogInput(val weightKg: Double? = null, val date: LocalDate? = null, val note: String? = null)
data class WeightLogsResult(val items: List<WeightLog>, val total: Int)
data class PhotoNutritionJobInput(val imageUrl: String? = null, val consentAccepted: Boolean = false)

class HealthService(
    private val currentUserResolver: CurrentUserResolver,
    private val repository: HealthRepository,
) {
    fun createNutritionLog(firebaseUid: String, input: NutritionLogInput): HealthNutritionLog {
        val user = currentUserResolver.resolve(firebaseUid)
        return repository.saveNutritionLog(
            HealthNutritionLog(
                id = UUID.randomUUID(),
                userId = user.id,
                date = input.date ?: LocalDate.now(),
                mealType = input.mealType ?: throw ValidationError(details = mapOf("mealType" to "Meal type is required")),
                title = validateTitle(input.title),
                calories = validateNonNegative(input.calories, "calories"),
                proteinGrams = validateNonNegative(input.proteinGrams ?: 0.0, "proteinGrams"),
                carbsGrams = validateNonNegative(input.carbsGrams ?: 0.0, "carbsGrams"),
                fatGrams = validateNonNegative(input.fatGrams ?: 0.0, "fatGrams"),
                incomplete = input.incomplete ?: false,
                warning = input.warning?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }

    fun listNutritionLogs(firebaseUid: String, from: LocalDate?, to: LocalDate?): NutritionLogsResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val items = repository.listNutritionLogs(user.id)
            .filter { !it.deleted }
            .filter { from == null || !it.date.isBefore(from) }
            .filter { to == null || !it.date.isAfter(to) }
        return NutritionLogsResult(items, items.size)
    }

    fun updateNutritionLog(firebaseUid: String, logId: UUID, input: NutritionLogInput): HealthNutritionLog {
        val user = currentUserResolver.resolve(firebaseUid)
        val current = repository.findNutritionLog(user.id, logId)?.takeIf { !it.deleted } ?: throw NotFoundError("Запис харчування не знайдено")
        return repository.saveNutritionLog(
            current.copy(
                date = input.date ?: current.date,
                mealType = input.mealType ?: current.mealType,
                title = input.title?.let { validateTitle(it) } ?: current.title,
                calories = input.calories?.let { validateNonNegative(it, "calories") } ?: current.calories,
                proteinGrams = input.proteinGrams?.let { validateNonNegative(it, "proteinGrams") } ?: current.proteinGrams,
                carbsGrams = input.carbsGrams?.let { validateNonNegative(it, "carbsGrams") } ?: current.carbsGrams,
                fatGrams = input.fatGrams?.let { validateNonNegative(it, "fatGrams") } ?: current.fatGrams,
                incomplete = input.incomplete ?: current.incomplete,
                warning = input.warning ?: current.warning,
            ),
        )
    }

    fun deleteNutritionLog(firebaseUid: String, logId: UUID): HealthNutritionLog {
        val user = currentUserResolver.resolve(firebaseUid)
        val current = repository.findNutritionLog(user.id, logId) ?: throw NotFoundError("Запис харчування не знайдено")
        return repository.saveNutritionLog(current.copy(deleted = true))
    }

    fun dashboard(firebaseUid: String, date: LocalDate): DashboardResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val dayStart = date
        val weekStart = date.with(DayOfWeek.MONDAY)
        val monthStart = YearMonth.from(date).atDay(1)
        val yearStart = Year.from(date).atDay(1)
        return DashboardResult(
            day = totals(user.id, dayStart, dayStart),
            week = totals(user.id, weekStart, weekStart.plusDays(6)),
            month = totals(user.id, monthStart, YearMonth.from(date).atEndOfMonth()),
            year = totals(user.id, yearStart, yearStart.plusYears(1).minusDays(1)),
        )
    }

    fun addWater(firebaseUid: String, input: WaterLogInput): WaterLog {
        val user = currentUserResolver.resolve(firebaseUid)
        val amount = input.amountMl ?: throw ValidationError(details = mapOf("amountMl" to "Amount is required"))
        if (amount !in 1..5000) throw ValidationError(details = mapOf("amountMl" to "Amount must be between 1 and 5000"))
        return repository.saveWaterLog(WaterLog(UUID.randomUUID(), user.id, input.date ?: LocalDate.now(), amount))
    }

    fun addWeight(firebaseUid: String, input: WeightLogInput): WeightLog {
        val user = currentUserResolver.resolve(firebaseUid)
        val weight = input.weightKg ?: throw ValidationError(details = mapOf("weightKg" to "Weight is required"))
        if (weight !in 20.0..400.0) throw ValidationError(details = mapOf("weightKg" to "Weight must be between 20 and 400 kg"))
        return repository.saveWeightLog(WeightLog(UUID.randomUUID(), user.id, input.date ?: LocalDate.now(), weight, input.note?.trim()?.takeIf { it.isNotBlank() }))
    }

    fun listWeights(firebaseUid: String): WeightLogsResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val items = repository.listWeightLogs(user.id)
        return WeightLogsResult(items, items.size)
    }

    fun createPhotoJob(firebaseUid: String, input: PhotoNutritionJobInput): PhotoNutritionJob {
        val user = currentUserResolver.resolve(firebaseUid)
        if (!input.consentAccepted) throw ForbiddenError()
        val imageUrl = input.imageUrl?.trim().orEmpty()
        if (!imageUrl.startsWith("http")) throw ValidationError(details = mapOf("imageUrl" to "Image URL is required"))
        return repository.savePhotoJob(PhotoNutritionJob(UUID.randomUUID(), user.id, imageUrl))
    }

    fun getPhotoJob(firebaseUid: String, jobId: UUID): PhotoNutritionJob {
        val user = currentUserResolver.resolve(firebaseUid)
        return repository.findPhotoJob(user.id, jobId) ?: throw NotFoundError("Завдання оцінки фото не знайдено")
    }

    private fun totals(userId: UUID, from: LocalDate, to: LocalDate): NutritionTotals {
        val logs = repository.listNutritionLogs(userId).filter { !it.deleted && !it.date.isBefore(from) && !it.date.isAfter(to) }
        val waterMl = repository.listWaterLogs(userId).filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sumOf { it.amountMl }
        val latestWeight = repository.listWeightLogs(userId).filter { !it.date.isAfter(to) }.maxByOrNull { it.date }?.weightKg
        return NutritionTotals(
            calories = logs.sumOf { it.calories },
            proteinGrams = logs.sumOf { it.proteinGrams },
            carbsGrams = logs.sumOf { it.carbsGrams },
            fatGrams = logs.sumOf { it.fatGrams },
            incompleteCount = logs.count { it.incomplete },
            waterMl = waterMl,
            latestWeightKg = latestWeight,
        )
    }

    private fun validateTitle(value: String?): String {
        val clean = value?.trim().orEmpty()
        if (clean.length !in 2..120) throw ValidationError(details = mapOf("title" to "Title must be between 2 and 120 characters"))
        return clean
    }

    private fun validateNonNegative(value: Double?, field: String): Double {
        val number = value ?: throw ValidationError(details = mapOf(field to "$field is required"))
        if (number < 0.0) throw ValidationError(details = mapOf(field to "$field cannot be negative"))
        return number
    }
}
