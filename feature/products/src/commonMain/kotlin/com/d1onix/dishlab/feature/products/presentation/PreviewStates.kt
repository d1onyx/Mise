package com.d1onix.dishlab.feature.products.presentation

import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewUiState
import com.d1onix.dishlab.feature.products.presentation.comparison.ComparisonUiState
import com.d1onix.dishlab.feature.products.presentation.graph.GraphUiState
import com.d1onix.dishlab.feature.products.presentation.history.HistoryUiState

/** See `HomePreviewStates` for why the fixtures are common and the previews are not. */
internal object GraphPreviewStates {

    val Default = GraphUiState(
        products = previewProducts(),
        connections = previewConnections(),
    )

    /** Nothing scanned yet — the state a fresh install opens in. */
    val Empty = GraphUiState()

    /** A node tapped, so the detail sheet is up over the graph. */
    val SheetOpen = GraphUiState(
        products = previewProducts(),
        connections = previewConnections(),
        selectedId = previewProducts().last().id,
    )

    val SheetFullData = GraphUiState(products = listOf(previewOats), selectedId = previewOats.id)

    val SheetMinimumFields = GraphUiState(products = listOf(previewHoney), selectedId = previewHoney.id)

    val SheetNotInDatabase = GraphUiState(products = listOf(previewUnknownProduct), selectedId = previewUnknownProduct.id)

    val EditingConnections = GraphUiState(
        products = previewProducts(),
        connections = emptySet(),
        isEditingConnections = true,
        pendingConnectionId = previewProducts().last().id,
    )

    private fun previewConnections() = setOf(
        ProductConnection.between(previewOats.id, previewHoney.id)
    )
}

internal object HistoryPreviewStates {

    val Default = HistoryUiState(products = previewProducts())

    val Empty = HistoryUiState()
}

internal object ConnectionOverviewPreviewStates {
    val Default = ConnectionOverviewUiState(
        products = previewProducts(),
        focusedProductId = previewProducts().first().id,
        directConnectionIds = setOf(previewProducts().last().id),
        connectionCount = 1,
    )
}

internal object ComparisonPreviewStates {
    val Default = ComparisonUiState(
        products = previewProducts(),
        selectedForGraph = setOf(previewOats.id),
    )
    val Empty = ComparisonUiState()
}
