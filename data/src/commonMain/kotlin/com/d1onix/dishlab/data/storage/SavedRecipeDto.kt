package com.d1onix.dishlab.data.storage

import com.d1onix.dishlab.domain.model.Ingredient
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep
import kotlinx.serialization.Serializable

/**
 * The full content of a saved recipe, cached locally at save time. Unlike
 * [com.d1onix.dishlab.data.catalog.dto.RecipeDto] (the bundled demo catalogue), this covers every
 * field on [Recipe] — a saved recipe usually comes from the pantry-match/backend catalogue, not
 * the bundled one, and must resolve fully offline afterwards.
 */
@Serializable
internal data class SavedRecipeDto(
    val id: String,
    val name: String,
    val minutes: Int,
    val difficulty: String,
    val categories: List<String>,
    val productIds: List<String>,
    val description: String,
    val ingredients: List<SavedIngredientDto>,
    val steps: List<SavedRecipeStepDto>,
)

@Serializable
internal data class SavedIngredientDto(
    val quantity: String,
    val name: String,
    val matched: Boolean = false,
)

@Serializable
internal data class SavedRecipeStepDto(
    val title: String,
    val description: String,
)

internal fun Recipe.toSavedDto(): SavedRecipeDto = SavedRecipeDto(
    id = id.value,
    name = name,
    minutes = minutes,
    difficulty = difficulty.name,
    categories = categories,
    productIds = productIds.map { it.value },
    description = description,
    ingredients = ingredients.map { SavedIngredientDto(it.quantity, it.name, it.matched) },
    steps = steps.map { SavedRecipeStepDto(it.title, it.description) },
)

internal fun SavedRecipeDto.toDomain(): Recipe = Recipe(
    id = RecipeId(id),
    name = name,
    minutes = minutes,
    difficulty = RecipeDifficulty.parse(difficulty) ?: RecipeDifficulty.Easy,
    categories = categories,
    productIds = productIds.map(::ProductId),
    description = description,
    ingredients = ingredients.map { Ingredient(it.quantity, it.name, it.matched) },
    steps = steps.map { RecipeStep(it.title, it.description) },
)
