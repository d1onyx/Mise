package com.dishlab.domain.model

data class CatalogNutrition(
    val calories: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val cholesterol: Double? = null,
    val sodium: Double? = null,
    val carbohydrates: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val protein: Double? = null,
)

data class CatalogRecipeIngredient(
    val name: String,
    val quantity: String? = null,
    val canonicalTags: List<String> = emptyList(),
)

data class CatalogRecipeStep(
    val position: Int,
    val text: String,
    val timerSeconds: Int? = null,
)

data class CatalogRecipe(
    val id: Long,
    val title: String,
    val authorName: String? = null,
    val cookTime: String? = null,
    val prepTime: String? = null,
    val totalTime: String? = null,
    val description: String? = null,
    val images: List<String> = emptyList(),
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val rating: Double? = null,
    val nutrition: CatalogNutrition = CatalogNutrition(),
    val ingredients: List<CatalogRecipeIngredient> = emptyList(),
    val steps: List<CatalogRecipeStep> = emptyList(),
    val bookmarked: Boolean = false,
)

data class PantryMatchedRecipe(
    val recipe: CatalogRecipe,
    val matchedCount: Int,
    val totalIngredients: Int,
) {
    val matchPercent: Int
        get() = if (totalIngredients == 0) 0 else (matchedCount * 100 / totalIngredients)
}
