package com.d1onix.dishlab.feature.products.presentation.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.ProductAvatar
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.connections_direct
import com.d1onix.dishlab.feature.products.resources.connections_direct_status
import com.d1onix.dishlab.feature.products.resources.connections_connect
import com.d1onix.dishlab.feature.products.resources.connections_disconnect
import com.d1onix.dishlab.feature.products.resources.connections_empty
import com.d1onix.dishlab.feature.products.resources.connections_missing
import com.d1onix.dishlab.feature.products.resources.connections_missing_status
import com.d1onix.dishlab.feature.products.resources.connections_none_direct
import com.d1onix.dishlab.feature.products.resources.connections_none_missing
import com.d1onix.dishlab.feature.products.resources.connections_product_picker
import com.d1onix.dishlab.feature.products.resources.connections_summary
import com.d1onix.dishlab.feature.products.resources.connections_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun ConnectionOverviewScreen(viewModel: ConnectionOverviewViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectionOverviewContent(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun ConnectionOverviewContent(
    state: ConnectionOverviewUiState,
    onAction: (ConnectionOverviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val focusedProduct = state.products.firstOrNull { it.id == state.focusedProductId }
    val directProducts = state.products.filter { it.id in state.directConnectionIds }
    val missingProducts = state.products.filter {
        it.id != state.focusedProductId && it.id !in state.directConnectionIds
    }
    val visibleProducts = when (state.visibleGroup) {
        ConnectionGroup.Direct -> directProducts
        ConnectionGroup.NotConnected -> missingProducts
    }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        MiseScreenHeader(
            title = stringResource(Res.string.connections_title),
            onBackClick = { onAction(ConnectionOverviewAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(
                Res.string.connections_summary,
                state.products.size,
                state.connectionCount,
            ),
            style = MiseTheme.typography.monoSmall,
            color = colors.violet,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(18.dp))

        if (focusedProduct == null) {
            EmptyState(stringResource(Res.string.connections_empty), Modifier.fillMaxWidth())
            return@Column
        }

        SectionLabel(
            text = stringResource(Res.string.connections_product_picker),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.products, key = { it.id.value }) { product ->
                ProductPickerItem(
                    product = product,
                    selected = product.id == state.focusedProductId,
                    onClick = {
                        onAction(ConnectionOverviewAction.ProductSelected(product.id))
                    },
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        FocusedProductPanel(
            product = focusedProduct,
            directCount = directProducts.size,
            missingCount = missingProducts.size,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))

        ConnectionGroupControl(
            selected = state.visibleGroup,
            directCount = directProducts.size,
            missingCount = missingProducts.size,
            onSelected = { onAction(ConnectionOverviewAction.GroupSelected(it)) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))

        if (visibleProducts.isEmpty()) {
            EmptyState(
                text = stringResource(
                    if (state.visibleGroup == ConnectionGroup.Direct) {
                        Res.string.connections_none_direct
                    } else {
                        Res.string.connections_none_missing
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 28.dp,
                ),
            ) {
                items(visibleProducts, key = { it.id.value }) { product ->
                    ConnectionProductRow(
                        product = product,
                        connected = state.visibleGroup == ConnectionGroup.Direct,
                        onClick = {
                            onAction(ConnectionOverviewAction.ProductSelected(product.id))
                        },
                        onConnectionChange = {
                            onAction(
                                ConnectionOverviewAction.ConnectionChanged(
                                    productId = product.id,
                                    connected = state.visibleGroup == ConnectionGroup.NotConnected,
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductPickerItem(
    product: Product,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiseTheme.colors
    val accent = Color(product.accentColor)
    MisePanel(
        modifier = Modifier.width(92.dp),
        cornerRadius = 8,
        background = if (selected) accent.copy(alpha = 0.10f) else colors.panel,
        borderColor = if (selected) accent.copy(alpha = 0.75f) else colors.border,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        onClick = onClick,
        singleUse = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProductAvatar(product.initial, accent, size = 34)
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                style = MiseTheme.typography.monoTiny,
                color = if (selected) colors.text else colors.textMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FocusedProductPanel(
    product: Product,
    directCount: Int,
    missingCount: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val accent = Color(product.accentColor)
    MisePanel(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 8,
        background = accent.copy(alpha = 0.08f),
        borderColor = accent.copy(alpha = 0.45f),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProductAvatar(product.initial, accent, size = 48)
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MiseTheme.typography.titleSmall, color = colors.text)
                Text(product.category, style = MiseTheme.typography.monoTiny, color = colors.textMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$directCount", style = MiseTheme.typography.titleSmall, color = colors.lime)
                Text(
                    stringResource(Res.string.connections_direct),
                    style = MiseTheme.typography.monoTiny,
                    color = colors.textMuted,
                )
                Text(
                    "$missingCount ${stringResource(Res.string.connections_missing)}",
                    style = MiseTheme.typography.monoTiny,
                    color = colors.textDim,
                )
            }
        }
    }
}

@Composable
private fun ConnectionGroupControl(
    selected: ConnectionGroup,
    directCount: Int,
    missingCount: Int,
    onSelected: (ConnectionGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConnectionGroupButton(
            text = "${stringResource(Res.string.connections_direct)} · $directCount",
            selected = selected == ConnectionGroup.Direct,
            onClick = { onSelected(ConnectionGroup.Direct) },
            modifier = Modifier.weight(1f),
        )
        ConnectionGroupButton(
            text = "${stringResource(Res.string.connections_missing)} · $missingCount",
            selected = selected == ConnectionGroup.NotConnected,
            onClick = { onSelected(ConnectionGroup.NotConnected) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ConnectionGroupButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = modifier,
        cornerRadius = 8,
        background = if (selected) colors.violet.copy(alpha = 0.12f) else colors.panel,
        borderColor = if (selected) colors.violet.copy(alpha = 0.65f) else colors.border,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
        onClick = onClick,
        singleUse = false,
    ) {
        Text(
            text = text,
            style = MiseTheme.typography.monoTiny,
            color = if (selected) colors.violet else colors.textMuted,
        )
    }
}

@Composable
private fun ConnectionProductRow(
    product: Product,
    connected: Boolean,
    onClick: () -> Unit,
    onConnectionChange: () -> Unit,
) {
    val colors = MiseTheme.colors
    val accent = Color(product.accentColor)
    val actionColor = if (connected) colors.red else colors.lime
    val actionLabel = stringResource(
        if (connected) Res.string.connections_disconnect else Res.string.connections_connect
    )
    ConnectionSwipeRow(
        connected = connected,
        label = actionLabel,
        color = actionColor,
        onConnectionChange = onConnectionChange,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ConnectionRowHeight)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductAvatar(product.initial, accent, size = 40)
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MiseTheme.typography.body, color = colors.text)
                    Text(
                        text = stringResource(
                            if (connected) {
                                Res.string.connections_direct_status
                            } else {
                                Res.string.connections_missing_status
                            }
                        ),
                        style = MiseTheme.typography.monoTiny,
                        color = if (connected) colors.lime else colors.textMuted,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = MiseIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.textDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                Modifier
                    .padding(start = 72.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ConnectionSwipeRow(
    connected: Boolean,
    label: String,
    color: Color,
    onConnectionChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = remember { AnchoredDraggableState(ConnectionSwipeValue.Closed) }
    val offset = state.offset.takeUnless(Float::isNaN) ?: 0f

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == ConnectionSwipeValue.Delete) onConnectionChange()
    }

    Box(
        modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
            state.updateAnchors(
                DraggableAnchors {
                    ConnectionSwipeValue.Closed at 0f
                    ConnectionSwipeValue.Revealed at -size.width * 0.5f
                    ConnectionSwipeValue.Delete at -size.width.toFloat()
                }
            )
        },
    ) {
        if (offset < 0f) {
            SwipeConnectionAction(
                connected = connected,
                label = label,
                color = color,
                onClick = onConnectionChange,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offset.roundToInt(), 0) }
                .clip(if (offset < 0f) ConnectionSwipeShape else RectangleShape)
                .background(MiseTheme.colors.background)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
                ),
        ) { content() }
    }
}

private enum class ConnectionSwipeValue { Closed, Revealed, Delete }

private val ConnectionSwipeShape = RoundedCornerShape(14.dp)
private val ConnectionRowHeight = 64.dp

@Composable
private fun SwipeConnectionAction(
    connected: Boolean,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (connected) MiseIcons.Scissors else MiseIcons.Plus,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Text(label, style = MiseTheme.typography.monoSmall, color = color)
        }
    }
}
