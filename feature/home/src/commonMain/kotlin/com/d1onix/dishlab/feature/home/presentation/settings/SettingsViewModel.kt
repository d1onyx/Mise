package com.d1onix.dishlab.feature.home.presentation.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.feature.home.navigation.SettingsRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map

@Immutable
data class SettingsUiState(
    val autoConnectNewProducts: Boolean = true,
    val reduceGraphMotion: Boolean = false,
    val showProductScores: Boolean = true,
)

sealed interface SettingsAction {
    data class AutoConnectChanged(val enabled: Boolean) : SettingsAction
    data class ReduceMotionChanged(val enabled: Boolean) : SettingsAction
    data class ShowScoresChanged(val enabled: Boolean) : SettingsAction
    data object RecipePreferencesClicked : SettingsAction
    data object BackClicked : SettingsAction
}

@Inject
class SettingsViewModel(
    dependencies: CommonDependencies,
    private val settingsRepository: ProfileSettingsRepository,
    private val router: SettingsRouter,
) : AbstractViewModel(dependencies), WithMviState<SettingsUiState> {
    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .mapToUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.AutoConnectChanged -> launch("saveAutoConnect") {
                settingsRepository.setAutoConnectNewProducts(action.enabled)
            }
            is SettingsAction.ReduceMotionChanged -> launch("saveReduceMotion") {
                settingsRepository.setReduceGraphMotion(action.enabled)
            }
            is SettingsAction.ShowScoresChanged -> launch("saveShowScores") {
                settingsRepository.setShowProductScores(action.enabled)
            }
            SettingsAction.RecipePreferencesClicked -> router.openPreferenceSetup()
            SettingsAction.BackClicked -> router.goBack()
        }
    }
}

private fun kotlinx.coroutines.flow.Flow<com.d1onix.dishlab.domain.model.ProfileSettings>.mapToUiState() =
    map { settings ->
        SettingsUiState(
            autoConnectNewProducts = settings.autoConnectNewProducts,
            reduceGraphMotion = settings.reduceGraphMotion,
            showProductScores = settings.showProductScores,
        )
    }
