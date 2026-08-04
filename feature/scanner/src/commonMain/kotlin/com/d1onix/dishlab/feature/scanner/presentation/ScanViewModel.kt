package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.GetProductByBarcodeUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.repository.ProductComparisonStore
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.scanner.navigation.ScanTarget
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@AssistedInject
class ScanViewModel(
    dependencies: CommonDependencies,
    @Assisted private val target: ScanTarget,
    private val getProductByBarcode: GetProductByBarcodeUseCase,
    private val recordScan: RecordScanUseCase,
    private val session: ScanSessionStore,
    private val comparison: ProductComparisonStore,
    private val router: ScannerRouter,
) : AbstractViewModel(dependencies), WithMviState<ScanUiState> {

    private val _uiState = MutableStateFlow(ScanUiState(target = target))
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onAction(action: ScanAction) {
        when (action) {
            is ScanAction.BarcodeDetected -> onBarcodeDetected(action.barcode)
            is ScanAction.CameraCapabilitiesChanged -> _uiState.update {
                it.copy(camera = it.camera.copy(capabilities = action.capabilities))
            }

            ScanAction.TorchToggled -> _uiState.update {
                if (!it.camera.canToggleTorch) it
                else it.copy(camera = it.camera.copy(torchOn = !it.camera.torchOn))
            }

            ScanAction.CameraFacingToggled -> _uiState.update {
                if (!it.camera.canSwitchFacing) return@update it
                val facing = when (it.camera.facing) {
                    CameraFacing.Back -> CameraFacing.Front
                    CameraFacing.Front -> CameraFacing.Back
                }
                // The front camera has no torch, so leaving the flag on would show
                // a lit button over a lens that cannot light anything.
                it.copy(camera = it.camera.copy(facing = facing, torchOn = false))
            }

            ScanAction.ManualEntryToggled ->
                _uiState.update { it.copy(manualEntryVisible = !it.manualEntryVisible) }

            is ScanAction.ManualBarcodeChanged -> _uiState.update {
                it.copy(
                    manualBarcode = action.value.filter { char -> !char.isWhitespace() },
                    resolutionFailed = false,
                    failedBarcode = null,
                )
            }

            ScanAction.ManualBarcodeSubmitted -> submitManualBarcode()
            ScanAction.RetryResolutionClicked -> retryResolution()
            ScanAction.AddReviewedProductClicked -> addReviewedProduct()
            ScanAction.ReviewedProductSkipped -> skipReviewedProduct()
            ScanAction.ReviewBackClicked -> _uiState.update {
                it.copy(
                    reviewedProduct = null,
                    reviewedProductAlreadyAdded = false,
                    detectedBarcode = null,
                )
            }
        }
    }

    /**
     * The camera reports the same code several times a second, so the first one
     * wins and everything else is dropped until this screen is left behind.
     */
    private fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.isResolving || _uiState.value.reviewedProduct != null) return
        startResolving(barcode)
        resolve(barcode)
    }

    private fun submitManualBarcode() {
        val barcode = _uiState.value.manualBarcode
        if (barcode.isBlank() || _uiState.value.isResolving) return
        startResolving(barcode)
        resolve(barcode)
    }

    private fun retryResolution() {
        val barcode = _uiState.value.failedBarcode ?: return
        if (_uiState.value.isResolving) return
        startResolving(barcode)
        resolve(barcode)
    }

    /** Publishes the digits before the lookup starts, so the viewfinder can show them. */
    private fun startResolving(barcode: String) {
        _uiState.update {
            it.copy(
                isResolving = true,
                resolutionFailed = false,
                failedBarcode = null,
                detectedBarcode = barcode,
            )
        }
    }

    private fun resolve(barcode: String) = launch("resolveBarcode") {
        try {
            val product = getProductByBarcode(barcode)
            if (product == null) {
                _uiState.update { it.copy(isResolving = false, detectedBarcode = null) }
                router.openNotFound(barcode, target)
            } else {
                present(product)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            logger.log(
                level = com.d1onyx.core.essentials.logger.LogLevel.Warn,
                tag = logTag,
                throwable = exception,
            ) { "Barcode resolution failed" }
            _uiState.update {
                it.copy(
                    isResolving = false,
                    resolutionFailed = true,
                    failedBarcode = barcode,
                    detectedBarcode = null,
                )
            }
        }
    }

    private suspend fun present(product: Product) {
        recordScan(product.id)
        _uiState.update {
            it.copy(
                isResolving = false,
                resolutionFailed = false,
                failedBarcode = null,
                manualEntryVisible = false,
                manualBarcode = "",
                detectedBarcode = null,
                reviewedProduct = product,
                reviewedProductAlreadyAdded = product.id in if (target == ScanTarget.Comparison) {
                    comparison.products.value
                } else {
                    session.products.value
                },
            )
        }
    }

    private fun addReviewedProduct() {
        val state = _uiState.value
        val product = state.reviewedProduct ?: return
        launch("addReviewedProduct") {
            if (target == ScanTarget.Comparison) {
                comparison.add(product.id)
                router.openComparison()
            } else {
                if (!state.reviewedProductAlreadyAdded) session.add(product.id)
                router.openCombinationGraph()
            }
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
            it.copy(
                reviewedProduct = null,
                reviewedProductAlreadyAdded = false,
                detectedBarcode = null,
            )
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(target: ScanTarget): ScanViewModel
    }
}
