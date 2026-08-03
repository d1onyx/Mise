package com.dishlab.api.dto

import com.dishlab.application.service.IngredientSearchResult
import com.dishlab.domain.model.Ingredient
import com.dishlab.domain.model.IngredientSubstitute
import com.dishlab.domain.model.NutritionFacts
import com.dishlab.domain.model.UnitConversionResult
import kotlinx.serialization.Serializable

@Serializable
data class NutritionFactsDto(
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
)

@Serializable
data class IngredientResponse(
    val id: String,
    val name: String,
    val defaultUnit: String,
    val allergens: List<String>,
    val nutritionPer100g: NutritionFactsDto? = null,
    val nutritionComplete: Boolean,
    val barcode: String? = null,
)

@Serializable
data class IngredientSearchResponse(
    val items: List<IngredientResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

@Serializable
data class CreateCustomIngredientRequest(
    val name: String,
    val defaultUnit: String,
    val allergens: List<String> = emptyList(),
    val nutritionPer100g: NutritionFactsDto? = null,
)

@Serializable
data class SubstituteResponse(
    val ingredient: IngredientResponse,
    val reason: String,
)

@Serializable
data class SubstitutesResponse(
    val substitutes: List<SubstituteResponse>,
)

@Serializable
data class UnitConvertRequest(
    val amount: Double,
    val fromUnit: String,
    val toUnit: String,
    val densityGramPerMl: Double? = null,
)

@Serializable
data class UnitConvertResponse(
    val amount: Double,
    val unit: String,
)

fun NutritionFactsDto.toDomain(): NutritionFacts = NutritionFacts(
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
)

fun NutritionFacts.toDto(): NutritionFactsDto = NutritionFactsDto(
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
)

fun Ingredient.toResponse(): IngredientResponse = IngredientResponse(
    id = id.toString(),
    name = name,
    defaultUnit = defaultUnit,
    allergens = allergens,
    nutritionPer100g = nutritionPer100g?.toDto(),
    nutritionComplete = nutritionComplete,
    barcode = barcode,
)

fun IngredientSearchResult.toResponse(): IngredientSearchResponse = IngredientSearchResponse(
    items = items.map { it.toResponse() },
    page = page,
    pageSize = pageSize,
    total = total,
)

fun Pair<Ingredient, IngredientSubstitute>.toResponse(): SubstituteResponse = SubstituteResponse(
    ingredient = first.toResponse(),
    reason = second.reason,
)

fun UnitConversionResult.toResponse(): UnitConvertResponse = UnitConvertResponse(
    amount = amount,
    unit = unit,
)
