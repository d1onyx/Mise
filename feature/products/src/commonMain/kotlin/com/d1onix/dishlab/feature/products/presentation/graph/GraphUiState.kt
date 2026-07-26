package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition

/**
 * `@Immutable` is the promise that makes this skippable in Compose: the state is
 * only ever replaced, never mutated in place — including the product list.
 */
@Immutable
data class GraphUiState(
    val products: List<Product> = emptyList(),
    val connections: Set<ProductConnection> = emptySet(),
    val positions: Map<ProductId, ProductGraphPosition> = emptyMap(),
    val selectedId: ProductId? = null,
    val isEditingConnections: Boolean = false,
    val pendingConnectionId: ProductId? = null,
    val showProfileHint: Boolean = false,
) {
    val selected: Product? get() = products.firstOrNull { it.id == selectedId }
}

sealed interface GraphAction {
    data class NodeClicked(val id: ProductId) : GraphAction
    data class RemoveClicked(val id: ProductId) : GraphAction
    data class ConnectionNodeClicked(val id: ProductId) : GraphAction
    data class ConnectionClicked(val connection: ProductConnection) : GraphAction
    data class NodePositionChanged(
        val id: ProductId,
        val position: ProductGraphPosition,
    ) : GraphAction
    data object ConnectionEditingToggled : GraphAction
    data object ConnectionOverviewClicked : GraphAction
    data object EmptySpaceClicked : GraphAction
    data object ScanMoreClicked : GraphAction
    data object FindRecipesClicked : GraphAction
    data object SavedClicked : GraphAction
    data object ProfileClicked : GraphAction
    data object BackClicked : GraphAction
    data object SheetDismissed : GraphAction
    data object MessageShown : GraphAction
}
