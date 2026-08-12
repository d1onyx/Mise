package com.d1onix.dishlab.feature.recipes.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.recipes.presentation.detail.RecipeDetailContent
import com.d1onix.dishlab.feature.recipes.presentation.list.RecipeListContent
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.recipes_empty
import com.d1onix.dishlab.feature.recipes.resources.recipes_title
import platform.UIKit.UIViewController

fun recipeListPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        RecipeListContent(
            title = Res.string.recipes_title,
            emptyText = Res.string.recipes_empty,
            state = RecipeListPreviewStates.Default,
            onAction = {},
        )
    }
}

fun recipeDetailPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        RecipeDetailContent(state = RecipeDetailPreviewStates.Default, onAction = {})
    }
}
