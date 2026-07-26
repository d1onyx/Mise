package com.d1onix.dishlab.designsystem.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/** `cubic-bezier(.2,.8,.2,1)` from the prototype — the app's default ease-out. */
val MiseEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * The `screenIn` keyframe: fade up 14dp on first composition.
 *
 * The animated values are read inside [graphicsLayer]'s lambda, so the entrance
 * only invalidates the draw phase and never recomposes the screen it wraps.
 */
@Composable
fun Modifier.screenIn(durationMillis: Int = 450): Modifier {
    if (LocalInspectionMode.current) return this

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, easing = MiseEasing))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

/**
 * A value oscillating between [from] and [to] forever — the `glowPulse`,
 * `edgePulse` and `scanPulse` keyframes.
 */
@Composable
fun rememberPulse(
    durationMillis: Int,
    from: Float = 0.35f,
    to: Float = 0.9f,
    label: String = "pulse",
): State<Float> {
    if (LocalInspectionMode.current) return rememberUpdatedState(to)

    val transition = rememberInfiniteTransition(label)
    return transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = pulseSpec(durationMillis),
        label = label,
    )
}

/** A value sweeping 0..1 and restarting — drives the scan line and node drift. */
@Composable
fun rememberSweep(durationMillis: Int, label: String = "sweep"): State<Float> {
    if (LocalInspectionMode.current) return rememberUpdatedState(0.5f)

    val transition = rememberInfiniteTransition(label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis, easing = LinearEasing)),
        label = label,
    )
}

private fun pulseSpec(durationMillis: Int): InfiniteRepeatableSpec<Float> =
    infiniteRepeatable(
        animation = tween(durationMillis / 2, easing = MiseEasing),
        repeatMode = RepeatMode.Reverse,
    )
