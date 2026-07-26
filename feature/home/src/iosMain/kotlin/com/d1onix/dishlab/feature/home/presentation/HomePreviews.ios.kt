package com.d1onix.dishlab.feature.home.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import platform.UIKit.UIViewController

/**
 * iOS previews are controllers, not annotations — there is no Compose preview
 * renderer on iOS, so Xcode's SwiftUI `#Preview` canvas hosts the real thing
 * (see `iosApp/iosApp/Previews.swift`).
 *
 * Which is why there are fewer of them than on Android: each one builds and
 * boots the framework, so iOS covers the states whose answer can only come from
 * a real device — safe-area insets around the notch and home indicator, and how
 * the mono type rasterises — while Android keeps the per-state matrix.
 */
fun homePreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        HomeContent(state = HomePreviewStates.Default, onAction = {})
    }
}
