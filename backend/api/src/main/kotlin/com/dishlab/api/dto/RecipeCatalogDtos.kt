package com.dishlab.api.dto

import com.dishlab.application.service.CatalogRecipePage
import com.dishlab.application.service.PantryMatchPage
import com.dishlab.domain.model.CatalogNutrition
import com.dishlab.domain.model.CatalogRecipe
import com.dishlab.domain.model.PantryMatchedRecipe
import com.dishlab.domain.model.Recipe
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import kotlinx.serialization.Serializable

@Serializable
data class CatalogNutritionResponse(
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

@Serializable
data class CatalogRecipeIngredientResponse(
    val name: String,
    val quantity: String? = null,
    val canonicalTags: List<String> = emptyList(),
)

@Serializable
data class CatalogRecipeStepResponse(
    val position: Int,
    val text: String,
    val timerSeconds: Int? = null,
)

@Serializable
data class CatalogRecipeResponse(
    val id: String,
    val title: String,
    val authorName: String? = null,
    val cookTime: String? = null,
    val prepTime: String? = null,
    val totalTime: String? = null,
    val description: String? = null,
    val images: List<String>,
    val category: String? = null,
    val tags: List<String>,
    val rating: Double? = null,
    val nutrition: CatalogNutritionResponse,
    val ingredients: List<CatalogRecipeIngredientResponse>,
    val steps: List<CatalogRecipeStepResponse>,
)

@Serializable
data class CatalogRecipePageResponse(
    val items: List<CatalogRecipeResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

fun CatalogRecipe.toCatalogResponse(): CatalogRecipeResponse = CatalogRecipeResponse(
    id = "catalog:$id",
    title = title,
    authorName = authorName,
    cookTime = cookTime,
    prepTime = prepTime,
    totalTime = totalTime,
    description = description,
    images = images,
    category = category,
    tags = tags,
    rating = rating,
    nutrition = nutrition.toResponse(),
    ingredients = ingredients.map {
        CatalogRecipeIngredientResponse(it.name, it.quantity, it.canonicalTags)
    },
    steps = steps.map { CatalogRecipeStepResponse(it.position, it.text, it.timerSeconds) },
)

fun CatalogRecipePage.toResponse(): CatalogRecipePageResponse = CatalogRecipePageResponse(
    items = items.map(CatalogRecipe::toCatalogResponse),
    page = page,
    pageSize = pageSize,
    total = total,
)

fun Recipe.toCatalogResponse(): CatalogRecipeResponse =
    CatalogRecipeResponse(
        id = id.toString(),
        title = currentVersion.title,
        description = currentVersion.description,
        images = listOfNotNull(currentVersion.imageUrl),
        category = currentVersion.tags.firstOrNull(),
        tags = currentVersion.tags,
        rating = null,
        nutrition = CatalogNutritionResponse(),
        ingredients = currentVersion.ingredients.map {
            CatalogRecipeIngredientResponse(
                name = it.name,
                quantity = listOf(it.amount.toDisplayAmount(), it.unit).filter(String::isNotBlank).joinToString(" "),
                canonicalTags = it.name.ingredientCanonicalTag()?.let { tag -> listOf(tag) } ?: emptyList(),
            )
        },
        steps = currentVersion.steps.map {
            CatalogRecipeStepResponse(it.position, it.text, it.timerSeconds)
        },
    )

@Serializable
data class PantryMatchedRecipeResponse(
    val recipe: CatalogRecipeResponse,
    val matchedCount: Int,
    val totalIngredients: Int,
    val matchPercent: Int,
)

@Serializable
data class PantryMatchPageResponse(
    val items: List<PantryMatchedRecipeResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

fun PantryMatchedRecipe.toResponse(): PantryMatchedRecipeResponse = PantryMatchedRecipeResponse(
    recipe = recipe.toCatalogResponse(),
    matchedCount = matchedCount,
    totalIngredients = totalIngredients,
    matchPercent = matchPercent,
)

fun PantryMatchPage.toResponse(): PantryMatchPageResponse = PantryMatchPageResponse(
    items = items.map { it.toResponse() },
    page = page,
    pageSize = pageSize,
    total = total,
)

private fun CatalogNutrition.toResponse() = CatalogNutritionResponse(
    calories = calories,
    fat = fat,
    saturatedFat = saturatedFat,
    cholesterol = cholesterol,
    sodium = sodium,
    carbohydrates = carbohydrates,
    fiber = fiber,
    sugar = sugar,
    protein = protein,
)

private fun Double.toDisplayAmount(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
