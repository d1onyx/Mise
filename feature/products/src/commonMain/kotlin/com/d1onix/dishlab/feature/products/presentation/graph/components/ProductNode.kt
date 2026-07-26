package com.d1onix.dishlab.feature.products.presentation.graph.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.d1onix.dishlab.designsystem.anim.rememberPulse
import com.d1onix.dishlab.designsystem.component.ScoreBadge
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import kotlin.math.roundToInt

/** Stable layout size; its center is also the center of the 66dp product circle. */
val ProductNodeSize = 78.dp
private val ProductNodeCircleSize = 66.dp

/**
 * A draggable product token.
 *
 * [position] is read inside the `offset` lambda, so dragging and the idle drift
 * only re-run layout — the node never recomposes while it moves.
 */
@Composable
fun ProductNode(
    product: Product,
    scoreColor: Color,
    selected: Boolean,
    position: () -> Offset,
    onDrag: (Offset) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val accent = Color(product.accentColor)
    val ring = rememberPulse(durationMillis = 1800, from = 0f, to = 1f, label = "nodeRing")
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnClick by rememberUpdatedState(onClick)

    Column(
        modifier = modifier
            .width(ProductNodeSize)
            .offset { position().let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
            .pointerInput(product.id) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            }
            .pointerInput(product.id) {
                detectTapGestures { currentOnClick() }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(ProductNodeSize),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(ProductNodeSize)
                        .drawBehind {
                            // Expanding, fading ring — the prototype's `nodeRing`.
                            drawCircle(
                                color = scoreColor,
                                radius = size.minDimension / 2 * (1f + ring.value * 0.12f),
                                alpha = (1f - ring.value) * 0.55f,
                                style = Stroke(1.5.dp.toPx()),
                            )
                        }
                )
            }
            Box(
                Modifier
                    .size(ProductNodeCircleSize)
                    .background(colors.surface, CircleShape)
                    .border(2.dp, accent, CircleShape)
                    .drawBehind {
                        drawCircle(color = accent, alpha = if (selected) 0.22f else 0.10f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = product.initial,
                    style = MiseTheme.typography.monoDisplay.copy(fontSize = 22.sp),
                    color = accent,
                )
            }
            ScoreBadge(
                score = product.score,
                color = scoreColor,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 5.dp),
            )
        }
        Text(
            text = product.name,
            style = MiseTheme.typography.bodySmall,
            color = colors.text,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
