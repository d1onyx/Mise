package com.d1onix.dishlab.feature.scanner.presentation

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors

/**
 * CameraX scanner for product codes. ML Kit receives the CameraX rotation so
 * product codes work in every physical device orientation.
 */
@Composable
actual fun ProductBarcodeCamera(
    facing: CameraFacing,
    torchOn: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onCapabilitiesChanged: (CameraCapabilities) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callbackState = rememberUpdatedState(onBarcodeDetected)
    val capabilitiesState = rememberUpdatedState(onCapabilitiesChanged)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient(productBarcodeOptions) }
    val analyzer = remember(scanner) {
        MlKitProductBarcodeAnalyzer(scanner) { callbackState.value(it) }
    }

    // Held so the torch effect below can reach the camera CameraX handed back.
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Re-binds on a lens change: CameraX has no way to swap the selector in place.
    DisposableEffect(lifecycleOwner, previewView, analyzer, executor, facing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        val bindCamera = Runnable {
            provider = providerFuture.get().also { cameraProvider ->
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    // A barcode needs far less detail than the sensor's default max
                    // resolution. Capping analysis frames to ~720p keeps ML Kit's
                    // per-frame decode cheap so it never backs up behind the live
                    // preview and stutters it — plenty of pixels for a barcode.
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                            .build(),
                    )
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                cameraProvider.unbindAll()
                val bound = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    facing.toSelector(),
                    preview,
                    analysis,
                )
                camera = bound
                capabilitiesState.value(
                    CameraCapabilities(
                        torchAvailable = bound.cameraInfo.hasFlashUnit(),
                        // Reported from the provider rather than assumed: tablets and
                        // emulators regularly ship only one of the two lenses.
                        lensSwitchAvailable = cameraProvider.hasCameraSafely(CameraSelector.DEFAULT_BACK_CAMERA) &&
                            cameraProvider.hasCameraSafely(CameraSelector.DEFAULT_FRONT_CAMERA),
                    ),
                )
            }
        }
        providerFuture.addListener(bindCamera, context.mainExecutor)

        onDispose {
            camera = null
            provider?.unbindAll()
        }
    }

    // The scanner and executor outlive lens changes — only the binding is redone.
    DisposableEffect(scanner, executor) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(camera, torchOn) {
        val control = camera ?: return@LaunchedEffect
        if (control.cameraInfo.hasFlashUnit()) control.cameraControl.enableTorch(torchOn)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

private fun CameraFacing.toSelector(): CameraSelector = when (this) {
    CameraFacing.Back -> CameraSelector.DEFAULT_BACK_CAMERA
    CameraFacing.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
}

/**
 * `hasCamera` throws instead of returning false when the selector cannot be
 * resolved at all, which is exactly the case we are asking about.
 */
private fun ProcessCameraProvider.hasCameraSafely(selector: CameraSelector): Boolean = try {
    hasCamera(selector)
} catch (_: IllegalArgumentException) {
    false
}

private class MlKitProductBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val isProcessing = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage == null) {
            isProcessing.set(false)
            image.close()
            return
        }

            val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstNotNullOfOrNull(Barcode::getRawValue)
                        ?.takeIf(String::isNotBlank)
                        ?.let(onBarcodeDetected)
                }
                .addOnCompleteListener {
                    isProcessing.set(false)
                    image.close()
                }
    }
}

private val productBarcodeOptions = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_ITF,
    )
    .build()
