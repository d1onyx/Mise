package com.d1onix.dishlab.feature.products.presentation.graph

import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.SuggestNextProductUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("oats"), viewModel.uiState.value.products.map { it.id.value })
        assertEquals(1, viewModel.uiState.value.products.size)
    }

    @Test
    fun `tapping empty space adds the next catalogue product and selects it`() = runTest(dispatcher) {
        val session = FakeSessionStore(listOf(ProductId("oats")))
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.EmptySpaceClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("oats", "banana"), session.products.value.map { it.value })
        assertEquals(ProductId("banana"), viewModel.uiState.value.selectedId)
    }

    @Test
    fun `an exhausted catalogue reports instead of adding`() = runTest(dispatcher) {
        val session = FakeSessionStore(catalogue.map { it.id })
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(GraphAction.EmptySpaceClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(catalogue.size, session.products.value.size)
        assertTrue(viewModel.uiState.value.showCatalogueExhausted)
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

    private fun viewModel(session: ScanSessionStore) = GraphViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        session = session,
        getProducts = GetProductsUseCase { ids -> catalogue.filter { it.id in ids } },
        suggestNextProduct = SuggestNextProductUseCase { current ->
            catalogue.firstOrNull { it.id !in current }
        },
        recordScan = RecordScanUseCase { },
        router = FakeRouter(),
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
        override val products: StateFlow<List<ProductId>> = state
        override fun add(id: ProductId) {
            state.value = state.value + id
        }

        override fun remove(id: ProductId) {
            state.value = state.value - id
        }

        override fun reset(ids: List<ProductId>) {
            state.value = ids
        }
    }

    private class FakeRouter : ProductsRouter {
        override fun openScanner() = Unit
        override fun openRecipes() = Unit
        override fun openSavedRecipes() = Unit
        override fun openCombinationGraph() = Unit
        override fun goBack() = Unit
    }
}
