package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.SuggestNextProductUseCase
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.products.navigation.ProductsRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class GraphViewModel(
    dependencies: CommonDependencies,
    private val session: ScanSessionStore,
    private val getProducts: GetProductsUseCase,
    private val suggestNextProduct: SuggestNextProductUseCase,
    private val recordScan: RecordScanUseCase,
    private val router: ProductsRouter,
) : AbstractViewModel(dependencies), WithMviState<GraphUiState> {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.products.collect { ids ->
                val products = getProducts(ids)
                _uiState.update { state ->
                    state.copy(
                        products = products,
                        // Keep the sheet open only while its product is still on the graph.
                        selectedId = state.selectedId?.takeIf { id -> products.any { it.id == id } },
                    )
                }
            }
        }
    }

    fun onAction(action: GraphAction) {
        when (action) {
            is GraphAction.NodeClicked -> _uiState.update { it.copy(selectedId = action.id) }
            is GraphAction.RemoveClicked -> session.remove(action.id)
            GraphAction.EmptySpaceClicked -> addNextProduct()
            GraphAction.ScanMoreClicked -> router.openScanner()
            GraphAction.FindRecipesClicked -> router.openRecipes()
            GraphAction.SavedClicked -> router.openSavedRecipes()
            GraphAction.BackClicked -> router.goBack()
            GraphAction.SheetDismissed -> _uiState.update { it.copy(selectedId = null) }
            GraphAction.ProfileClicked -> _uiState.update { it.copy(showProfileHint = true) }
            GraphAction.MessageShown -> _uiState.update {
                it.copy(showCatalogueExhausted = false, showProfileHint = false)
            }
        }
    }

    /** Tapping empty canvas adds the next product that is not on the graph yet. */
    private fun addNextProduct() = launch("addProduct") {
        val next = suggestNextProduct(session.products.value)
        if (next == null) {
            _uiState.update { it.copy(showCatalogueExhausted = true) }
        } else {
            session.add(next.id)
            recordScan(next.id)
            _uiState.update { it.copy(selectedId = next.id) }
        }
    }
}
