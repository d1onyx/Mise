package com.d1onix.dishlab.feature.home.presentation

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.ObserveSavedRecipeIdsUseCase
import com.d1onix.dishlab.domain.ObserveScanHistoryUseCase
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onix.dishlab.feature.home.navigation.HomeRouter
import com.d1onix.dishlab.feature.home.navigation.ProtectedDestination
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
    profileSettings: ProfileSettingsRepository,
    userSession: UserSessionRepository,
) : AbstractViewModel(dependencies), WithMviState<HomeUiState> {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Long-lived collection, so it runs on the view-model scope directly
        // rather than through `launch`, which drives the progress indicator.
        viewModelScope.launch {
            combine(
                observeSaved(),
                observeHistory(),
                profileSettings.settings,
                userSession.session,
            ) { saved, history, profile, session ->
                HomeUiState(
                    savedCount = saved.size,
                    historyCount = history.size,
                    profileInitials = if (session.isAuthenticated) profile.initials else "?",
                    isAuthenticated = session.isAuthenticated,
                )
            }.collect { homeState ->
                _uiState.update {
                    homeState
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.ScanClicked -> router.openScanner()
            HomeAction.SavedClicked -> authenticated(ProtectedDestination.Saved) { router.openSavedRecipes() }
            HomeAction.HistoryClicked -> authenticated(ProtectedDestination.History) { router.openHistory() }
            HomeAction.ProfileClicked -> authenticated(ProtectedDestination.Profile) { router.openProfile() }
            HomeAction.CompareClicked -> authenticated(ProtectedDestination.Comparison) { router.openComparison() }
            HomeAction.DiscoverRecipesClicked -> authenticated(ProtectedDestination.RecipeDiscovery) {
                router.openRecipeDiscovery()
            }
        }
    }

    private fun authenticated(destination: ProtectedDestination, action: () -> Unit) {
        if (_uiState.value.isAuthenticated) action() else router.openAuth(destination)
    }
}
