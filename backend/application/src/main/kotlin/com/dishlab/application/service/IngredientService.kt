package com.dishlab.application.service

import com.dishlab.domain.error.ConversionImpossibleError
import com.dishlab.domain.error.DensityRequiredError
import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.Ingredient
import com.dishlab.domain.model.IngredientSubstitute
import com.dishlab.domain.model.NutritionFacts
import com.dishlab.domain.model.UnitConversionResult
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round

data class IngredientSearchResult(
    val items: List<Ingredient>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

interface IngredientRepository {
    fun save(ingredient: Ingredient): Ingredient
    fun findById(id: UUID): Ingredient?
    fun findAll(): List<Ingredient>
    fun findByName(name: String): Ingredient?
    fun search(query: String, page: Int, pageSize: Int): IngredientSearchResult
    fun findSubstitutes(ingredientId: UUID): List<Pair<Ingredient, IngredientSubstitute>>
}

class InMemoryIngredientRepository : IngredientRepository {
    private val ingredients = ConcurrentHashMap<UUID, Ingredient>()
    private val substitutes = ConcurrentHashMap<UUID, MutableList<IngredientSubstitute>>()

    init {
        seed()
    }

    override fun save(ingredient: Ingredient): Ingredient {
        ingredients[ingredient.id] = ingredient
        return ingredient
    }

    override fun findById(id: UUID): Ingredient? = ingredients[id]

    override fun findAll(): List<Ingredient> = ingredients.values.sortedBy { it.name.lowercase() }

    override fun findByName(name: String): Ingredient? =
        ingredients.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

    override fun search(query: String, page: Int, pageSize: Int): IngredientSearchResult {
        val normalized = query.trim().lowercase()
        val filtered = ingredients.values
            .filter { normalized.isBlank() || it.name.lowercase().contains(normalized) }
            .sortedBy { it.name.lowercase() }
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, 100)
        val start = (safePage - 1) * safePageSize
        val items = if (start >= filtered.size) emptyList() else filtered.drop(start).take(safePageSize)
        return IngredientSearchResult(items = items, page = safePage, pageSize = safePageSize, total = filtered.size)
    }

    override fun findSubstitutes(ingredientId: UUID): List<Pair<Ingredient, IngredientSubstitute>> =
        substitutes[ingredientId].orEmpty().mapNotNull { substitute ->
            ingredients[substitute.substituteIngredientId]?.let { it to substitute }
        }

    private fun seed() {
        val tomato = save(
            Ingredient(
                id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                name = "Tomato",
                defaultUnit = "g",
                nutritionPer100g = NutritionFacts(calories = 18.0, protein = 0.9, fat = 0.2, carbs = 3.9),
            ),
        )
        val milk = save(
            Ingredient(
                id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
                name = "Milk",
                defaultUnit = "ml",
                allergens = listOf("milk"),
                nutritionPer100g = NutritionFacts(calories = 42.0, protein = 3.4, fat = 1.0, carbs = 5.0),
            ),
        )
        val oatMilk = save(
            Ingredient(
                id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                name = "Oat milk",
                defaultUnit = "ml",
                nutritionPer100g = NutritionFacts(calories = 46.0, protein = 1.0, fat = 1.5, carbs = 7.0),
            ),
        )
        save(
            Ingredient(
                id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                name = "Wheat flour",
                defaultUnit = "g",
                allergens = listOf("gluten"),
                nutritionPer100g = NutritionFacts(calories = 364.0, protein = 10.0, fat = 1.0, carbs = 76.0),
            ),
        )
        substitutes.computeIfAbsent(milk.id) { mutableListOf() }.add(
            IngredientSubstitute(
                ingredientId = milk.id,
                substituteIngredientId = oatMilk.id,
                reason = "dairy_free",
            ),
        )
        tomato.id
    }
}

class IngredientService(
    private val currentUserResolver: CurrentUserResolver,
    private val ingredients: IngredientRepository,
) {
    fun search(firebaseUid: String, query: String, page: Int, pageSize: Int): IngredientSearchResult {
        currentUserResolver.resolve(firebaseUid)
        return ingredients.search(query, page, pageSize)
    }

    fun createCustom(firebaseUid: String, name: String, defaultUnit: String, allergens: List<String>, nutrition: NutritionFacts?): Ingredient {
        val user = currentUserResolver.resolve(firebaseUid)
        val trimmedName = name.trim()
        if (trimmedName.length !in 2..120) {
            throw ValidationError(details = mapOf("name" to "Ingredient name must be between 2 and 120 characters"))
        }
        val unit = defaultUnit.trim().lowercase()
        if (unit.isBlank()) throw ValidationError(details = mapOf("defaultUnit" to "Default unit is required"))
        return ingredients.save(
            Ingredient(
                id = UUID.randomUUID(),
                name = trimmedName,
                defaultUnit = unit,
                allergens = allergens.normalizedDistinct(),
                nutritionPer100g = nutrition,
                customCreatedByUserId = user.id,
            ),
        )
    }

    fun get(firebaseUid: String, ingredientId: UUID): Ingredient {
        currentUserResolver.resolve(firebaseUid)
        return ingredients.findById(ingredientId) ?: throw NotFoundError("Інгредієнт не знайдено")
    }

    fun substitutes(firebaseUid: String, ingredientId: UUID): List<Pair<Ingredient, IngredientSubstitute>> {
        currentUserResolver.resolve(firebaseUid)
        get(firebaseUid, ingredientId)
        return ingredients.findSubstitutes(ingredientId)
    }
}

class UnitConversionService {
    fun convert(amount: Double, fromUnit: String, toUnit: String, densityGramPerMl: Double?): UnitConversionResult {
        if (amount <= 0.0) throw ValidationError(details = mapOf("amount" to "Amount must be positive"))
        val from = fromUnit.trim().lowercase()
        val to = toUnit.trim().lowercase()
        if (from == to) return UnitConversionResult(roundForApi(amount), to)

        val fromDimension = dimension(from)
        val toDimension = dimension(to)
        if (fromDimension == null || toDimension == null) throw ConversionImpossibleError(details = mapOf("fromUnit" to from, "toUnit" to to))

        if (fromDimension == toDimension) {
            val base = amount * factorToBase(from)
            return UnitConversionResult(roundForApi(base / factorToBase(to)), to)
        }

        if ((fromDimension == "volume" && toDimension == "mass") || (fromDimension == "mass" && toDimension == "volume")) {
            val density = densityGramPerMl ?: throw DensityRequiredError(details = mapOf("fromUnit" to from, "toUnit" to to))
            val grams = if (fromDimension == "volume") amount * factorToBase(from) * density else amount * factorToBase(from)
            val result = if (toDimension == "mass") grams / factorToBase(to) else grams / density / factorToBase(to)
            return UnitConversionResult(roundForApi(result), to)
        }

        throw ConversionImpossibleError(details = mapOf("fromUnit" to from, "toUnit" to to))
    }

    private fun dimension(unit: String): String? = when (unit) {
        "g", "kg", "mg" -> "mass"
        "ml", "l" -> "volume"
        else -> null
    }

    private fun factorToBase(unit: String): Double = when (unit) {
        "mg" -> 0.001
        "g" -> 1.0
        "kg" -> 1000.0
        "ml" -> 1.0
        "l" -> 1000.0
        else -> throw ConversionImpossibleError(details = mapOf("unit" to unit))
    }

    private fun roundForApi(value: Double): Double = round(value * 10000.0) / 10000.0
}

private fun List<String>.normalizedDistinct(): List<String> =
    map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
