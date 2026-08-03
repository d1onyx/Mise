package com.dishlab.application.service

import com.dishlab.domain.model.RecipeIngredient

data class IngredientValidationItem(
    val name: String,
    val isFood: Boolean,
    val isEdible: Boolean,
    val isAmountReasonable: Boolean,
    val reason: String? = null,
    val canonicalTags: List<String> = emptyList(),
    val matchedIngredientName: String? = null,
)

data class RecipeIngredientsValidationResult(
    val items: List<IngredientValidationItem>,
    val available: Boolean = true,
    val unavailableReason: String? = null,
) {
    val allValid: Boolean get() = items.all { it.isFood && it.isEdible && it.isAmountReasonable }

    fun errorDetails(): Map<String, String> = buildMap {
        items.forEachIndexed { index, item ->
            val key = "ingredients[$index]"
            when {
                !item.isFood -> put("$key.name", "\"${item.name}\" is not a food ingredient.${item.reason?.let { " $it" }.orEmpty()}")
                !item.isEdible -> put("$key.name", "\"${item.name}\" is not edible.${item.reason?.let { " $it" }.orEmpty()}")
                !item.isAmountReasonable -> put("$key.amount", item.reason ?: "Unreasonable amount or unit for \"${item.name}\"")
            }
        }
    }
}

fun interface RecipeIngredientValidationProvider {
    suspend fun validate(
        ingredients: List<RecipeIngredient>,
        knownIngredientNames: List<String>,
    ): RecipeIngredientsValidationResult
}

class NoOpRecipeIngredientValidationProvider : RecipeIngredientValidationProvider {
    override suspend fun validate(
        ingredients: List<RecipeIngredient>,
        knownIngredientNames: List<String>,
    ) = RecipeIngredientsValidationResult(
        items = ingredients.map { ing ->
            IngredientValidationItem(
                name = ing.name,
                isFood = true,
                isEdible = true,
                isAmountReasonable = true,
            )
        },
        available = false,
        unavailableReason = "AI-перевірка інгредієнтів не налаштована.",
    )
}
