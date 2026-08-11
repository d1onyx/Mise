package com.d1onix.dishlab.feature.products.presentation.graph

import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.domain.model.ProfileSettings
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.products.navigation.ProductsRouter
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.CommonDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the graph mirrors the scan session`() = runTest(dispatcher) {
        val session = FakeSessionStore(listOf(ProductId("oats")))
        val viewModel = viewModel(session)

        assertTrue(viewModel.uiState.value.isLoading)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("oats"), viewModel.uiState.value.products.map { it.id.value })
        assertEquals(ProductId("oats"), viewModel.uiState.value.selectedId)
        assertEquals(1, viewModel.uiState.value.products.size)
    }

    @Test
    fun `tapping empty space opens the scanner without changing the graph`() = runTest(dispatcher) {
        val session = FakeSessionStore(listOf(ProductId("oats")))
        val router = FakeRouter()
        val viewModel = viewModel(session, router)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.EmptySpaceClicked)

        assertEquals(listOf("oats"), session.products.value.map { it.value })
        assertEquals(1, router.scannerOpened)
    }

    @Test
    fun `removing the selected product closes its sheet`() = runTest(dispatcher) {
        val session = FakeSessionStore(listOf(ProductId("oats"), ProductId("banana")))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.NodeClicked(ProductId("banana")))
        assertEquals(ProductId("banana"), viewModel.uiState.value.selectedId)

        viewModel.onAction(GraphAction.RemoveClicked(ProductId("banana")))
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedId)
        assertEquals(listOf("oats"), viewModel.uiState.value.products.map { it.id.value })
    }

    @Test
    fun `connection mode cuts an existing edge by selecting its two nodes`() = runTest(dispatcher) {
        val oats = ProductId("oats")
        val banana = ProductId("banana")
        val session = FakeSessionStore(listOf(oats, banana))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.ConnectionEditingToggled)
        viewModel.onAction(GraphAction.ConnectionNodeClicked(oats))
        viewModel.onAction(GraphAction.ConnectionNodeClicked(banana))
        testScheduler.advanceUntilIdle()

        assertTrue(session.connections.value.isEmpty())
        assertNull(viewModel.uiState.value.pendingConnectionId)
    }

    @Test
    fun `a regular node click cannot open details in connection mode`() = runTest(dispatcher) {
        val oats = ProductId("oats")
        val session = FakeSessionStore(listOf(oats))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.ConnectionEditingToggled)
        viewModel.onAction(GraphAction.NodeClicked(oats))

        assertNull(viewModel.uiState.value.selectedId)
        assertEquals(oats, viewModel.uiState.value.pendingConnectionId)
    }

    @Test
    fun `selecting two disconnected nodes creates an edge`() = runTest(dispatcher) {
        val oats = ProductId("oats")
        val banana = ProductId("banana")
        val session = FakeSessionStore(listOf(oats, banana))
        session.disconnect(oats, banana)
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.ConnectionEditingToggled)
        viewModel.onAction(GraphAction.ConnectionNodeClicked(oats))
        viewModel.onAction(GraphAction.ConnectionNodeClicked(banana))
        testScheduler.advanceUntilIdle()

        assertEquals(setOf(ProductConnection.between(oats, banana)), session.connections.value)
    }

    @Test
    fun `clicking a line in connection mode cuts it`() = runTest(dispatcher) {
        val oats = ProductId("oats")
        val banana = ProductId("banana")
        val connection = ProductConnection.between(oats, banana)
        val session = FakeSessionStore(listOf(oats, banana))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.ConnectionEditingToggled)
        viewModel.onAction(GraphAction.ConnectionClicked(connection))
        testScheduler.advanceUntilIdle()

        assertTrue(session.connections.value.isEmpty())
    }

    @Test
    fun `moving a node persists its normalized position`() = runTest(dispatcher) {
        val oats = ProductId("oats")
        val position = ProductGraphPosition(0.25f, 0.75f)
        val session = FakeSessionStore(listOf(oats))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.NodePositionChanged(oats, position))
        testScheduler.advanceUntilIdle()

        assertEquals(position, session.positions.value[oats])
        assertEquals(position, viewModel.uiState.value.positions[oats])
    }

    private fun viewModel(
        session: ScanSessionStore,
        router: ProductsRouter = FakeRouter(),
    ) = GraphViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        session = session,
        getProducts = GetProductsUseCase { ids -> catalogue.filter { it.id in ids } },
        profileSettings = FakeProfileSettingsRepository(),
        router = router,
    )

    private val catalogue = listOf(
        product("oats", "Rolled Oats", 82),
        product("banana", "Banana", 76),
    )

    private fun product(id: String, name: String, score: Int) = Product(
        id = ProductId(id),
        barcode = id,
        name = name,
        category = "Test",
        score = score,
        accentColor = 0xFFC8FF4D,
        initial = name.first().toString(),
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    )

    private class FakeSessionStore(initial: List<ProductId>) : ScanSessionStore {
        private val state = MutableStateFlow(initial)
        override val startupProducts: StateFlow<List<ProductId>?> = MutableStateFlow(initial)
        override val products: StateFlow<List<ProductId>> = state
        private val connectionState = MutableStateFlow(completeConnections(initial))
        override val connections: StateFlow<Set<ProductConnection>> = connectionState
        override val positions = MutableStateFlow<Map<ProductId, ProductGraphPosition>>(emptyMap())
        override suspend fun add(id: ProductId) {
            val current = state.value
            state.value = state.value + id
            connectionState.value += current.map { ProductConnection.between(it, id) }
        }

        override suspend fun remove(id: ProductId) {
            state.value = state.value - id
            connectionState.value = connectionState.value.filterNotTo(mutableSetOf()) {
                it.contains(id)
            }
        }

        override suspend fun connect(first: ProductId, second: ProductId) {
            connectionState.value += ProductConnection.between(first, second)
        }

        override suspend fun disconnect(first: ProductId, second: ProductId) {
            connectionState.value -= ProductConnection.between(first, second)
        }

        override suspend fun updatePosition(id: ProductId, position: ProductGraphPosition) {
            positions.value += id to position
        }

        override suspend fun reset(ids: List<ProductId>) {
            state.value = ids
            connectionState.value = completeConnections(ids)
        }

        companion object {
            private fun completeConnections(ids: List<ProductId>): Set<ProductConnection> =
                ids.flatMapIndexed { index, first ->
                    ids.drop(index + 1).map { second ->
                        ProductConnection.between(first, second)
                    }
                }.toSet()
        }
    }

    private class FakeRouter : ProductsRouter {
        var scannerOpened = 0
        override fun openScanner() {
            scannerOpened++
        }
        override fun openRecipes() = Unit
        override fun openSavedRecipes() = Unit
        override fun openCombinationGraph() = Unit
        override fun openConnectionOverview() = Unit
        override fun openComparison() = Unit
        override fun openComparisonScanner() = Unit
        override fun openSettings() = Unit
        override fun goBack() = Unit
    }

    private class FakeProfileSettingsRepository : ProfileSettingsRepository {
        override val settings: Flow<ProfileSettings> = MutableStateFlow(ProfileSettings())
        override suspend fun setDisplayName(value: String) = Unit
        override suspend fun setAutoConnectNewProducts(enabled: Boolean) = Unit
        override suspend fun setReduceGraphMotion(enabled: Boolean) = Unit
        override suspend fun setShowProductScores(enabled: Boolean) = Unit
    }
}
