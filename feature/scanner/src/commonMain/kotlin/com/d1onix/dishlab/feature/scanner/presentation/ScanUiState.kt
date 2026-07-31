package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.feature.scanner.navigation.ScanTarget

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
    val reviewedProduct: Product? = null,
    val reviewedProductAlreadyAdded: Boolean = false,
) {
    val canSubmitManualBarcode: Boolean get() = manualBarcode.isNotBlank() && !isResolving
    val isComparison: Boolean get() = target == ScanTarget.Comparison
}

sealed interface ScanAction {
    /** The camera decoded a barcode. */
    data class BarcodeDetected(val barcode: String) : ScanAction
    data object ManualEntryToggled : ScanAction
    data class ManualBarcodeChanged(val value: String) : ScanAction
    data object ManualBarcodeSubmitted : ScanAction
    data object RetryResolutionClicked : ScanAction
    data object AddReviewedProductClicked : ScanAction
    data object ReviewedProductSkipped : ScanAction
    data object ReviewBackClicked : ScanAction
    data object BackClicked : ScanAction
}
