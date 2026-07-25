package com.d1onix.dishlab.feature.products.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.ProductAvatar
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.feature.products.presentation.previewProducts
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.history_clear
import com.d1onix.dishlab.feature.products.resources.history_empty
import com.d1onix.dishlab.feature.products.resources.history_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state = state, onAction = viewModel::onAction)
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        MiseScreenHeader(
            title = stringResource(Res.string.history_title),
            onBackClick = { onAction(HistoryAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            trailing = {
                if (state.products.isNotEmpty()) {
                    MiseTextAction(
                        text = stringResource(Res.string.history_clear),
                        onClick = { onAction(HistoryAction.ClearClicked) },
                        color = colors.red,
                    )
                }
            },
        )

        if (state.products.isEmpty()) {
            EmptyState(stringResource(Res.string.history_empty), Modifier.fillMaxWidth())
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.products, key = { it.id.value }) { product ->
                MisePanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18,
                    onClick = { onAction(HistoryAction.ProductClicked(product.id)) },
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ProductAvatar(
                            initial = product.initial,
                            accent = Color(product.accentColor),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                product.name,
                                style = MiseTheme.typography.body,
                                color = colors.text,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                product.category,
                                style = MiseTheme.typography.monoSmall,
                                color = colors.textMuted,
                            )
                        }
                        Text(
                            product.score.toString(),
                            style = MiseTheme.typography.mono,
                            color = scoreColor(product.verdict),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HistoryContentPreview() {
    MiseTheme {
        HistoryContent(state = HistoryUiState(products = previewProducts()), onAction = {})
    }
}

@Preview
@Composable
private fun HistoryContentEmptyPreview() {
    MiseTheme {
        HistoryContent(state = HistoryUiState(), onAction = {})
    }
}
