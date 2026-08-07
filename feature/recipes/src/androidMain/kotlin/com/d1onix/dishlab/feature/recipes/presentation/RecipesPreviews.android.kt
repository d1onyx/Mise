package com.d1onix.dishlab.feature.recipes.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.designsystem.preview.MiseScreenPreviews
import com.d1onix.dishlab.designsystem.preview.MiseWidthPreviews
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.recipes.presentation.components.RecipeCard
import com.d1onix.dishlab.feature.recipes.presentation.cooking.CookingContent
import com.d1onix.dishlab.feature.recipes.presentation.detail.RecipeDetailContent
import com.d1onix.dishlab.feature.recipes.presentation.list.RecipeListContent
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.recipes_empty
import com.d1onix.dishlab.feature.recipes.resources.recipes_title
import com.d1onix.dishlab.feature.recipes.resources.saved_empty
import com.d1onix.dishlab.feature.recipes.resources.saved_title

@MiseScreenPreviews
@Composable
private fun RecipeListContentPreview() {
    MiseTheme {
        RecipeListContent(
            title = Res.string.recipes_title,
            emptyText = Res.string.recipes_empty,
            state = RecipeListPreviewStates.Default,
            onAction = {},
        )
    }
}

/** The Saved variant of the same screen, with nothing left after filtering. */
@MiseScreenPreviews
@Composable
private fun RecipeListContentEmptyPreview() {
    MiseTheme {
        RecipeListContent(
            title = Res.string.saved_title,
            emptyText = Res.string.saved_empty,
            state = RecipeListPreviewStates.Empty,
            onAction = {},
        )
    }
}

@MiseScreenPreviews
@Composable
private fun RecipeDetailContentPreview() {
    MiseTheme {
        RecipeDetailContent(state = RecipeDetailPreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun RecipeDetailContentSavedPreview() {
    MiseTheme {
        RecipeDetailContent(state = RecipeDetailPreviewStates.Saved, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun CookingContentPreview() {
    MiseTheme {
        CookingContent(state = CookingPreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun CookingContentRunningTimerPreview() {
    MiseTheme {
        CookingContent(state = CookingPreviewStates.RunningTimer, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun CookingContentLastStepPreview() {
    MiseTheme {
        CookingContent(state = CookingPreviewStates.LastStep, onAction = {})
    }
}

/** The card is width-driven, so it gets the two widths rather than three devices. */
@MiseWidthPreviews
@Composable
private fun RecipeCardPreview() {
    MiseTheme {
        RecipeCard(recipe = previewBowl, products = previewProducts, onClick = {})
    }
}

/** One product, hard difficulty — the other end of the card's range. */
@MiseWidthPreviews
@Composable
private fun RecipeCardSingleProductPreview() {
    MiseTheme {
        RecipeCard(
            recipe = previewMeatballs,
            products = previewProducts.take(1),
            onClick = {},
        )
    }
}
