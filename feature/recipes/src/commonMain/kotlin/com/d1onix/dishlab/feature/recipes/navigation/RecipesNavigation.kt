package com.d1onix.dishlab.feature.recipes.navigation

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

/** Recipes matching the current combination graph. */
@Serializable
data object RecipesRoute : Route

/** Browse the complete recipe catalogue without requiring scanned products. */
@Serializable
data object DiscoverRecipesRoute : Route

@Serializable
data object SavedRoute : Route

/** [productIds] preserves pantry-match "already on your graph" ingredient highlighting into the detail call. */
@Serializable
data class RecipeDetailRoute(val recipeId: String, val productIds: List<String> = emptyList()) : Route

interface RecipesRouter {
    fun openRecipe(id: RecipeId, productIds: List<ProductId> = emptyList())
    fun goBack()
}
