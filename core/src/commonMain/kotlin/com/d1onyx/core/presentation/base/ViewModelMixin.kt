package com.d1onyx.core.presentation.base

import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Base interface for all mixins.
 *
 * A mixin is an interface that carries its own state. Implementing one adds a
 * capability to a view-model without inheritance:
 *
 * ```
 * class LoginViewModel : AbstractViewModel(),
 *     WithCommonDependencies,
 *     WithMviState<LoginUiState>
 * ```
 */
public interface ViewModelMixin {

    /**
     * Access to the view-model's scope from any mixin.
     */
    public val coroutineScope: CoroutineScope

    /**
     * Get — or lazily create — the state belonging to a mixin. This method is
     * what actually turns a stateless interface into a stateful mixin.
     */
    public fun <T : Any> getMixinState(
        stateClass: KClass<T>,
        initializer: () -> T,
    ): T
}

/**
 * Get a mixin state by its reified type.
 *
 * Unlike the Hilt-based original, [initializer] is required: the default there
 * constructed the state reflectively (`T::class.java.getDeclaredConstructor()`),
 * and neither `Class` nor runtime construction exists on Kotlin/Native.
 */
public inline fun <reified T : Any> ViewModelMixin.getMixinState(
    noinline initializer: () -> T,
): T = getMixinState(T::class, initializer)
