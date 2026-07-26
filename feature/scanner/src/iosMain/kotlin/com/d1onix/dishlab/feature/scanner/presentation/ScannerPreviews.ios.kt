package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import platform.UIKit.UIViewController

/** No camera slot, so the overlay renders without asking for the camera permission. */
fun scanPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.Idle, onAction = {})
    }
}

/**
 * The manual-entry sheet is here and not only on Android because it is the one
 * place a keyboard comes up, and the keyboard inset is the thing iOS gets wrong.
 */
fun scanManualEntryPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.ManualEntry, onAction = {})
    }
}

fun scanProductReviewPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.ProductReview, onAction = {})
    }
}

fun scanNotFoundPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        ScanNotFoundContent(state = ScanNotFoundPreviewStates.Default, onAction = {})
    }
}
