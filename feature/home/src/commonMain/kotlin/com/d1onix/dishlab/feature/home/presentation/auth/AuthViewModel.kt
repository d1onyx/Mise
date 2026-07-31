package com.d1onix.dishlab.feature.home.presentation.auth

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onix.dishlab.feature.home.navigation.HomeRouter
import com.d1onix.dishlab.feature.home.navigation.ProtectedDestination
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

enum class AuthMode { SignIn, Register }

@Immutable
data class AuthUiState(
    val mode: AuthMode = AuthMode.SignIn,
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val submitted: Boolean = false,
    val authenticationFailed: Boolean = false,
) {
    val emailValid: Boolean get() = email.substringAfterLast('@', "").contains('.')
    val canContinue: Boolean
        get() = emailValid && password.length >= 6 &&
            (mode == AuthMode.SignIn || displayName.trim().length >= 2)
}

sealed interface AuthAction {
    data class ModeChanged(val mode: AuthMode) : AuthAction
    data class EmailChanged(val value: String) : AuthAction
    data class DisplayNameChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data object ContinueClicked : AuthAction
    data object BackClicked : AuthAction
}

@AssistedInject
class AuthViewModel(
    dependencies: CommonDependencies,
    @Assisted private val destination: ProtectedDestination,
    private val sessions: UserSessionRepository,
    private val profileSettings: ProfileSettingsRepository,
    private val router: HomeRouter,
) : AbstractViewModel(dependencies), WithMviState<AuthUiState> {
    private val mutableState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.ModeChanged -> mutableState.update {
                it.copy(mode = action.mode, submitted = false, authenticationFailed = false)
            }
            is AuthAction.EmailChanged -> mutableState.update {
                it.copy(email = action.value.trim().take(120), submitted = false, authenticationFailed = false)
            }
            is AuthAction.DisplayNameChanged -> mutableState.update {
                it.copy(displayName = action.value.take(40), submitted = false, authenticationFailed = false)
            }
            is AuthAction.PasswordChanged -> mutableState.update {
                it.copy(password = action.value.take(72), submitted = false, authenticationFailed = false)
            }
            AuthAction.ContinueClicked -> submit()
            AuthAction.BackClicked -> router.goBack()
        }
    }

    private fun submit() {
        mutableState.update { it.copy(submitted = true) }
        val state = mutableState.value
        if (!state.canContinue) return
        launch("authenticate") {
            try {
                when (state.mode) {
                    AuthMode.SignIn -> {
                        sessions.signIn(state.email, state.password)
                        router.completeProtectedNavigation(destination)
                    }
                    AuthMode.Register -> {
                        sessions.register(state.email, state.password)
                        profileSettings.setDisplayName(state.displayName)
                        router.openPostRegistrationOnboarding(destination)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                mutableState.update { it.copy(authenticationFailed = true) }
            }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(destination: ProtectedDestination): AuthViewModel
    }
}
