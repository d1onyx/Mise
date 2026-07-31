package com.d1onix.dishlab.feature.scanner.presentation

import android.graphics.ImageFormat
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executors

/**
 * CameraX scanner for product codes. Every Y-plane is decoded in all four
 * rotations, so both the device and the printed barcode may be rotated.
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
    val analyzer = remember { RotatingProductBarcodeAnalyzer { callbackState.value(it) } }

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
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

private class RotatingProductBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(
                    DecodeHintType.POSSIBLE_FORMATS,
                    listOf(
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.QR_CODE,
                    ),
                )
            },
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (image.format != ImageFormat.YUV_420_888) return

            val luminance = image.planes.first().buffer.let { buffer ->
                buffer.rewind()
                ByteArray(buffer.remaining()).also(buffer::get)
            }
            val source = PlanarYUVLuminanceSource(
                luminance,
                image.planes.first().rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false,
            )
            decodeAtAnyRotation(source)?.let(onBarcodeDetected)
        } finally {
            image.close()
        }
    }

    private fun decodeAtAnyRotation(source: LuminanceSource): String? {
        var candidate = source
        repeat(4) { rotation ->
            try {
                return reader.decodeWithState(BinaryBitmap(HybridBinarizer(candidate))).text
            } catch (_: Exception) {
                // Product codes may be printed in any orientation relative to the sensor.
            } finally {
                reader.reset()
            }
            if (rotation < 3 && candidate.isRotateSupported) {
                candidate = candidate.rotateCounterClockwise()
            }
        }
        return null
    }
}
