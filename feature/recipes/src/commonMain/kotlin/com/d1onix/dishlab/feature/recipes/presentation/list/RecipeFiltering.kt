package com.d1onix.dishlab.feature.recipes.presentation.list

import com.d1onix.dishlab.domain.model.RecipeCatalogFilterSelection
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeFilters
import com.d1onix.dishlab.domain.model.TimeBucket

/**
 * Toggle one option of one group, keeping every group independent.
 *
 * Options are identified by id — the enum name for difficulty and time, the
 * category itself for categories — so the displayed label can be translated
 * without breaking the filter.
 */
fun RecipeFilters.toggle(group: FilterGroupId, optionId: String): RecipeFilters = when (group) {
    FilterGroupId.Difficulty -> {
        val difficulty = RecipeDifficulty.parse(optionId)
        if (difficulty == null) this else copy(difficulties = difficulties.flip(difficulty))
    }

    FilterGroupId.Category -> copy(categories = categories.flip(optionId))

    FilterGroupId.Time -> {
        val bucket = TimeBucket.entries.firstOrNull { it.name == optionId }
        if (bucket == null) this else copy(times = times.flip(bucket))
    }
}

private fun <T> Set<T>.flip(value: T): Set<T> = if (value in this) this - value else this + value

/**
 * Toggle one option of one catalogue filter group, keeping every group independent.
 * Shared between the graph-based and catalogue-browse recipe screens.
 */
fun RecipeCatalogFilterSelection.toggle(
    group: RecipeCatalogFilterGroup,
    option: String,
): RecipeCatalogFilterSelection = when (group) {
    RecipeCatalogFilterGroup.Category -> copy(categories = categories.flip(option))
    RecipeCatalogFilterGroup.Cuisine -> copy(cuisines = cuisines.flip(option))
    RecipeCatalogFilterGroup.Equipment -> copy(equipment = equipment.flip(option))
    RecipeCatalogFilterGroup.Technique -> copy(techniques = techniques.flip(option))
}
