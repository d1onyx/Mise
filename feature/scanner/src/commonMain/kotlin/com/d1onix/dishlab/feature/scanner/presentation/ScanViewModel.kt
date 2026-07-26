package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.GetAllProductsUseCase
import com.d1onix.dishlab.domain.GetProductByBarcodeUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.SuggestNextProductUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Inject
class ScanViewModel(
    dependencies: CommonDependencies,
    private val getProductByBarcode: GetProductByBarcodeUseCase,
    private val suggestNextProduct: SuggestNextProductUseCase,
    private val getAllProducts: GetAllProductsUseCase,
    private val recordScan: RecordScanUseCase,
    private val session: ScanSessionStore,
    private val router: ScannerRouter,
) : AbstractViewModel(dependencies), WithMviState<ScanUiState> {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onAction(action: ScanAction) {
        when (action) {
            is ScanAction.BarcodeDetected -> onBarcodeDetected(action.barcode)
            ScanAction.CaptureClicked -> captureDemoScan()
            ScanAction.SimulateNotFoundClicked -> simulateNotFound()
            ScanAction.ManualEntryToggled ->
                _uiState.update { it.copy(manualEntryVisible = !it.manualEntryVisible) }

            is ScanAction.ManualBarcodeChanged -> _uiState.update {
                it.copy(manualBarcode = action.value.filter { char -> !char.isWhitespace() })
            }

            ScanAction.ManualBarcodeSubmitted -> submitManualBarcode()
            ScanAction.AddReviewedProductClicked -> addReviewedProduct()
            ScanAction.ReviewedProductSkipped -> skipReviewedProduct()
            ScanAction.ReviewBackClicked -> _uiState.update {
                it.copy(reviewedProduct = null, reviewedProductAlreadyAdded = false)
            }
            ScanAction.BackClicked -> router.goBack()
        }
    }

    /**
     * The camera reports the same code several times a second, so the first one
     * wins and everything else is dropped until this screen is left behind.
     */
    private fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.isResolving || _uiState.value.reviewedProduct != null) return
        _uiState.update { it.copy(isResolving = true) }
        resolve(barcode)
    }

    /**
     * Demo capture: adds the next catalogue product without a camera, so the app
     * is walkable on an emulator or a device with the permission refused.
     */
    private fun captureDemoScan() {
        if (_uiState.value.isResolving) return
        _uiState.update { it.copy(isResolving = true) }
        launch("captureDemoScan") {
            val product = suggestNextProduct(session.products.value)
                ?: getAllProducts().randomOrNull()
            if (product == null) {
                _uiState.update { it.copy(isResolving = false) }
            } else {
                present(product)
            }
        }
    }

    /** Keeps the «not found» screen reachable while every real scan succeeds. */
    private fun simulateNotFound() {
        if (_uiState.value.isResolving) return
        router.openNotFound(NOT_FOUND_DEMO_BARCODE)
    }

    private fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcode
        if (barcode.isBlank() || _uiState.value.isResolving) return
        _uiState.update { it.copy(isResolving = true) }
        resolve(barcode)
    }

    private fun resolve(barcode: String) = launch("resolveBarcode") {
        val product = getProductByBarcode(barcode)
        if (product == null) {
            _uiState.update { it.copy(isResolving = false) }
            router.openNotFound(barcode)
        } else {
            present(product)
        }
    }

    private suspend fun present(product: Product) {
        recordScan(product.id)
        _uiState.update {
            it.copy(
                isResolving = false,
                manualEntryVisible = false,
                manualBarcode = "",
                reviewedProduct = product,
                reviewedProductAlreadyAdded = product.id in session.products.value,
            )
        }
    }

    private fun addReviewedProduct() {
        val state = _uiState.value
        val product = state.reviewedProduct ?: return
        launch("addReviewedProduct") {
            if (!state.reviewedProductAlreadyAdded) {
                session.add(product.id)
            }
            router.openCombinationGraph()
            clearReview()
        }
    }

    private fun skipReviewedProduct() {
        if (_uiState.value.reviewedProduct == null) return
        router.goBack()
        clearReview()
    }

    private fun clearReview() {
        _uiState.update {
            it.copy(reviewedProduct = null, reviewedProductAlreadyAdded = false)
        }
    }

    private companion object {
        /** Mirrors `DemoMode.NOT_FOUND_BARCODE` in the data layer. */
        const val NOT_FOUND_DEMO_BARCODE = "000000000000"
    }
}
