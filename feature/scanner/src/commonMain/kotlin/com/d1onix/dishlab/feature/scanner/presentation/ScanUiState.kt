package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product

@Immutable
data class ScanUiState(
    /** A barcode is being resolved — further detections are ignored until it finishes. */
    val isResolving: Boolean = false,
    val manualEntryVisible: Boolean = false,
    val manualBarcode: String = "",
    val reviewedProduct: Product? = null,
    val reviewedProductAlreadyAdded: Boolean = false,
) {
    val canSubmitManualBarcode: Boolean get() = manualBarcode.isNotBlank() && !isResolving
}

sealed interface ScanAction {
    /** The camera decoded a barcode. */
    data class BarcodeDetected(val barcode: String) : ScanAction
    data object CaptureClicked : ScanAction
    data object SimulateNotFoundClicked : ScanAction
    data object ManualEntryToggled : ScanAction
    data class ManualBarcodeChanged(val value: String) : ScanAction
    data object ManualBarcodeSubmitted : ScanAction
    data object AddReviewedProductClicked : ScanAction
    data object ReviewedProductSkipped : ScanAction
    data object ReviewBackClicked : ScanAction
    data object BackClicked : ScanAction
}
