package com.d1onyx.core.network.error

import com.d1onyx.core.essentials.entities.HttpCode
import com.d1onyx.core.essentials.entities.ServerCode
import com.d1onyx.core.essentials.exceptions.BackendException

/**
 * Turns a generic [BackendException] into a feature-specific one.
 *
 * This replaces the `@MapHttpCodeToException` / `@MapServerCodeToException`
 * annotations of the Retrofit original. Those were read reflectively by a
 * custom `CallAdapter`; Ktor has no call adapters, and Kotlin/Native has no
 * annotation reflection, so the mapping is now an explicit, testable strategy.
 *
 * A feature contributes its own mapper:
 *
 * ```
 * @ContributesIntoSet(AppScope::class)
 * @Inject
 * class AuthErrorMapper : BackendExceptionMapper {
 *     override fun map(exception: BackendException): Throwable =
 *         if (exception.serverCode.value == "M_USER_DEACTIVATED") {
 *             UserDeactivatedException(exception)
 *         } else {
 *             exception
 *         }
 * }
 * ```
 *
 * Return the original [exception] to decline the mapping.
 */
public fun interface BackendExceptionMapper {

    public fun map(exception: BackendException): Throwable

    public companion object {

        /**
         * Maps by HTTP status code.
         */
        public fun forHttpCode(
            httpCode: Int,
            transform: (BackendException) -> Throwable,
        ): BackendExceptionMapper = BackendExceptionMapper { exception ->
            if (exception.httpCode == HttpCode(httpCode)) transform(exception) else exception
        }

        /**
         * Maps by the backend's own error code.
         */
        public fun forServerCode(
            serverCode: String,
            transform: (BackendException) -> Throwable,
        ): BackendExceptionMapper = BackendExceptionMapper { exception ->
            if (exception.serverCode == ServerCode(serverCode)) transform(exception) else exception
        }
    }
}

/**
 * Apply mappers in order, stopping at the first one that returns something else.
 */
internal fun Iterable<BackendExceptionMapper>.mapException(
    exception: BackendException,
): Throwable {
    forEach { mapper ->
        val mapped = mapper.map(exception)
        if (mapped !== exception) return mapped
    }
    return exception
}
