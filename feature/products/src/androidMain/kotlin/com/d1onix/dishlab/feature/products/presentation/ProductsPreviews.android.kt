package com.d1onix.dishlab.feature.products.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.designsystem.preview.MiseComponentPreview
import com.d1onix.dishlab.designsystem.preview.MiseScreenPreviews
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewContent
import com.d1onix.dishlab.feature.products.presentation.comparison.ComparisonContent
import com.d1onix.dishlab.feature.products.presentation.graph.GraphContent
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductDetailSheet
import com.d1onix.dishlab.feature.products.presentation.history.HistoryContent

@MiseScreenPreviews
@Composable
private fun GraphContentPreview() {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun GraphContentEmptyPreview() {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.Empty, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun GraphContentSheetPreview() {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.SheetOpen, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun GraphContentExcludedPreview() {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.EditingConnections, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun HistoryContentPreview() {
    MiseTheme {
        HistoryContent(state = HistoryPreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun HistoryContentEmptyPreview() {
    MiseTheme {
        HistoryContent(state = HistoryPreviewStates.Empty, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ConnectionOverviewContentPreview() {
    MiseTheme {
        ConnectionOverviewContent(
            state = ConnectionOverviewPreviewStates.Default,
            onAction = {},
        )
    }
}

@MiseScreenPreviews
@Composable
private fun ComparisonContentPreview() {
    MiseTheme {
        ComparisonContent(state = ComparisonPreviewStates.Default, onAction = {})
    }
}

@MiseScreenPreviews
@Composable
private fun ComparisonContentEmptyPreview() {
    MiseTheme {
        ComparisonContent(state = ComparisonPreviewStates.Empty, onAction = {})
    }
}

/** The sheet on its own, sized to its content rather than to a device. */
@MiseComponentPreview
@Composable
private fun ProductDetailSheetPreview() {
    MiseTheme {
        ProductDetailSheet(product = previewOats, onAction = {})
    }
}

/** Low score, estimated nutrients and alternatives — the busiest version of the sheet. */
@MiseComponentPreview
@Composable
private fun ProductDetailSheetIncompletePreview() {
    MiseTheme {
        ProductDetailSheet(product = previewHoney, onAction = {})
    }
}

@MiseComponentPreview
@Composable
private fun ProductDetailSheetNotInDatabasePreview() {
    MiseTheme {
        ProductDetailSheet(product = previewUnknownProduct, onAction = {})
    }
}
