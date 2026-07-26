package com.d1onix.dishlab.feature.products.presentation.graph

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
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

@Inject
class GraphViewModel(
    dependencies: CommonDependencies,
    private val session: ScanSessionStore,
    private val getProducts: GetProductsUseCase,
    private val router: ProductsRouter,
) : AbstractViewModel(dependencies), WithMviState<GraphUiState> {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                session.products,
                session.connections,
                session.positions,
            ) { ids, connections, positions ->
                Triple(ids, connections, positions)
            }.collectLatest { (ids, connections, positions) ->
                val products = getProducts(ids)
                _uiState.update { state ->
                    state.copy(
                        products = products,
                        connections = connections,
                        positions = positions,
                        // Keep the sheet open only while its product is still on the graph.
                        selectedId = state.selectedId?.takeIf { id -> products.any { it.id == id } },
                        pendingConnectionId = state.pendingConnectionId?.takeIf { id ->
                            products.any { it.id == id }
                        },
                    )
                }
            }
        }
    }

    fun onAction(action: GraphAction) {
        when (action) {
            is GraphAction.NodeClicked -> {
                if (_uiState.value.isEditingConnections) {
                    selectConnectionNode(action.id)
                } else {
                    _uiState.update { it.copy(selectedId = action.id) }
                }
            }
            is GraphAction.RemoveClicked -> launch("removeProduct") { session.remove(action.id) }
            is GraphAction.NodePositionChanged -> launch("saveGraphPosition") {
                session.updatePosition(action.id, action.position)
            }
            is GraphAction.ConnectionNodeClicked -> selectConnectionNode(action.id)
            is GraphAction.ConnectionClicked -> {
                if (_uiState.value.isEditingConnections) {
                    launch("disconnectProducts") {
                        session.disconnect(action.connection.first, action.connection.second)
                    }
                    _uiState.update { it.copy(pendingConnectionId = null) }
                }
            }
            GraphAction.ConnectionEditingToggled -> _uiState.update {
                it.copy(
                    isEditingConnections = !it.isEditingConnections,
                    pendingConnectionId = null,
                    selectedId = null,
                )
            }
            GraphAction.ConnectionOverviewClicked -> router.openConnectionOverview()
            GraphAction.EmptySpaceClicked -> {
                if (_uiState.value.isEditingConnections) {
                    _uiState.update { it.copy(pendingConnectionId = null) }
                } else {
                    router.openScanner()
                }
            }
            GraphAction.ScanMoreClicked -> router.openScanner()
            GraphAction.FindRecipesClicked -> router.openRecipes()
            GraphAction.SavedClicked -> router.openSavedRecipes()
            GraphAction.BackClicked -> router.goBack()
            GraphAction.SheetDismissed -> _uiState.update { it.copy(selectedId = null) }
            GraphAction.ProfileClicked -> _uiState.update { it.copy(showProfileHint = true) }
            GraphAction.MessageShown -> _uiState.update {
                it.copy(showProfileHint = false)
            }
        }
    }

    private fun selectConnectionNode(id: ProductId) {
        val state = _uiState.value
        if (!state.isEditingConnections) return
        val first = state.pendingConnectionId
        when {
            first == null -> _uiState.update { it.copy(pendingConnectionId = id) }
            first == id -> _uiState.update { it.copy(pendingConnectionId = null) }
            else -> {
                val connection = ProductConnection.between(first, id)
                if (connection in state.connections) {
                    launch("disconnectProducts") { session.disconnect(first, id) }
                } else {
                    launch("connectProducts") { session.connect(first, id) }
                }
                _uiState.update { it.copy(pendingConnectionId = null) }
            }
        }
    }
}
