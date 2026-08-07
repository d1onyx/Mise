package com.d1onix.dishlab.feature.recipes.navigation

import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

/** Recipes matching the current combination graph. */
@Serializable
data object RecipesRoute : Route

@Serializable
data object SavedRoute : Route

@Serializable
data class RecipeDetailRoute(val recipeId: String) : Route

@Serializable
data class CookingRoute(val recipeId: String) : Route

interface RecipesRouter {
    fun openRecipe(id: RecipeId)
    fun openCookingMode(id: RecipeId)
    fun goBack()
}
