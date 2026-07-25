package com.d1onix.dishlab.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d1onix.dishlab.designsystem.anim.MiseEasing
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/**
 * The animated progress ring around a score. The sweep animates from 0 to the
 * score once, mirroring the prototype's `stroke-dashoffset` transition.
 */
@Composable
fun ScoreRing(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 64,
    strokeWidth: Int = 6,
    content: @Composable (() -> Unit)? = null,
) {
    val sweep = remember(score) { Animatable(0f) }
    LaunchedEffect(score) {
        sweep.animateTo(score / 100f, tween(800, easing = MiseEasing))
    }

    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = Stroke(width = strokeWidth.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val inset = strokeWidth.dp.toPx() / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - inset * 2,
                this.size.height - inset * 2,
            )
            drawArc(
                color = Color.White.copy(alpha = 0.09f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            // Glow first, then the crisp arc on top — cheaper than a real blur.
            drawArc(
                color = color.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f * sweep.value,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = (strokeWidth * 2.4f).dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                blendMode = BlendMode.Plus,
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * sweep.value,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        content?.invoke()
    }
}

/** «Buy» / «Maybe» / «Skip» chip filled with the verdict colour. */
@Composable
fun VerdictBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MiseTheme.typography.bodySmall,
        color = MiseTheme.colors.onLime,
        modifier = modifier
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** The small numeric badge clipped to the bottom-right of a product node. */
@Composable
fun ScoreBadge(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color, RoundedCornerShape(11.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score.toString(),
            style = MiseTheme.typography.monoSmall.copy(fontSize = 11.sp),
            color = MiseTheme.colors.onLime,
        )
    }
}
