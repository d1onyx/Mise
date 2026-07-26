package com.d1onix.dishlab.feature.products.presentation.connections

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ConnectionOverviewUiState(
    val products: List<Product> = emptyList(),
    val focusedProductId: ProductId? = null,
    val directConnectionIds: Set<ProductId> = emptySet(),
    val visibleGroup: ConnectionGroup = ConnectionGroup.Direct,
    val connectionCount: Int = 0,
)

enum class ConnectionGroup {
    Direct,
    NotConnected,
}

sealed interface ConnectionOverviewAction {
    data class ProductSelected(val id: ProductId) : ConnectionOverviewAction
    data class GroupSelected(val group: ConnectionGroup) : ConnectionOverviewAction
    data class ConnectionChanged(
        val productId: ProductId,
        val connected: Boolean,
    ) : ConnectionOverviewAction
    data object BackClicked : ConnectionOverviewAction
}

@Inject
class ConnectionOverviewViewModel(
    dependencies: CommonDependencies,
    private val session: ScanSessionStore,
    private val getProducts: GetProductsUseCase,
    private val router: ProductsRouter,
) : AbstractViewModel(dependencies), WithMviState<ConnectionOverviewUiState> {

    private val _uiState = MutableStateFlow(ConnectionOverviewUiState())
    val uiState: StateFlow<ConnectionOverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(session.products, session.connections) { ids, edges -> ids to edges }
                .collectLatest { (ids, edges) ->
                    val products = getProducts(ids)
                    _uiState.update { state ->
                        val focusedId = state.focusedProductId
                            ?.takeIf { it in ids }
                            ?: ids.firstOrNull()
                        state.copy(
                            products = products,
                            focusedProductId = focusedId,
                            directConnectionIds = edges.connectedTo(focusedId),
                            connectionCount = edges.size,
                        )
                    }
                }
        }
    }

    fun onAction(action: ConnectionOverviewAction) {
        when (action) {
            is ConnectionOverviewAction.ProductSelected -> _uiState.update { state ->
                state.copy(
                    focusedProductId = action.id,
                    directConnectionIds = session.connections.value.connectedTo(action.id),
                    visibleGroup = ConnectionGroup.Direct,
                )
            }
            is ConnectionOverviewAction.GroupSelected -> _uiState.update {
                it.copy(visibleGroup = action.group)
            }
            is ConnectionOverviewAction.ConnectionChanged -> {
                val focusedId = _uiState.value.focusedProductId ?: return
                viewModelScope.launch {
                    if (action.connected) {
                        session.connect(focusedId, action.productId)
                    } else {
                        session.disconnect(focusedId, action.productId)
                    }
                }
            }
            ConnectionOverviewAction.BackClicked -> router.goBack()
        }
    }

    private fun Set<ProductConnection>.connectedTo(id: ProductId?): Set<ProductId> =
        id?.let { productId -> mapNotNullTo(linkedSetOf()) { it.other(productId) } }.orEmpty()
}
