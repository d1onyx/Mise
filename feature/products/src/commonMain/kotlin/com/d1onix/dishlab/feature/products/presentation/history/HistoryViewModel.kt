package com.d1onix.dishlab.feature.products.presentation.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.ClearScanHistoryUseCase
import com.d1onix.dishlab.domain.ObserveScanHistoryUseCase
import com.d1onix.dishlab.domain.model.Product
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

@Immutable
data class HistoryUiState(
    val products: List<Product> = emptyList(),
)

sealed interface HistoryAction {
    data class ProductClicked(val id: ProductId) : HistoryAction
    data object ClearClicked : HistoryAction
    data object BackClicked : HistoryAction
}

@Inject
class HistoryViewModel(
    dependencies: CommonDependencies,
    observeHistory: ObserveScanHistoryUseCase,
    private val clearHistory: ClearScanHistoryUseCase,
    private val session: ScanSessionStore,
    private val router: ProductsRouter,
) : AbstractViewModel(dependencies), WithMviState<HistoryUiState> {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeHistory().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.ProductClicked -> openGraphWith(action.id)
            HistoryAction.ClearClicked -> clear()
            HistoryAction.BackClicked -> router.goBack()
        }
    }

    /** Opening a past scan starts a fresh combination from that product alone. */
    private fun openGraphWith(id: ProductId) = launch("openHistoryProduct") {
        session.reset(listOf(id))
        router.openCombinationGraph()
    }

    private fun clear() = launch("clearHistory") { clearHistory() }
}
