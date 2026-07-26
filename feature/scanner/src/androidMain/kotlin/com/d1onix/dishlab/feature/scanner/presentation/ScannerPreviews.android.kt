package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.designsystem.preview.MiseScreenPreviews
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/**
 * The camera slot is left at its default — an empty composable — so these
 * render without a camera. That is the whole reason `ScanContent` takes
 * `cameraPreview` as a parameter instead of calling CameraK itself.
 */
@MiseScreenPreviews
@Composable
private fun ScanContentPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.Idle, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanContentManualEntryPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.ManualEntry, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanContentResolvingPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.Resolving, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanProductReviewPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.ProductReview, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanNotFoundContentPreview() {
    MiseTheme {
        ScanNotFoundContent(state = ScanNotFoundPreviewStates.Default, onAction = {})
    }
}
