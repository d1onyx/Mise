package com.d1onix.dishlab.feature.products.presentation.connections

import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductGraphPosition
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionOverviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val a = ProductId("a")
    private val b = ProductId("b")
    private val c = ProductId("c")
    private val catalogue = listOf(product(a), product(b), product(c))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `selecting a product exposes only its direct connections`() = runTest(dispatcher) {
        val session = FakeSessionStore(
            ids = listOf(a, b, c),
            initialConnections = setOf(
                ProductConnection.between(a, b),
                ProductConnection.between(b, c),
            ),
        )
        val viewModel = viewModel(session)
        testScheduler.advanceUntilIdle()

        assertEquals(a, viewModel.uiState.value.focusedProductId)
        assertEquals(setOf(b), viewModel.uiState.value.directConnectionIds)

        viewModel.onAction(ConnectionOverviewAction.ProductSelected(b))

        assertEquals(b, viewModel.uiState.value.focusedProductId)
        assertEquals(setOf(a, c), viewModel.uiState.value.directConnectionIds)
    }

    private fun viewModel(session: ScanSessionStore) = ConnectionOverviewViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        session = session,
        getProducts = GetProductsUseCase { ids -> catalogue.filter { it.id in ids } },
        router = FakeRouter(),
    )

    private fun product(id: ProductId) = Product(
        id = id,
        barcode = id.value,
        name = id.value.uppercase(),
        category = "Test",
        score = 70,
        accentColor = 0xFFC8FF4D,
        initial = id.value.uppercase(),
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    )

    private class FakeSessionStore(
        ids: List<ProductId>,
        initialConnections: Set<ProductConnection>,
    ) : ScanSessionStore {
        override val startupProducts: StateFlow<List<ProductId>?> = MutableStateFlow(ids)
        override val products: StateFlow<List<ProductId>> = MutableStateFlow(ids)
        override val connections: StateFlow<Set<ProductConnection>> =
            MutableStateFlow(initialConnections)
        override val positions: StateFlow<Map<ProductId, ProductGraphPosition>> =
            MutableStateFlow(emptyMap())

        override suspend fun add(id: ProductId) = Unit
        override suspend fun remove(id: ProductId) = Unit
        override suspend fun connect(first: ProductId, second: ProductId) = Unit
        override suspend fun disconnect(first: ProductId, second: ProductId) = Unit
        override suspend fun updatePosition(id: ProductId, position: ProductGraphPosition) = Unit
        override suspend fun reset(ids: List<ProductId>) = Unit
    }

    private class FakeRouter : ProductsRouter {
        override fun openScanner() = Unit
        override fun openRecipes() = Unit
        override fun openSavedRecipes() = Unit
        override fun openCombinationGraph() = Unit
        override fun openConnectionOverview() = Unit
        override fun openProfile() = Unit
        override fun openComparison() = Unit
        override fun openComparisonScanner() = Unit
        override fun goBack() = Unit
    }
}
