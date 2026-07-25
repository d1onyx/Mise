package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.GetAllProductsUseCase
import com.d1onix.dishlab.domain.GetProductByBarcodeUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.SuggestNextProductUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a known barcode lands on the graph and in the history`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val recorded = mutableListOf<ProductId>()
        val viewModel = viewModel(session, router, recorded)

        viewModel.onAction(ScanAction.BarcodeDetected("111"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(ProductId("oats")), session.products.value)
        assertEquals(listOf(ProductId("oats")), recorded)
        assertEquals(1, router.graphOpened)
        assertTrue(router.notFoundBarcodes.isEmpty())
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

        assertEquals(1, router.graphOpened)
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
    fun `capture adds the next catalogue product without a camera`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val recorded = mutableListOf<ProductId>()
        val viewModel = viewModel(session, router, recorded)

        viewModel.onAction(ScanAction.CaptureClicked)
        testScheduler.advanceUntilIdle()
        viewModel.onAction(ScanAction.CaptureClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(ProductId("oats"), ProductId("banana")), session.products.value)
        assertEquals(2, recorded.size)
        assertEquals(2, router.graphOpened)
    }

    @Test
    fun `the not-found screen stays reachable on demand`() = runTest(dispatcher) {
        val session = FakeSessionStore()
        val router = FakeRouter()
        val viewModel = viewModel(session, router)

        viewModel.onAction(ScanAction.SimulateNotFoundClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(1, router.notFoundBarcodes.size)
        assertTrue(session.products.value.isEmpty())
    }

    private fun viewModel(
        session: ScanSessionStore,
        router: ScannerRouter,
        recorded: MutableList<ProductId> = mutableListOf(),
    ) = ScanViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        getProductByBarcode = GetProductByBarcodeUseCase { barcode ->
            if (barcode == "111") oats else null
        },
        suggestNextProduct = SuggestNextProductUseCase { current ->
            catalogue.firstOrNull { it.id !in current }
        },
        getAllProducts = GetAllProductsUseCase { catalogue },
        recordScan = RecordScanUseCase { id -> recorded += id },
        session = session,
        router = router,
    )

    private val catalogue get() = listOf(oats, banana)

    private val oats = Product(
        id = ProductId("oats"),
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

    private val banana = Product(
        id = ProductId("banana"),
        barcode = "222",
        name = "Banana",
        category = "Fruit",
        score = 76,
        accentColor = 0xFFFFE24E,
        initial = "B",
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    )

    private class FakeSessionStore : ScanSessionStore {
        private val state = MutableStateFlow<List<ProductId>>(emptyList())
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

    private class FakeRouter : ScannerRouter {
        var graphOpened = 0
        val notFoundBarcodes = mutableListOf<String>()
        override fun openCombinationGraph() {
            graphOpened++
        }

        override fun openNotFound(barcode: String) {
            notFoundBarcodes += barcode
        }

        override fun openScanner() = Unit
        override fun openHome() = Unit
        override fun goBack() = Unit
    }
}
