package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.feature.scanner.navigation.ScanTarget

/** Which physical camera the viewfinder is bound to. */
enum class CameraFacing { Back, Front }

/**
 * What the platform camera reports it can actually do. Both flags start `false`
 * so a control only appears once the camera has confirmed it — a torch button
 * that does nothing is worse than no button.
 */
@Immutable
data class CameraCapabilities(
    val torchAvailable: Boolean = false,
    val lensSwitchAvailable: Boolean = false,
)

/** Camera controls the user drives, plus what the hardware allows. */
@Immutable
data class CameraControls(
    val facing: CameraFacing = CameraFacing.Back,
    val torchOn: Boolean = false,
    val capabilities: CameraCapabilities = CameraCapabilities(),
) {
    /** Front cameras have no torch on either platform, so the button hides there. */
    val canToggleTorch: Boolean
        get() = capabilities.torchAvailable && facing == CameraFacing.Back

    val canSwitchFacing: Boolean get() = capabilities.lensSwitchAvailable
}

/**
 * The stage the scan is at. This is what the viewfinder renders, so the user can
 * tell «the camera is looking» from «we are asking the server» instead of
 * staring at one idle animation until a product appears.
 */
enum class ScanPhase { Searching, Resolving, Failed }

@Immutable
data class ScanUiState(
    val target: ScanTarget = ScanTarget.Graph,
    /** A barcode is being resolved — further detections are ignored until it finishes. */
    val isResolving: Boolean = false,
    val manualEntryVisible: Boolean = false,
    val manualBarcode: String = "",
    /** The barcode was valid, but the server could not be reached. */
    val resolutionFailed: Boolean = false,
    val failedBarcode: String? = null,
    /** The digits the camera just read, shown while they are being looked up. */
    val detectedBarcode: String? = null,
    val camera: CameraControls = CameraControls(),
    val reviewedProduct: Product? = null,
    val reviewedProductAlreadyAdded: Boolean = false,
) {
    val canSubmitManualBarcode: Boolean get() = manualBarcode.isNotBlank() && !isResolving
    val isComparison: Boolean get() = target == ScanTarget.Comparison

    val phase: ScanPhase
        get() = when {
            resolutionFailed -> ScanPhase.Failed
            isResolving -> ScanPhase.Resolving
            else -> ScanPhase.Searching
        }

    /** The code to show under the reticle — the one in flight, or the one that failed. */
    val visibleBarcode: String? get() = detectedBarcode ?: failedBarcode
}

sealed interface ScanAction {
    /** The camera decoded a barcode. */
    data class BarcodeDetected(val barcode: String) : ScanAction
    /** The platform camera reported what this device supports. */
    data class CameraCapabilitiesChanged(val capabilities: CameraCapabilities) : ScanAction
    data object TorchToggled : ScanAction
    data object CameraFacingToggled : ScanAction
    data object ManualEntryToggled : ScanAction
    data class ManualBarcodeChanged(val value: String) : ScanAction
    data object ManualBarcodeSubmitted : ScanAction
    data object RetryResolutionClicked : ScanAction
    data object AddReviewedProductClicked : ScanAction
    /** Starts a new, session-only comparison with this first scanned product. */
    data object CompareWithAnotherClicked : ScanAction
    data object ReviewedProductSkipped : ScanAction
    data object ReviewBackClicked : ScanAction
    data object BackClicked : ScanAction
    data object RecipesClicked : ScanAction
}
