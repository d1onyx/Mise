package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.recipes.presentation.components.RecipeCard
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.discover_empty
import com.d1onix.dishlab.feature.recipes.resources.discover_title
import com.d1onix.dishlab.feature.recipes.resources.recipes_empty
import com.d1onix.dishlab.feature.recipes.resources.recipes_title
import com.d1onix.dishlab.feature.recipes.resources.recipes_error
import com.d1onix.dishlab.feature.recipes.resources.recipes_loading
import com.d1onix.dishlab.feature.recipes.resources.recipes_retry
import com.d1onix.dishlab.feature.recipes.resources.saved_empty
import com.d1onix.dishlab.feature.recipes.resources.saved_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Recipes for what is currently on the combination graph. */
@Composable
fun RecipesScreen(viewModel: RecipesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        title = Res.string.recipes_title,
        emptyText = Res.string.recipes_empty,
        state = state,
        onAction = viewModel::onAction,
        showBanner = true,
    )
}

@Composable
fun SavedRecipesScreen(viewModel: SavedRecipesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        title = Res.string.saved_title,
        emptyText = Res.string.saved_empty,
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun DiscoverRecipesScreen(viewModel: DiscoverRecipesViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeListContent(
        title = Res.string.discover_title,
        emptyText = Res.string.discover_empty,
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * Recipes and Saved are the same screen with a different source and empty text,
 * so they share one composable rather than two near-identical copies.
 */
@Composable
internal fun RecipeListContent(
    title: StringResource,
    emptyText: StringResource,
    state: RecipeListUiState,
    onAction: (RecipeListAction) -> Unit,
    showBanner: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        // Keep the monetisation surface fixed above the screen chrome, rather
        // than blending it into the recipe list content.
        if (showBanner) {
            RecipeBannerAd(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }

        MiseScreenHeader(
            title = stringResource(title),
            onBackClick = { onAction(RecipeListAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (state.isLoading) {
            EmptyState(stringResource(Res.string.recipes_loading), Modifier.fillMaxWidth())
            return@Column
        }
        if (state.loadError) {
            EmptyState(stringResource(Res.string.recipes_error), Modifier.fillMaxWidth())
            MisePrimaryButton(stringResource(Res.string.recipes_retry), { onAction(RecipeListAction.RetryClicked) }, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
            return@Column
        }
        if (state.visible.isEmpty()) {
            EmptyState(stringResource(emptyText), Modifier.fillMaxWidth())
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(state.visible, key = { _, recipe -> recipe.id.value }) { index, recipe ->
                RecipeCard(
                    recipe = recipe,
                    products = state.productsOf(recipe),
                    onClick = { onAction(RecipeListAction.RecipeClicked(recipe.id)) },
                )
                if (index == state.visible.lastIndex && state.hasNextPage) {
                    LaunchedEffect(recipe.id) { onAction(RecipeListAction.LoadNextPage) }
                }
            }
        }
    }
}
