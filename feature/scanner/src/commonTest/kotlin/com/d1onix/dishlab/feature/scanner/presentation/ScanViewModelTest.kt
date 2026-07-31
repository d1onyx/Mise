package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.GetProductByBarcodeUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.domain.model.UserSession
import com.d1onix.dishlab.domain.repository.ProductComparisonStore
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onix.dishlab.feature.scanner.navigation.ScanTarget
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a known barcode is reviewed before it is added to the graph`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val recorded = mutableListOf<ProductId>()
        val viewModel = viewModel(session, router, recorded)

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()

        assertTrue(session.products.value.isEmpty())
        assertEquals(listOf(ProductId("barcode:111")), recorded)
        assertEquals(ProductId("barcode:111"), viewModel.uiState.value.reviewedProduct?.id)
        assertEquals(0, router.graphOpened)
        assertTrue(router.notFoundBarcodes.isEmpty())

        viewModel.onAction(ScanAction.AddReviewedProductClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(ProductId("barcode:111")), session.products.value)
        assertEquals(1, router.graphOpened)
        assertNull(viewModel.uiState.value.reviewedProduct)
    }

    @Test
    fun `an unknown barcode goes to the not-found screen`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val viewModel = viewModel(session, router)

        viewModel.onAction(ScanAction.BarcodeDetected("999"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("999"), router.notFoundBarcodes)
        assertTrue(session.products.value.isEmpty())
        assertEquals(false, viewModel.uiState.value.isResolving)
    }

    @Test
    fun `repeated detections of the same frame are ignored while resolving`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val viewModel = viewModel(session, router)

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()

        assertEquals(ProductId("barcode:111"), viewModel.uiState.value.reviewedProduct?.id)
        assertEquals(0, router.graphOpened)
    }

    @Test
    fun `manual entry drops whitespace and needs a value`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSessionStore(), FakeRouter())

        viewModel.onAction(ScanAction.ManualBarcodeChanged(" 1 1 1 "))
        assertEquals("111", viewModel.uiState.value.manualBarcode)
        assertTrue(viewModel.uiState.value.canSubmitManualBarcode)

        viewModel.onAction(ScanAction.ManualBarcodeChanged(""))
        assertEquals(false, viewModel.uiState.value.canSubmitManualBarcode)
    }

    @Test
    fun `not now keeps the scanned product out of the graph`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val recorded = mutableListOf<ProductId>()
        val viewModel = viewModel(session, router, recorded)

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()
        viewModel.onAction(ScanAction.ReviewedProductSkipped)

        assertTrue(session.products.value.isEmpty())
        assertEquals(listOf(ProductId("barcode:111")), recorded)
        assertEquals(1, router.backCount)
    }

    @Test
    fun `guest is asked to authenticate before adding to graph`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val viewModel = viewModel(session, router, authenticated = false)

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()
        viewModel.onAction(ScanAction.AddReviewedProductClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(1, router.authOpened)
        assertTrue(session.products.value.isEmpty())
        assertEquals(ProductId("barcode:111"), viewModel.uiState.value.reviewedProduct?.id)
    }

    @Test
    fun `comparison scan adds product without changing graph`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val comparison = FakeComparisonStore()
        val viewModel = viewModel(
            session = session,
            router = router,
            target = ScanTarget.Comparison,
            comparison = comparison,
        )

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()
        viewModel.onAction(ScanAction.AddReviewedProductClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(ProductId("barcode:111")), comparison.products.value)
        assertTrue(session.products.value.isEmpty())
        assertEquals(1, router.comparisonOpened)
    }

    private fun viewModel(
        session: ScanSessionStore,
        router: ScannerRouter,
        recorded: MutableList<ProductId> = mutableListOf(),
        target: ScanTarget = ScanTarget.Graph,
        comparison: ProductComparisonStore = FakeComparisonStore(),
        authenticated: Boolean = true,
    ) = ScanViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        getProductByBarcode = GetProductByBarcodeUseCase { barcode ->
            if (barcode == "111") product else null
        },
        recordScan = RecordScanUseCase { id -> recorded += id },
        target = target,
        session = session,
        comparison = comparison,
        userSession = FakeUserSessionRepository(authenticated),
        router = router,
    )

    private val product = Product(
        id = ProductId("barcode:111"),
        barcode = "111",
        name = "Rolled Oats",
        category = "Grains",
        score = 82,
        accentColor = 0xFFC8FF4D,
        initial = "O",
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    )

    private class FakeSessionStore : ScanSessionStore {
        private val state = MutableStateFlow<List<ProductId>>(emptyList())
        override val products: StateFlow<List<ProductId>> = state
        private val connectionState = MutableStateFlow<Set<ProductConnection>>(emptySet())
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
            connectionState.value = ids.flatMapIndexed { index, first ->
                ids.drop(index + 1).map { second ->
                    ProductConnection.between(first, second)
                }
            }.toSet()
        }
    }

    private class FakeRouter : ScannerRouter {
        var graphOpened = 0
        var comparisonOpened = 0
        var authOpened = 0
        var backCount = 0
        val notFoundBarcodes = mutableListOf<String>()
        override fun openCombinationGraph() {
            graphOpened++
        }

        override fun openNotFound(barcode: String, target: ScanTarget) {
            notFoundBarcodes += barcode
        }

        override fun openScanner(target: ScanTarget) = Unit
        override fun openComparison() {
            comparisonOpened++
        }
        override fun openAuth() {
            authOpened++
        }
        override fun openHome() = Unit
        override fun goBack() {
            backCount++
        }
    }

    private class FakeComparisonStore : ProductComparisonStore {
        private val state = MutableStateFlow<List<ProductId>>(emptyList())
        override val products: StateFlow<List<ProductId>> = state
        override suspend fun add(id: ProductId): Boolean {
            state.value += id
            return true
        }
        override suspend fun remove(id: ProductId) {
            state.value -= id
        }
        override suspend fun clear() {
            state.value = emptyList()
        }
    }

    private class FakeUserSessionRepository(authenticated: Boolean) : UserSessionRepository {
        private val state = MutableStateFlow(UserSession(isAuthenticated = authenticated))
        override val session: Flow<UserSession> = state
        override suspend fun signIn(email: String, password: String) = Unit
        override suspend fun register(email: String, password: String) = Unit
        override suspend fun signOut() = Unit
        override suspend fun markOnboardingCompleted() = Unit
    }
}
