package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.rememberPulse
import com.d1onix.dishlab.designsystem.anim.rememberSweep
import com.d1onix.dishlab.designsystem.component.MiseCircleButton
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseToast
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductDetailSheet
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductNode
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductNodeSize
import com.d1onix.dishlab.feature.products.presentation.previewProducts
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.graph_back
import com.d1onix.dishlab.feature.products.resources.graph_catalogue_exhausted
import com.d1onix.dishlab.feature.products.resources.graph_count_many
import com.d1onix.dishlab.feature.products.resources.graph_count_one
import com.d1onix.dishlab.feature.products.resources.graph_empty
import com.d1onix.dishlab.feature.products.resources.graph_find_recipes
import com.d1onix.dishlab.feature.products.resources.graph_hint
import com.d1onix.dishlab.feature.products.resources.graph_profile_coming_soon
import com.d1onix.dishlab.feature.products.resources.graph_profile_initials
import com.d1onix.dishlab.feature.products.resources.graph_saved
import com.d1onix.dishlab.feature.products.resources.graph_scan_more
import com.d1onix.dishlab.feature.products.resources.graph_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphScreen(viewModel: GraphViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GraphContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun GraphContent(
    state: GraphUiState,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiseIconCircleButton(
                    icon = MiseIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.graph_back),
                    onClick = { onAction(GraphAction.BackClicked) },
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.graph_title),
                        style = MiseTheme.typography.titleSmall,
                        color = colors.text,
                    )
                    Text(
                        text = countLabel(state.products.size),
                        style = MiseTheme.typography.monoSmall,
                        color = colors.violet,
                    )
                }
                MiseIconCircleButton(
                    icon = MiseIcons.Plus,
                    contentDescription = stringResource(Res.string.graph_scan_more),
                    onClick = { onAction(GraphAction.ScanMoreClicked) },
                    iconSize = 16,
                    tint = colors.lime,
                    borderColor = colors.lime.copy(alpha = 0.4f),
                    background = colors.lime.copy(alpha = 0.1f),
                )
            }

            GraphCanvas(
                products = state.products,
                selectedId = state.selectedId,
                onAction = onAction,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MisePrimaryButton(
                    text = stringResource(Res.string.graph_find_recipes, state.products.size),
                    onClick = { onAction(GraphAction.FindRecipesClicked) },
                    modifier = Modifier.weight(1f),
                )
                MiseIconCircleButton(
                    icon = MiseIcons.Bookmark,
                    contentDescription = stringResource(Res.string.graph_saved),
                    onClick = { onAction(GraphAction.SavedClicked) },
                    size = 56,
                    iconSize = 20,
                    tint = colors.violet,
                    borderColor = colors.border,
                    background = colors.panel,
                )
                MiseCircleButton(
                    onClick = { onAction(GraphAction.ProfileClicked) },
                    size = 56,
                ) {
                    Text(
                        text = stringResource(Res.string.graph_profile_initials),
                        style = MiseTheme.typography.monoSmall,
                        color = colors.textMuted,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.selected != null,
            enter = slideInVertically(tween(350)) { it } + fadeIn(),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val product = state.selected
            if (product != null) {
                ProductDetailSheet(
                    product = product,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val message = when {
            state.showCatalogueExhausted -> stringResource(Res.string.graph_catalogue_exhausted)
            state.showProfileHint -> stringResource(Res.string.graph_profile_coming_soon)
            else -> null
        }
        if (message != null) {
            MiseToast(
                text = message,
                onShown = { onAction(GraphAction.MessageShown) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 70.dp),
            )
        }
    }
}

/**
 * The playfield: a dotted grid, glowing edges between every pair of products and
 * the draggable nodes themselves.
 *
 * Node positions live here rather than in the view-model — they are a gesture
 * artefact, not app state — and every frame of drift or drag is read inside a
 * draw or layout lambda, so nothing above recomposes while the graph moves.
 */
@Composable
private fun GraphCanvas(
    products: List<Product>,
    selectedId: ProductId?,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val density = LocalDensity.current
    val drift = rememberSweep(durationMillis = 7000, label = "graphDrift")
    val edgePulse = rememberPulse(durationMillis = 3200, from = 0.35f, to = 0.85f, label = "edge")

    BoxWithConstraints(
        modifier
            .drawBehind {
                val step = 22.dp.toPx()
                val dot = 1.dp.toPx()
                var y = step
                while (y < size.height) {
                    var x = step
                    while (x < size.width) {
                        drawCircle(Color.White, radius = dot, center = Offset(x, y), alpha = 0.05f)
                        x += step
                    }
                    y += step
                }
            }
            .pointerInput(Unit) { detectTapGestures { onAction(GraphAction.EmptySpaceClicked) } }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val nodePx = with(density) { ProductNodeSize.toPx() }

        // Only dragged nodes are stored; everything else falls back to a ring
        // around the centre, so nothing is written to state during composition.
        val positions = remember { mutableStateMapOf<ProductId, Offset>() }

        fun defaultPosition(index: Int): Offset {
            val angle = index * 2.4f
            val radius = minOf(widthPx, heightPx) * 0.22f
            return Offset(
                x = widthPx / 2 - nodePx / 2 + cos(angle) * radius,
                y = heightPx / 2 - nodePx / 2 + sin(angle) * radius,
            )
        }

        fun basePosition(id: ProductId, index: Int): Offset =
            positions[id] ?: defaultPosition(index)

        fun drifted(id: ProductId, index: Int, dragging: Boolean): Offset {
            val base = basePosition(id, index)
            if (dragging) return base
            val phase = drift.value * 2f * PI.toFloat() + index * 1.7f
            return Offset(base.x + sin(phase) * 4f, base.y + cos(phase * 0.85f) * 4f)
        }

        val dragging = remember { mutableStateMapOf<ProductId, Boolean>() }

        Box(
            Modifier.fillMaxSize().drawBehind {
                products.forEachIndexed { i, first ->
                    products.drop(i + 1).forEachIndexed { j, second ->
                        val from = drifted(first.id, i, dragging[first.id] == true)
                        val to = drifted(second.id, i + j + 1, dragging[second.id] == true)
                        val center = Offset(nodePx / 2, nodePx / 2)
                        drawLine(
                            color = colors.violet,
                            start = from + center,
                            end = to + center,
                            strokeWidth = 1.6.dp.toPx(),
                            alpha = edgePulse.value,
                        )
                    }
                }
            }
        )

        products.forEachIndexed { index, product ->
            ProductNode(
                product = product,
                scoreColor = scoreColor(product.verdict),
                selected = product.id == selectedId,
                position = { drifted(product.id, index, dragging[product.id] == true) },
                onDragStart = { dragging[product.id] = true },
                onDragEnd = { dragging[product.id] = false },
                onDrag = { delta ->
                    val current = basePosition(product.id, index)
                    positions[product.id] = Offset(
                        x = (current.x + delta.x).coerceIn(0f, (widthPx - nodePx).coerceAtLeast(0f)),
                        y = (current.y + delta.y).coerceIn(0f, (heightPx - nodePx * 1.6f).coerceAtLeast(0f)),
                    )
                },
                onClick = { onAction(GraphAction.NodeClicked(product.id)) },
            )
        }

        Text(
            text = stringResource(Res.string.graph_hint),
            style = MiseTheme.typography.monoSmall,
            color = colors.textDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        if (products.isEmpty()) {
            Column(
                Modifier.align(Alignment.Center).width(240.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(MiseIcons.Barcode, null, Modifier.size(28.dp), tint = colors.textDim)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(Res.string.graph_empty),
                    style = MiseTheme.typography.body,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun countLabel(count: Int): String =
    if (count == 1) {
        stringResource(Res.string.graph_count_one)
    } else {
        stringResource(Res.string.graph_count_many, count)
    }

@Preview
@Composable
private fun GraphContentPreview() {
    MiseTheme {
        GraphContent(state = GraphUiState(products = previewProducts()), onAction = {})
    }
}

/** Nothing scanned yet — the state a fresh install opens in. */
@Preview
@Composable
private fun GraphContentEmptyPreview() {
    MiseTheme {
        GraphContent(state = GraphUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun GraphContentSheetPreview() {
    val products = previewProducts()
    MiseTheme {
        GraphContent(
            state = GraphUiState(products = products, selectedId = products.last().id),
            onAction = {},
        )
    }
}
