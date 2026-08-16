package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.d1onix.dishlab.designsystem.icon.MiseIcons

/**
 * A tappable product image that opens in a full-screen viewer. The viewer
 * supports pinch zoom and panning, and resets its zoom state when dismissed.
 */
@Composable
fun ZoomableProductImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
) {
    var isViewerOpen by remember { mutableStateOf(false) }

    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = { isViewerOpen = true })
            .padding(contentPadding),
    )

    if (isViewerOpen) {
        ZoomableProductImageDialog(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            onDismiss = { isViewerOpen = false },
        )
    }
}

@Composable
private fun ZoomableProductImageDialog(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        val maxTranslationX = viewportSize.width * (nextScale - 1f) / 2f
        val maxTranslationY = viewportSize.height * (nextScale - 1f) / 2f
        offset = if (nextScale == 1f) {
            Offset.Zero
        } else {
            Offset(
                x = (offset.x + panChange.x).coerceIn(-maxTranslationX, maxTranslationX),
                y = (offset.y + panChange.y).coerceIn(-maxTranslationY, maxTranslationY),
            )
        }
        scale = nextScale
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .transformable(transformableState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            MiseIconCircleButton(
                icon = MiseIcons.Close,
                contentDescription = "Close photo",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(16.dp)
                    .size(44.dp),
                size = 44,
                iconSize = 20,
                tint = Color.White,
                borderColor = Color.White.copy(alpha = 0.45f),
                background = Color.Black.copy(alpha = 0.45f),
            )
        }
    }
}
