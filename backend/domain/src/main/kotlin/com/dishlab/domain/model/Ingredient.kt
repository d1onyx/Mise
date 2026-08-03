package com.dishlab.domain.model

import java.time.Instant
import java.util.UUID

data class Ingredient(
    val id: UUID,
    val name: String,
    val defaultUnit: String,
    val allergens: List<String> = emptyList(),
    val nutritionPer100g: NutritionFacts? = null,
    val barcode: String? = null,
    val customCreatedByUserId: UUID? = null,
    val createdAt: Instant = Instant.now(),
) {
    val nutritionComplete: Boolean
        get() = nutritionPer100g?.isComplete == true
}

data class NutritionFacts(
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
) {
    val isComplete: Boolean
        get() = calories != null && protein != null && fat != null && carbs != null
}

data class IngredientSubstitute(
    val ingredientId: UUID,
    val substituteIngredientId: UUID,
    val reason: String,
)

data class UnitConversionResult(
    val amount: Double,
    val unit: String,
)
