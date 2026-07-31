package com.d1onix.dishlab.feature.scanner.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callbackState = rememberUpdatedState(onBarcodeDetected)
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

    DisposableEffect(lifecycleOwner, previewView, analyzer, executor) {
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
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }
        providerFuture.addListener(bindCamera, context.mainExecutor)

        onDispose {
            provider?.unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
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
