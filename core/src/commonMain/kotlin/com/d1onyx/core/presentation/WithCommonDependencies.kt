package com.d1onyx.core.presentation

import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.presentation.base.ViewModelMixin
import dev.zacsweers.metro.Inject

/**
 * The dependencies almost every view-model needs, bundled so that adding one
 * later does not change the signature of every view-model in the app.
 *
 * Injected by constructor — Metro validates the graph at compile time, so
 * there is no runtime lookup to fail:
 *
 * ```
 * @Inject
 * class LoginViewModel(
 *     dependencies: CommonDependencies,
 *     private val login: LoginUseCase,
 * ) : AbstractViewModel(dependencies), WithMviState<LoginUiState>
 * ```
 */
@Inject
public class CommonDependencies(
    public val logger: Logger,
    public val exceptionHandler: ExceptionHandler,
)

/**
 * A mixin marking a view-model that carries the common dependencies.
 *
 * [com.d1onyx.core.presentation.base.AbstractViewModel] implements it from
 * its [CommonDependencies] parameter, which is what lets other mixins — notably
 * [WithMviState] — reach the exception handler without any of them knowing how
 * it was obtained.
 *
 * Under Koin this interface resolved its own dependencies through
 * `KoinComponent.get()`. That was a service locator: the graph was consulted at
 * runtime, and a missing binding surfaced as a crash on the screen that needed
 * it. Metro has no such escape hatch by design — the dependency is now a
 * constructor parameter, and a missing binding fails the build instead.
 */
public interface WithCommonDependencies : ViewModelMixin, Loggable {

    /**
     * Handles exceptions that reach the user, e.g. by showing a dialog.
     */
    public val exceptionHandler: ExceptionHandler

    /**
     * The logger backing [Loggable]'s logging helpers.
     */
    override val logger: Logger
}
