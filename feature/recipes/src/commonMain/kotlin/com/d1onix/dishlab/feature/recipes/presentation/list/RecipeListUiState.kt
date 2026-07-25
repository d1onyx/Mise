package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeFilters
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.TimeBucket

/**
 * Shared by the Recipes and Saved screens — same list, same filter bar, only the
 * source of [all] differs.
 */
@Immutable
data class RecipeListUiState(
    val all: List<Recipe> = emptyList(),
    val visible: List<Recipe> = emptyList(),
    val filters: RecipeFilters = RecipeFilters(),
    val expandedGroup: FilterGroupId? = null,
    /** Products behind the recipe avatars, keyed for lookup while rendering. */
    val products: Map<ProductId, Product> = emptyMap(),
) {

    fun productsOf(recipe: Recipe): List<Product> = recipe.productIds.mapNotNull(products::get)

    /** Difficulty options are limited to what the current list actually contains. */
    val difficultyOptions: List<RecipeDifficulty>
        get() = RecipeDifficulty.entries.filter { difficulty -> all.any { it.difficulty == difficulty } }

    val categoryOptions: List<String>
        get() = all.flatMap { it.categories }.distinct()

    val timeOptions: List<TimeBucket> get() = TimeBucket.entries
}

/** The three filter groups. The label is a resource, resolved by the UI. */
enum class FilterGroupId { Difficulty, Category, Time }

sealed interface RecipeListAction {
    data class QueryChanged(val value: String) : RecipeListAction
    data class GroupClicked(val group: FilterGroupId) : RecipeListAction
    data class OptionClicked(val group: FilterGroupId, val option: String) : RecipeListAction
    data class RecipeClicked(val id: RecipeId) : RecipeListAction
    data object BackClicked : RecipeListAction
}
