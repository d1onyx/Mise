package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.anim.rememberSweep
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The drifting constellation behind the home screen. Positions are fractions of
 * the canvas so it adapts to any screen, and the whole thing is drawn in one
 * [Canvas] — the animation never leaves the draw phase.
 */
@Composable
fun AmbientConstellation(modifier: Modifier = Modifier) {
    val colors = MiseTheme.colors
    val sweep = rememberSweep(durationMillis = 9000, label = "constellation")

    Canvas(modifier.fillMaxSize()) {
        val phase = sweep.value * 2f * PI.toFloat()
        val points = ConstellationDots.mapIndexed { index, (fx, fy) ->
            val drift = phase + index
            Offset(
                x = fx * size.width + cos(drift) * 4f,
                y = fy * size.height + sin(drift) * 6f,
            )
        }
        listOf(0 to 1, 1 to 2, 2 to 3).forEach { (from, to) ->
            drawLine(
                color = colors.violet.copy(alpha = 0.28f),
                start = points[from],
                end = points[to],
                strokeWidth = 0.6.dp.toPx(),
            )
        }
        points.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 2 == 0) colors.violet else colors.lime,
                radius = 1.6.dp.toPx(),
                center = point,
                alpha = 0.5f,
            )
        }
    }
}

private val ConstellationDots: List<Pair<Float, Float>> = listOf(
    0.07f to 0.14f,
    0.20f to 0.34f,
    0.75f to 0.21f,
    0.65f to 0.53f,
    0.30f to 0.64f,
    0.85f to 0.71f,
    0.15f to 0.78f,
)

/**
 * Transient message shown over a screen. [onShown] fires once the message has
 * been up long enough to read, so the caller can clear it from its state.
 */
@Composable
fun MiseToast(
    text: String,
    modifier: Modifier = Modifier,
    onShown: (() -> Unit)? = null,
) {
    if (onShown != null) {
        LaunchedEffect(text) {
            delay(MISE_TOAST_MILLIS)
            onShown()
        }
    }
    Box(modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MiseTheme.typography.monoSmall,
            color = Color.White,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.Center)
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private const val MISE_TOAST_MILLIS = 1800L
