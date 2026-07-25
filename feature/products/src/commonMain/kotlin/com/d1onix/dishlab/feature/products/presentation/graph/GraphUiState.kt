package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId

/**
 * `@Immutable` is the promise that makes this skippable in Compose: the state is
 * only ever replaced, never mutated in place — including the product list.
 */
@Immutable
data class GraphUiState(
    val products: List<Product> = emptyList(),
    val selectedId: ProductId? = null,
    /** Set when the catalogue has nothing left to add; the text is a resource. */
    val showCatalogueExhausted: Boolean = false,
    val showProfileHint: Boolean = false,
) {
    val selected: Product? get() = products.firstOrNull { it.id == selectedId }
}

sealed interface GraphAction {
    data class NodeClicked(val id: ProductId) : GraphAction
    data class RemoveClicked(val id: ProductId) : GraphAction
    data object EmptySpaceClicked : GraphAction
    data object ScanMoreClicked : GraphAction
    data object FindRecipesClicked : GraphAction
    data object SavedClicked : GraphAction
    data object ProfileClicked : GraphAction
    data object BackClicked : GraphAction
    data object SheetDismissed : GraphAction
    data object MessageShown : GraphAction
}
