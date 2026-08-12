package com.d1onix.dishlab.feature.recipes.presentation

import com.d1onix.dishlab.feature.recipes.presentation.detail.RecipeDetailUiState
import com.d1onix.dishlab.feature.recipes.presentation.list.FilterGroupId
import com.d1onix.dishlab.feature.recipes.presentation.list.RecipeListUiState

/** See `HomePreviewStates` for why the fixtures are common and the previews are not. */
internal object RecipeListPreviewStates {

    private val recipes = previewRecipes()
    private val productsById = previewProducts.associateBy { it.id }

    val Default = RecipeListUiState(all = recipes, visible = recipes, products = productsById)

    /** Filters that match nothing — the state a user hits and then reports as a bug. */
    val Empty = RecipeListUiState(all = recipes, visible = emptyList(), products = productsById)

    val ExpandedFilter = Default.copy(expandedGroup = FilterGroupId.Difficulty)
}

internal object RecipeDetailPreviewStates {

    val Default = RecipeDetailUiState(
        recipe = previewBowl,
        products = previewProducts,
        isSaved = false,
    )

    val Saved = Default.copy(isSaved = true)
}
