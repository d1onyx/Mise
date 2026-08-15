package com.d1onix.dishlab.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class RecipeId(val value: String)

data class Recipe(
    val id: RecipeId,
    val name: String,
    val minutes: Int,
    val difficulty: RecipeDifficulty,
    val categories: List<String>,
    /** Scanned products this recipe is built around — drives the graph → recipes link. */
    val productIds: List<ProductId>,
    val description: String,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
)

data class RecipePage(
    val items: List<Recipe>,
    val page: Int,
    val hasNextPage: Boolean,
)

data class Ingredient(
    val quantity: String,
    val name: String,
    /** Already on the user's combination graph — set by the backend, never recomputed on the client. */
    val matched: Boolean = false,
)

data class RecipeStep(
    val title: String,
    val description: String,
)

/** No display text here — the label is a UI resource, this is only the value. */
enum class RecipeDifficulty {
    Easy,
    Medium,
    Hard;

    companion object {
        fun parse(value: String): RecipeDifficulty? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/** The three time buckets offered by the recipe filter. */
enum class TimeBucket {
    Under15,
    Under30,
    Over30;

    fun matches(minutes: Int): Boolean = when (this) {
        Under15 -> minutes < 15
        Under30 -> minutes < 30
        Over30 -> minutes >= 30
    }
}

/** Everything the Recipes and Saved screens filter by. Empty sets mean «no filter». */
data class RecipeFilters(
    val query: String = "",
    val difficulties: Set<RecipeDifficulty> = emptySet(),
    val categories: Set<String> = emptySet(),
    val times: Set<TimeBucket> = emptySet(),
) {
    val isEmpty: Boolean
        get() = query.isBlank() && difficulties.isEmpty() && categories.isEmpty() && times.isEmpty()
}

/** Values advertised by the recipe catalogue for its filter UI. */
data class RecipeCatalogFilters(
    val categories: List<String> = emptyList(),
    val cuisines: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
)

/** Selected remote catalogue filters. Each set is sent as repeated query parameters. */
data class RecipeCatalogFilterSelection(
    val categories: Set<String> = emptySet(),
    val cuisines: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet(),
    val techniques: Set<String> = emptySet(),
)
