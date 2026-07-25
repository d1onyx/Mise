package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.FilterChipBar
import com.d1onix.dishlab.designsystem.component.FilterGroup
import com.d1onix.dishlab.designsystem.component.FilterOption
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseSearchField
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.recipes.presentation.components.RecipeCard
import com.d1onix.dishlab.feature.recipes.presentation.difficultyLabel
import com.d1onix.dishlab.feature.recipes.presentation.previewRecipes
import com.d1onix.dishlab.feature.recipes.presentation.timeBucketLabel
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.filter_group_category
import com.d1onix.dishlab.feature.recipes.resources.filter_group_difficulty
import com.d1onix.dishlab.feature.recipes.resources.filter_group_time
import com.d1onix.dishlab.feature.recipes.resources.recipes_empty
import com.d1onix.dishlab.feature.recipes.resources.recipes_search_placeholder
import com.d1onix.dishlab.feature.recipes.resources.recipes_title
import com.d1onix.dishlab.feature.recipes.resources.saved_empty
import com.d1onix.dishlab.feature.recipes.resources.saved_search_placeholder
import com.d1onix.dishlab.feature.recipes.resources.saved_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Recipes for what is currently on the combination graph. */
@Composable
fun RecipesScreen(viewModel: RecipesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        title = Res.string.recipes_title,
        searchPlaceholder = Res.string.recipes_search_placeholder,
        emptyText = Res.string.recipes_empty,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun SavedRecipesScreen(viewModel: SavedRecipesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        title = Res.string.saved_title,
        searchPlaceholder = Res.string.saved_search_placeholder,
        emptyText = Res.string.saved_empty,
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * Recipes and Saved are the same screen with a different source and empty text,
 * so they share one composable rather than two near-identical copies.
 */
@Composable
private fun RecipeListContent(
    title: StringResource,
    searchPlaceholder: StringResource,
    emptyText: StringResource,
    state: RecipeListUiState,
    onAction: (RecipeListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        MiseScreenHeader(
            title = stringResource(title),
            onBackClick = { onAction(RecipeListAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        MiseSearchField(
            value = state.filters.query,
            onValueChange = { onAction(RecipeListAction.QueryChanged(it)) },
            placeholder = stringResource(searchPlaceholder),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        )

        FilterChipBar(
            groups = state.filterGroups(),
            expandedGroupId = state.expandedGroup?.name,
            onGroupClick = { id -> onAction(RecipeListAction.GroupClicked(FilterGroupId.valueOf(id))) },
            onOptionClick = { id, option ->
                onAction(RecipeListAction.OptionClicked(FilterGroupId.valueOf(id), option))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )

        if (state.visible.isEmpty()) {
            EmptyState(stringResource(emptyText), Modifier.fillMaxWidth())
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.visible, key = { it.id.value }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    products = state.productsOf(recipe),
                    onClick = { onAction(RecipeListAction.RecipeClicked(recipe.id)) },
                )
            }
        }
    }
}

@Composable
private fun RecipeListUiState.filterGroups(): List<FilterGroup> = listOf(
    FilterGroup(
        id = FilterGroupId.Difficulty.name,
        name = stringResource(Res.string.filter_group_difficulty),
        options = difficultyOptions.map { FilterOption(it.name, difficultyLabel(it)) },
        selected = filters.difficulties.map { it.name }.toSet(),
    ),
    FilterGroup(
        id = FilterGroupId.Category.name,
        name = stringResource(Res.string.filter_group_category),
        // Categories come from the catalogue, so the id is the value itself.
        options = categoryOptions.map { FilterOption(it, it) },
        selected = filters.categories,
    ),
    FilterGroup(
        id = FilterGroupId.Time.name,
        name = stringResource(Res.string.filter_group_time),
        options = timeOptions.map { FilterOption(it.name, timeBucketLabel(it)) },
        selected = filters.times.map { it.name }.toSet(),
    ),
)

@Preview
@Composable
private fun RecipeListContentPreview() {
    val recipes = previewRecipes()
    MiseTheme {
        RecipeListContent(
            title = Res.string.recipes_title,
            searchPlaceholder = Res.string.recipes_search_placeholder,
            emptyText = Res.string.recipes_empty,
            state = RecipeListUiState(all = recipes, visible = recipes),
            onAction = {},
        )
    }
}

/** Filters that match nothing — the state a user hits and then reports as a bug. */
@Preview
@Composable
private fun RecipeListContentEmptyPreview() {
    val recipes = previewRecipes()
    MiseTheme {
        RecipeListContent(
            title = Res.string.saved_title,
            searchPlaceholder = Res.string.saved_search_placeholder,
            emptyText = Res.string.saved_empty,
            state = RecipeListUiState(all = recipes, visible = emptyList()),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun RecipeListContentExpandedFilterPreview() {
    val recipes = previewRecipes()
    MiseTheme {
        RecipeListContent(
            title = Res.string.recipes_title,
            searchPlaceholder = Res.string.recipes_search_placeholder,
            emptyText = Res.string.recipes_empty,
            state = RecipeListUiState(
                all = recipes,
                visible = recipes,
                expandedGroup = FilterGroupId.Difficulty,
            ),
            onAction = {},
        )
    }
}
