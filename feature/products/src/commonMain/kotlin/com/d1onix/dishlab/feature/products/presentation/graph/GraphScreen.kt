package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
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
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductDetailSheet
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductNode
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductNodeSize
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.graph_back
import com.d1onix.dishlab.feature.products.resources.graph_compare
import com.d1onix.dishlab.feature.products.resources.graph_count_many
import com.d1onix.dishlab.feature.products.resources.graph_count_one
import com.d1onix.dishlab.feature.products.resources.graph_edit_connections
import com.d1onix.dishlab.feature.products.resources.graph_empty
import com.d1onix.dishlab.feature.products.resources.graph_find_recipes
import com.d1onix.dishlab.feature.products.resources.graph_hint
import com.d1onix.dishlab.feature.products.resources.graph_hint_connect
import com.d1onix.dishlab.feature.products.resources.graph_hint_edit
import com.d1onix.dishlab.feature.products.resources.graph_saved
import com.d1onix.dishlab.feature.products.resources.graph_scan_more
import com.d1onix.dishlab.feature.products.resources.graph_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphScreen(viewModel: GraphViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GraphContent(state = state, onAction = viewModel::onAction)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun GraphContent(
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
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    GraphTitleButton(
                        onClick = { onAction(GraphAction.ConnectionOverviewClicked) },
                    )
                    Text(
                        text = countLabel(state.products.size),
                        style = MiseTheme.typography.monoSmall,
                        color = colors.violet,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiseIconCircleButton(
                        icon = MiseIcons.Scissors,
                        contentDescription = stringResource(Res.string.graph_edit_connections),
                        onClick = { onAction(GraphAction.ConnectionEditingToggled) },
                        iconSize = 16,
                        tint = if (state.isEditingConnections) colors.onLime else colors.violet,
                        borderColor = if (state.isEditingConnections) colors.lime else colors.border,
                        background = if (state.isEditingConnections) colors.lime else colors.panel,
                    )
                    MiseIconCircleButton(
                        icon = MiseIcons.Bookmark,
                        contentDescription = stringResource(Res.string.graph_compare),
                        onClick = { onAction(GraphAction.ComparisonClicked) },
                        iconSize = 16,
                        tint = colors.violet,
                        borderColor = colors.border,
                        background = colors.panel,
                    )
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
            }

            GraphCanvas(
                products = state.products,
                connections = state.connections,
                savedPositions = state.positions,
                selectedId = state.selectedId,
                isEditingConnections = state.isEditingConnections,
                pendingConnectionId = state.pendingConnectionId,
                reduceMotion = state.reduceMotion,
                showProductScores = state.showProductScores,
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
                        text = state.profileInitials,
                        style = MiseTheme.typography.monoSmall,
                        color = colors.textMuted,
                    )
                }
            }
        }

        val product = state.selected
        if (product != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onAction(GraphAction.SheetDismissed) },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = Color.Transparent,
                contentColor = colors.text,
                tonalElevation = 0.dp,
                scrimColor = Color.Transparent,
                dragHandle = null,
            ) {
                ProductDetailSheet(
                    product = product,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun GraphTitleButton(onClick: () -> Unit) {
    val colors = MiseTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.graph_title),
            style = MiseTheme.typography.titleSmall,
            color = colors.text,
        )
        Icon(
            imageVector = MiseIcons.ChevronDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = colors.textMuted,
        )
    }
}

/**
 * The playfield: a dotted grid, user-defined recipe constraints and draggable
 * product nodes. Connection editing is an explicit mode, so normal node taps
 * continue to open details and dragging keeps its original behavior.
 *
 * Dragging remains local for frame-by-frame updates. Only the normalized final
 * position is sent to storage, so restoring works across different canvas sizes.
 */
