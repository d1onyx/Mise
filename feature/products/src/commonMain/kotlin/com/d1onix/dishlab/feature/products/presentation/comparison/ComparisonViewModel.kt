package com.d1onix.dishlab.feature.products.presentation.comparison

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ProductComparisonStore
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.products.navigation.ProductsRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ComparisonUiState(
    val products: List<Product> = emptyList(),
    val selectedForGraph: Set<ProductId> = emptySet(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) {
    val canAddProduct: Boolean get() = products.size < ProductComparisonStore.MAX_PRODUCTS
    val bestProductId: ProductId? get() = products.maxByOrNull(Product::score)?.id
    val allSelected: Boolean get() = products.isNotEmpty() && selectedForGraph.size == products.size
}

sealed interface ComparisonAction {
    data object AddProductClicked : ComparisonAction
    data class ProductSelectionToggled(val id: ProductId) : ComparisonAction
    data class ProductRemoved(val id: ProductId) : ComparisonAction
    data object AddSelectedToGraphClicked : ComparisonAction
    data object SelectAllToggled : ComparisonAction
    data object ClearClicked : ComparisonAction
    data object BackClicked : ComparisonAction
}

@Inject
class ComparisonViewModel(
    dependencies: CommonDependencies,
    private val getProducts: GetProductsUseCase,
    private val comparison: ProductComparisonStore,
    private val graph: ScanSessionStore,
    private val router: ProductsRouter,
) : AbstractViewModel(dependencies), WithMviState<ComparisonUiState> {
    private val mutableState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            comparison.products.collectLatest { ids ->
                mutableState.update { it.copy(isLoading = true, hasError = false) }
                try {
                    val products = getProducts(ids).sortedBy { ids.indexOf(it.id) }
                    mutableState.update { state ->
                        state.copy(
                            products = products,
                            selectedForGraph = state.selectedForGraph.intersect(ids.toSet()),
                            isLoading = false,
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Throwable) {
                    mutableState.update { it.copy(isLoading = false, hasError = true) }
                }
            }
        }
    }

    fun onAction(action: ComparisonAction) {
        when (action) {
            ComparisonAction.AddProductClicked -> {
                if (mutableState.value.canAddProduct) router.openComparisonScanner()
            }
            is ComparisonAction.ProductSelectionToggled -> mutableState.update { state ->
                state.copy(selectedForGraph = state.selectedForGraph.flip(action.id))
            }
            is ComparisonAction.ProductRemoved -> launch("removeComparisonProduct") {
                comparison.remove(action.id)
            }
            ComparisonAction.AddSelectedToGraphClicked -> addSelectedToGraph()
            ComparisonAction.SelectAllToggled -> mutableState.update { state ->
                state.copy(
                    selectedForGraph = if (state.allSelected) {
                        emptySet()
                    } else {
                        state.products.mapTo(mutableSetOf()) { it.id }
                    },
                )
            }
            ComparisonAction.ClearClicked -> launch("clearComparison") { comparison.clear() }
            ComparisonAction.BackClicked -> router.goBack()
        }
    }

    private fun addSelectedToGraph() {
        val selected = mutableState.value.selectedForGraph
        if (selected.isEmpty()) return
        launch("addComparedProductsToGraph") {
            selected.forEach { graph.add(it) }
            router.openCombinationGraph()
        }
    }
}

private fun <T> Set<T>.flip(value: T): Set<T> = if (value in this) this - value else this + value
