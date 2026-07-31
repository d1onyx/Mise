package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
import com.d1onix.dishlab.feature.scanner.navigation.ScanTarget
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class ScanNotFoundUiState(
    val barcode: String = "",
)

sealed interface ScanNotFoundAction {
    data object RetryClicked : ScanNotFoundAction
    data object HomeClicked : ScanNotFoundAction
}

@AssistedInject
class ScanNotFoundViewModel(
    dependencies: CommonDependencies,
    @Assisted barcode: String,
    @Assisted private val target: ScanTarget,
    private val router: ScannerRouter,
) : AbstractViewModel(dependencies), WithMviState<ScanNotFoundUiState> {

    private val _uiState = MutableStateFlow(ScanNotFoundUiState(barcode = barcode))
    val uiState: StateFlow<ScanNotFoundUiState> = _uiState.asStateFlow()

    fun onAction(action: ScanNotFoundAction) {
        when (action) {
            ScanNotFoundAction.RetryClicked -> router.openScanner(target)
            ScanNotFoundAction.HomeClicked -> router.openHome()
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(barcode: String, target: ScanTarget): ScanNotFoundViewModel
    }
}
