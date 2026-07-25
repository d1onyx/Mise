package com.d1onyx.core.presentation

import com.d1onyx.core.essentials.logger.logged
import com.d1onyx.core.presentation.base.ViewModelMixin
import com.d1onyx.core.presentation.base.getMixinState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * A mixin providing the common plumbing for an MVI screen: a progress flag and
 * a way to run async actions that are traced, and whose failures reach the
 * app's [com.d1onyx.core.essentials.exceptions.ExceptionHandler].
 *
 * @param State the screen's UI state type, used by implementors to constrain
 * their own reducer plumbing; this mixin only manages progress and execution.
 *
 * ```
 * class LoginViewModel : AbstractViewModel(), WithMviState<LoginUiState> {
 *
 *     fun onLoginClicked(credentials: Credentials) = launch("login") {
 *         val session = loginUseCase(credentials)
 *         // ...
 *     }
 * }
 * ```
 *
 * The handler comes from [WithCommonDependencies], which `AbstractViewModel`
 * satisfies from its constructor — this mixin never touches the DI graph itself.
 *
 * The reducer helpers of the Hilt original are gone: they were built directly
 * on `com.elveum:container`, which is JVM-only. Compose a `StateFlow` from
 * [progressStateFlow] and the feature's own flows instead.
 */
public interface WithMviState<State> : ViewModelMixin, WithCommonDependencies {

    public enum class HideProgressPolicy {

        /** Hide the indicator once the action finishes, successfully or not. */
        OnFinally,

        /**
         * Hide the indicator only on failure. Use it when success navigates
         * away, so the indicator stays up until the screen is gone.
         */
        OnError,
    }

    /**
     * The current progress indicator status, toggled by [launch].
     */
    public val progressStateFlow: StateFlow<Boolean>
        get() = getMixinState(::MviMixinState).progressStateFlow

    /**
     * Execute an async action, tracking its progress, tracing it, and routing
     * failures to the exception handler.
     *
     * Cancellation propagates untouched — a cancelled view-model scope is not
     * an error and must not reach the user as one.
     *
     * @param operation name used in the trace
     */
    public fun launch(
        operation: String,
        hideProgressPolicy: HideProgressPolicy = HideProgressPolicy.OnFinally,
        action: suspend () -> Unit,
    ) {
        coroutineScope.launch {
            try {
                updateProgress(true)
                // `logged` already reports the failure with its stack trace,
                // so the catch below only has to surface it to the user.
                logged(operation) { action() }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                if (hideProgressPolicy == HideProgressPolicy.OnError) updateProgress(false)
                exceptionHandler.handleException(exception)
            } finally {
                if (hideProgressPolicy == HideProgressPolicy.OnFinally) updateProgress(false)
            }
        }
    }

    /**
     * Show/hide the progress indicator.
     */
    public fun updateProgress(value: Boolean) {
        getMixinState(::MviMixinState).progressStateFlow.value = value
    }
}

/**
 * State backing [WithMviState]. Internal, but top-level rather than nested:
 * [ViewModelMixin.getMixinState] keys states by `KClass`, and a class nested in
 * an interface cannot be referenced by constructor from the interface body.
 */
internal class MviMixinState(
    val progressStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false),
)
