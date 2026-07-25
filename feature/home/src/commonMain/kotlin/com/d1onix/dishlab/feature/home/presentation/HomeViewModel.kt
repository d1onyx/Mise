package com.d1onix.dishlab.feature.home.presentation

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.ObserveSavedRecipeIdsUseCase
import com.d1onix.dishlab.domain.ObserveScanHistoryUseCase
import com.d1onix.dishlab.feature.home.navigation.HomeRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class HomeViewModel(
    dependencies: CommonDependencies,
    private val router: HomeRouter,
    observeSaved: ObserveSavedRecipeIdsUseCase,
    observeHistory: ObserveScanHistoryUseCase,
) : AbstractViewModel(dependencies), WithMviState<HomeUiState> {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Long-lived collection, so it runs on the view-model scope directly
        // rather than through `launch`, which drives the progress indicator.
        viewModelScope.launch {
            combine(observeSaved(), observeHistory()) { saved, history ->
                saved.size to history.size
            }.collect { (savedCount, historyCount) ->
                _uiState.update { it.copy(savedCount = savedCount, historyCount = historyCount) }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.ScanClicked -> router.openScanner()
            HomeAction.SavedClicked -> router.openSavedRecipes()
            HomeAction.HistoryClicked -> router.openHistory()
            HomeAction.ProfileClicked -> _uiState.update { it.copy(showProfileHint = true) }
            HomeAction.MessageShown -> _uiState.update { it.copy(showProfileHint = false) }
        }
    }
}
