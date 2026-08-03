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

/**
 * The scan phases are separate previews because the reticle, the trail and the
 * status line all key off `state.phase` — a regression in any one of them is
 * only visible per phase.
 */
@MiseScreenPreviews
@Composable
private fun ScanContentResolvingPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.Resolving, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanContentFailedPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.Failed, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanContentTorchOnPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.TorchOn, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ScanContentFrontCameraPreview() {
    MiseTheme {
        ScanContent(state = ScanPreviewStates.FrontCamera, onAction = {})
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
