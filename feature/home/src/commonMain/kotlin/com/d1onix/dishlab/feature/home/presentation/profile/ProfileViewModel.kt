package com.d1onix.dishlab.feature.home.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.domain.repository.UserSessionRepository
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

@Immutable
data class ProfileUiState(
    val displayName: String = "Alex Kim",
    val savedDisplayName: String = "Alex Kim",
    val initials: String = "AK",
    val graphProductCount: Int = 0,
    val autoConnectNewProducts: Boolean = true,
    val reduceGraphMotion: Boolean = false,
    val showProductScores: Boolean = true,
) {
    val canSaveName: Boolean
        get() = displayName.isNotBlank() && displayName.trim() != savedDisplayName
}

sealed interface ProfileAction {
    data class DisplayNameChanged(val value: String) : ProfileAction
    data object DisplayNameSaved : ProfileAction
    data class AutoConnectChanged(val enabled: Boolean) : ProfileAction
    data class ReduceMotionChanged(val enabled: Boolean) : ProfileAction
    data class ShowScoresChanged(val enabled: Boolean) : ProfileAction
    data object BackClicked : ProfileAction
    data object RecipePreferencesClicked : ProfileAction
    data object SignOutClicked : ProfileAction
}

@Inject
class ProfileViewModel(
    dependencies: CommonDependencies,
    private val settingsRepository: ProfileSettingsRepository,
    session: ScanSessionStore,
    private val userSession: UserSessionRepository,
    private val router: HomeRouter,
) : AbstractViewModel(dependencies), WithMviState<ProfileUiState> {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settingsRepository.settings, session.products) { settings, products ->
                settings to products.size
            }.collect { (settings, productCount) ->
                _uiState.update { state ->
                    val hasUnsavedName = state.displayName.trim() != state.savedDisplayName
                    state.copy(
                        displayName = if (hasUnsavedName) state.displayName else settings.displayName,
                        savedDisplayName = settings.displayName,
                        initials = settings.initials,
                        graphProductCount = productCount,
                        autoConnectNewProducts = settings.autoConnectNewProducts,
                        reduceGraphMotion = settings.reduceGraphMotion,
                        showProductScores = settings.showProductScores,
                    )
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.DisplayNameChanged -> _uiState.update {
                it.copy(displayName = action.value.take(40))
            }
            ProfileAction.DisplayNameSaved -> launch("saveProfileName") {
                val name = _uiState.value.displayName.trim()
                if (name.isNotEmpty()) settingsRepository.setDisplayName(name)
            }
            is ProfileAction.AutoConnectChanged -> launch("saveAutoConnect") {
                settingsRepository.setAutoConnectNewProducts(action.enabled)
            }
            is ProfileAction.ReduceMotionChanged -> launch("saveReduceMotion") {
                settingsRepository.setReduceGraphMotion(action.enabled)
            }
            is ProfileAction.ShowScoresChanged -> launch("saveShowScores") {
                settingsRepository.setShowProductScores(action.enabled)
            }
            ProfileAction.BackClicked -> router.goBack()
            ProfileAction.RecipePreferencesClicked -> router.openPreferenceSetup()
            ProfileAction.SignOutClicked -> launch("signOut") {
                userSession.signOut()
                router.goBack()
            }
        }
    }
}
