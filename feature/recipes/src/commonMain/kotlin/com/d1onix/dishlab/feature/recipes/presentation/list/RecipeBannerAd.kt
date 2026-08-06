package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Android supplies the monetized banner; other platforms keep the recipe layout ad-free. */
@Composable
internal expect fun RecipeBannerAd(modifier: Modifier)