@Composable
private fun GraphCanvas(
    products: List<Product>,
    connections: Set<ProductConnection>,
    savedPositions: Map<ProductId, ProductGraphPosition>,
    selectedId: ProductId?,
    isEditingConnections: Boolean,
    pendingConnectionId: ProductId?,
    reduceMotion: Boolean,
    showProductScores: Boolean,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val density = LocalDensity.current
    val drift = if (reduceMotion) {
        0f
    } else {
        rememberSweep(durationMillis = 7000, label = "graphDrift").value
    }
    val edgePulse = if (reduceMotion) {
        0.72f
    } else {
        rememberPulse(durationMillis = 3200, from = 0.35f, to = 0.85f, label = "edge").value
    }

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
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val nodePx = with(density) { ProductNodeSize.toPx() }
        val maxX = (widthPx - nodePx).coerceAtLeast(0f)
        val maxY = (heightPx - nodePx * 1.6f).coerceAtLeast(0f)

        // Pixel positions stay local while dragging; Room stores normalized values.
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

        fun normalized(position: Offset): ProductGraphPosition = ProductGraphPosition(
            xFraction = if (maxX == 0f) 0f else (position.x / maxX).coerceIn(0f, 1f),
            yFraction = if (maxY == 0f) 0f else (position.y / maxY).coerceIn(0f, 1f),
        )

        fun drifted(id: ProductId, index: Int, dragging: Boolean): Offset {
            val base = basePosition(id, index)
            if (dragging) return base
            val phase = drift * 2f * PI.toFloat() + index * 1.7f
            return Offset(base.x + sin(phase) * 4f, base.y + cos(phase * 0.85f) * 4f)
        }

        val dragging = remember { mutableStateMapOf<ProductId, Boolean>() }
        val productIndices = remember(products) {
            products.mapIndexed { index, product -> product.id to index }.toMap()
        }

        LaunchedEffect(products, savedPositions, maxX, maxY) {
            positions.keys.filter { it !in productIndices }.forEach(positions::remove)
            products.forEachIndexed { index, product ->
                if (dragging[product.id] == true) return@forEachIndexed
                val saved = savedPositions[product.id]
                val position = if (saved == null) {
                    defaultPosition(index).let {
                        Offset(it.x.coerceIn(0f, maxX), it.y.coerceIn(0f, maxY))
                    }
                } else {
                    Offset(saved.xFraction * maxX, saved.yFraction * maxY)
                }
                positions[product.id] = position
                if (saved == null) {
                    onAction(GraphAction.NodePositionChanged(product.id, normalized(position)))
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(isEditingConnections, connections, products) {
                    detectTapGestures { tap ->
                        if (!isEditingConnections) {
                            onAction(GraphAction.EmptySpaceClicked)
                            return@detectTapGestures
                        }
                        val center = Offset(nodePx / 2, nodePx / 2)
                        val hitRadius = 18.dp.toPx()
                        val hit = connections
                            .mapNotNull { connection ->
                                val fromIndex = productIndices[connection.first]
                                    ?: return@mapNotNull null
                                val toIndex = productIndices[connection.second]
                                    ?: return@mapNotNull null
                                val from = drifted(
                                    connection.first,
                                    fromIndex,
                                    dragging[connection.first] == true,
                                ) + center
                                val to = drifted(
                                    connection.second,
                                    toIndex,
                                    dragging[connection.second] == true,
                                ) + center
                                connection to distanceToSegment(tap, from, to)
                            }
                            .minByOrNull { it.second }
                            ?.takeIf { it.second <= hitRadius }
                            ?.first

                        onAction(
                            if (hit != null) {
                                GraphAction.ConnectionClicked(hit)
                            } else {
                                GraphAction.EmptySpaceClicked
                            }
                        )
                    }
                }
                .drawBehind {
                    val focusedId = if (isEditingConnections) pendingConnectionId else selectedId
                    val focusedProduct = products.firstOrNull { it.id == focusedId }
                    val focusedColor = focusedProduct?.let { Color(it.accentColor) } ?: colors.lime
                    connections.forEach { connection ->
                        val fromIndex = productIndices[connection.first] ?: return@forEach
                        val toIndex = productIndices[connection.second] ?: return@forEach
                        val from = drifted(
                            connection.first,
                            fromIndex,
                            dragging[connection.first] == true,
                        )
                        val to = drifted(
                            connection.second,
                            toIndex,
                            dragging[connection.second] == true,
                        )
                        val center = Offset(nodePx / 2, nodePx / 2)
                        val isFocused = focusedId != null && connection.contains(focusedId)
                        val lineColor = when {
                            isFocused -> focusedColor
                            isEditingConnections -> colors.lime
                            else -> colors.violet
                        }
                        val lineAlpha = when {
                            isFocused -> 0.96f
                            focusedId != null -> 0.12f
                            else -> edgePulse
                        }
                        if (isFocused) {
                            drawLine(
                                color = focusedColor,
                                start = from + center,
                                end = to + center,
                                strokeWidth = 7.dp.toPx(),
                                alpha = 0.14f,
                            )
                        }
                        drawLine(
                            color = lineColor,
                            start = from + center,
                            end = to + center,
                            strokeWidth = if (isFocused) 3.2.dp.toPx() else 1.6.dp.toPx(),
                            alpha = lineAlpha,
                        )
                    }
                }
        )

        products.forEachIndexed { index, product ->
            ProductNode(
                product = product,
                scoreColor = scoreColor(product.verdict),
                showScore = showProductScores,
                selected = if (isEditingConnections) {
                    product.id == pendingConnectionId
                } else {
                    product.id == selectedId
                },
                position = { drifted(product.id, index, dragging[product.id] == true) },
                onDragStart = { dragging[product.id] = true },
                onDragEnd = {
                    dragging[product.id] = false
                    onAction(
                        GraphAction.NodePositionChanged(
                            product.id,
                            normalized(basePosition(product.id, index)),
                        )
                    )
                },
                onDrag = { delta ->
                    val current = basePosition(product.id, index)
                    positions[product.id] = Offset(
                        x = (current.x + delta.x).coerceIn(0f, maxX),
                        y = (current.y + delta.y).coerceIn(0f, maxY),
                    )
                },
                onClick = {
                    onAction(
                        if (isEditingConnections) {
                            GraphAction.ConnectionNodeClicked(product.id)
                        } else {
                            GraphAction.NodeClicked(product.id)
                        }
                    )
                },
            )
        }

        Text(
            text = stringResource(
                when {
                    pendingConnectionId != null -> Res.string.graph_hint_connect
                    isEditingConnections -> Res.string.graph_hint_edit
                    else -> Res.string.graph_hint
                }
            ),
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

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val segment = end - start
    val lengthSquared = segment.x * segment.x + segment.y * segment.y
    if (lengthSquared == 0f) return (point - start).getDistance()
    val fromStart = point - start
    val projection = (
        (fromStart.x * segment.x + fromStart.y * segment.y) / lengthSquared
        ).coerceIn(0f, 1f)
    return (point - (start + segment * projection)).getDistance()
}
