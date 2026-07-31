package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.qrscannerplugin.QRScannerPlugin

@Composable
actual fun ProductBarcodeCamera(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val scanner = remember(scope) { QRScannerPlugin(scope) }
    val cameraState by rememberCameraKState(
        setupPlugins = { holder -> scanner.attachToStateHolder(holder) },
    )

    LaunchedEffect(scanner) {
        scanner.getQrCodeFlow().collect(onBarcodeDetected)
    }

    CameraKScreen(
        modifier = modifier,
        cameraState = cameraState,
        content = {},
    )
}
