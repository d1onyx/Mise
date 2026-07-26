package com.d1onix.dishlab.feature.home.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.designsystem.preview.MiseScreenPreviews
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/**
 * Android previews for the home screen.
 *
 * They live in `androidMain` because only Android Studio renders them, and
 * because everything that makes them worth having — the device catalogue, the
 * system bars, the font-scale setting behind [MiseScreenPreviews] — is an
 * Android concept. The iOS counterparts are in `iosMain`.
 */
@MiseScreenPreviews
@Composable
private fun HomeContentPreview() {
    MiseTheme {
        HomeContent(state = HomePreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun HomeContentEmptyPreview() {
    MiseTheme {
        HomeContent(state = HomePreviewStates.Empty, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun HomeContentToastPreview() {
    MiseTheme {
        HomeContent(state = HomePreviewStates.Toast, onAction = {})
    }
}
