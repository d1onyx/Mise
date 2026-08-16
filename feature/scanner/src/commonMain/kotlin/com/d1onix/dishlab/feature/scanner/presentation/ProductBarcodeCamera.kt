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
 *
 * [active] pauses barcode analysis without tearing the camera down — the
 * composable stays mounted (e.g. behind the manual-entry sheet) so toggling
 * it never re-triggers the camera hardware bind/unbind cycle.
 */
@Composable
expect fun ProductBarcodeCamera(
    facing: CameraFacing,
    torchOn: Boolean,
    active: Boolean = true,
    onBarcodeDetected: (String) -> Unit,
    onCapabilitiesChanged: (CameraCapabilities) -> Unit,
    modifier: Modifier = Modifier,
)
