package com.d1onyx.core.essentials.resources

import com.d1onyx.core.essentials.entities.HttpCode
import com.d1onyx.core.essentials.entities.ServerCode
import kotlin.reflect.KClass

/**
 * Base marker interface for all string providers.
 *
 * Each module defines its own provider by extending this interface and
 * registering it in the DI graph.
 */
public interface StringProvider

/**
 * Gives every module access to the string providers of all other modules.
 *
 * Keyed by [KClass] rather than by `Class` — the latter does not exist on
 * Kotlin/Native. Built through DI on app start:
 *
 * ```
 * single {
 *     StringProviderStore(
 *         mapOf(CoreStringProvider::class to AndroidCoreStringProvider(get()))
 *     )
 * }
 * ```
 */
public class StringProviderStore(
    @PublishedApi
    internal val stringProviders: Map<KClass<out StringProvider>, StringProvider>,
) {

    /**
     * Get a string provider by its type.
     *
     * @throws IllegalStateException if no provider of type [T] was registered —
     * failing loudly here beats returning a wrong provider through an unchecked cast.
     */
    public inline operator fun <reified T : StringProvider> invoke(): T {
        val provider = stringProviders[T::class]
            ?: error("No StringProvider registered for ${T::class.simpleName}")
        return provider as T
    }
}
