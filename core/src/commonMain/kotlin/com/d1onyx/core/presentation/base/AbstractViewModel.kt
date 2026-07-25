package com.d1onyx.core.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logD
import com.d1onyx.core.essentials.logger.logged
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithCommonDependencies
import com.d1onyx.core.presentation.WithInitCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

/**
 * An abstract view-model that adds mixin support and logs its own lifecycle.
 *
 * Usage:
 *
 * ```
 * @Inject
 * class LoginViewModel(
 *     dependencies: CommonDependencies,
 *     private val login: LoginUseCase,
 * ) : AbstractViewModel(dependencies),
 *     WithInitCallback,
 *     WithMviState<LoginUiState> {
 *
 *     override suspend fun onInitialized() { /* ... */ }
 * }
 * ```
 *
 * Every instance logs `created` and `cleared` under its own class name, so a
 * feature's view-model lifecycle shows up in the log without any per-screen
 * boilerplate.
 *
 * @see ViewModelMixin
 */
public abstract class AbstractViewModel(
    dependencies: CommonDependencies,
) : ViewModel(), ViewModelMixin, WithCommonDependencies {

    private val mixinStateMap = mutableMapOf<KClass<*>, Any>()

    override val logger: Logger = dependencies.logger

    override val exceptionHandler: ExceptionHandler = dependencies.exceptionHandler

    override val coroutineScope: CoroutineScope get() = viewModelScope

    override val logTag: String get() = this::class.simpleName ?: "ViewModel"

    init {
        logD { "created" }
        viewModelScope.launch {
            // `viewModelScope` runs on Dispatchers.Main.immediate, which executes
            // a coroutine body inline when already on the main thread — that would
            // reach the subclass before its constructor finished. Yielding first
            // guarantees the callback runs only after construction completes.
            yield()
            (this@AbstractViewModel as? WithInitCallback)?.let { callback ->
                try {
                    logged("onInitialized") { callback.onInitialized() }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Throwable) {
                    // `logged` already reported it with its stack trace. Swallowing
                    // here is deliberate: an uncaught throw from this coroutine would
                    // reach the default handler and crash the app because one screen
                    // failed to initialise.
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getMixinState(stateClass: KClass<T>, initializer: () -> T): T =
        mixinStateMap.getOrPut(stateClass) { initializer() } as T

    override fun onCleared() {
        super.onCleared()
        mixinStateMap.values
            .filterIsInstance<AutoCloseable>()
            .forEach(AutoCloseable::close)
        mixinStateMap.clear()
        logD { "cleared" }
    }
}
