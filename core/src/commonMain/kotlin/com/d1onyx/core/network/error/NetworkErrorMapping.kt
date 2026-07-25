package com.d1onyx.core.network.error

import com.d1onyx.core.essentials.entities.HttpCode
import com.d1onyx.core.essentials.entities.ServerCode
import com.d1onyx.core.essentials.exceptions.AbstractAppException
import com.d1onyx.core.essentials.exceptions.AuthException
import com.d1onyx.core.essentials.exceptions.BackendException
import com.d1onyx.core.essentials.exceptions.ConnectionException
import com.d1onyx.core.essentials.exceptions.InvalidBackendResponseException
import com.d1onyx.core.essentials.exceptions.RateLimitException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

internal const val HTTP_UNAUTHORIZED = 401
internal const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Build the app exception for a non-2xx response.
 *
 * 401 and 429 get dedicated types because callers act on them — refresh the
 * session, back off — while everything else becomes a [BackendException]
 * carrying the backend's own code for feature-level mappers to refine.
 */
internal fun mapHttpFailure(
    statusCode: Int,
    rawBody: String,
    json: Json,
    mappers: Iterable<BackendExceptionMapper>,
): Throwable {
    val errorDto = parseErrorDto(rawBody, json)
    return when (statusCode) {
        HTTP_UNAUTHORIZED -> AuthException()
        HTTP_TOO_MANY_REQUESTS -> RateLimitException()
        else -> mappers.mapException(
            BackendException(
                httpCode = HttpCode(statusCode),
                serverCode = ServerCode(errorDto?.errcode.orEmpty()),
                backendMessage = errorDto?.error ?: rawBody.take(MAX_RAW_BODY_IN_MESSAGE),
            ),
        )
    }
}

/**
 * Marks a failure that response validation already translated, so that Ktor's
 * exception handler — which also sees exceptions thrown out of validation —
 * re-throws it unchanged.
 *
 * Without this marker a mapper returning a feature's own exception type would
 * have it flattened into a [ConnectionException] on the way out, because that
 * type is not an [AbstractAppException].
 */
internal class AlreadyMappedException(val actual: Throwable) : Exception(actual)

/**
 * Translate a transport-level failure into an app exception.
 */
internal fun mapTransportFailure(cause: Throwable): Throwable = when (cause) {
    is AlreadyMappedException -> cause.actual
    is CancellationException -> cause
    is AbstractAppException -> cause
    is SerializationException -> InvalidBackendResponseException(cause)
    else -> ConnectionException(cause)
}

private const val MAX_RAW_BODY_IN_MESSAGE = 512

/**
 * A malformed error body must not mask the real status code, so parsing
 * failures degrade to `null` rather than throwing.
 */
private fun parseErrorDto(rawBody: String, json: Json): ErrorDto? =
    if (rawBody.isBlank()) {
        null
    } else {
        runCatching { json.decodeFromString<ErrorDto>(rawBody) }.getOrNull()
    }
