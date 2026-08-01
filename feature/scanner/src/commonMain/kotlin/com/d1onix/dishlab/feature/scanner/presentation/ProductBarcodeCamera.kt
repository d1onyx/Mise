package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform camera preview that continuously emits product barcode values.
 *
 * [facing] and [torchOn] are driven from [ScanUiState], not held inside the
 * camera, so the controls stay testable and the viewfinder chrome can render in
 * a preview without a camera. The implementation reports back through
 * [onCapabilitiesChanged] once the hardware is known — until then the UI shows
 * no camera controls at all.
 */
@Composable
expect fun ProductBarcodeCamera(
    facing: CameraFacing,
    torchOn: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onCapabilitiesChanged: (CameraCapabilities) -> Unit,
    modifier: Modifier = Modifier,
)
