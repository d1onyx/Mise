package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.CameraLens
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.state.CameraKState
import com.kashif.qrscannerplugin.QRScannerPlugin

@Composable
actual fun ProductBarcodeCamera(
    facing: CameraFacing,
    torchOn: Boolean,
    active: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onCapabilitiesChanged: (CameraCapabilities) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val scanner = remember(scope) { QRScannerPlugin(scope) }
    val cameraState by rememberCameraKState(
        setupPlugins = { holder -> scanner.attachToStateHolder(holder) },
    )
    val capabilitiesState = rememberUpdatedState(onCapabilitiesChanged)
    val activeState = rememberUpdatedState(active)

    LaunchedEffect(scanner) {
        // Manual entry pauses detection without tearing the camera down —
        // mirrors the Android side, so a code drifting into frame while the
        // user is typing can't hijack them into the resolving screen.
        scanner.getQrCodeFlow().collect { if (activeState.value) onBarcodeDetected(it) }
    }

    val ready = cameraState as? CameraKState.Ready

    LaunchedEffect(ready) {
        if (ready == null) return@LaunchedEffect
        // Every iPhone ships both lenses and a rear torch; the simulator reports a
        // camera but lights nothing, which only ever costs a no-op tap.
        capabilitiesState.value(
            CameraCapabilities(torchAvailable = true, lensSwitchAvailable = true),
        )
    }

    LaunchedEffect(ready, facing) {
        val controller = ready?.controller ?: return@LaunchedEffect
        val target = when (facing) {
            CameraFacing.Back -> CameraLens.BACK
            CameraFacing.Front -> CameraLens.FRONT
        }
        // CameraK only exposes a toggle, so the desired lens is reached by
        // comparing against the one the controller reports.
        if (controller.getCameraLens() != target) controller.toggleCameraLens()
    }

    LaunchedEffect(ready, facing, torchOn) {
        val controller = ready?.controller ?: return@LaunchedEffect
        controller.setTorchMode(if (torchOn) TorchMode.ON else TorchMode.OFF)
    }

    CameraKScreen(
        modifier = modifier,
        cameraState = cameraState,
        content = {},
    )
}
