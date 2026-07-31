package com.d1onix.dishlab.feature.home.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.designsystem.preview.MiseScreenPreviews
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.home.presentation.profile.ProfileContent
import com.d1onix.dishlab.feature.home.presentation.auth.AuthContent
import com.d1onix.dishlab.feature.home.presentation.auth.AuthUiState
import com.d1onix.dishlab.feature.home.presentation.onboarding.OnboardingContent
import com.d1onix.dishlab.feature.home.presentation.onboarding.OnboardingUiState

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
private fun ProfileContentPreview() {
    MiseTheme {
        ProfileContent(state = ProfilePreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun AuthContentPreview() {
    MiseTheme {
        AuthContent(state = AuthUiState(), onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun OnboardingContentPreview() {
    MiseTheme {
        OnboardingContent(state = OnboardingUiState(showIntro = false), onAction = {})
    }
}
