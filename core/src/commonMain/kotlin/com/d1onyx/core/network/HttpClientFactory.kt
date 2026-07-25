package com.d1onyx.core.network

import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.network.auth.AuthTokenProvider
import com.d1onyx.core.network.engine.platformHttpClientEngine
import com.d1onyx.core.network.error.AlreadyMappedException
import com.d1onyx.core.network.error.BackendExceptionMapper
import com.d1onyx.core.network.error.mapHttpFailure
import com.d1onyx.core.network.error.mapTransportFailure
import com.d1onyx.core.network.logging.KtorLoggerBridge
import com.d1onyx.core.network.serialization.createDefaultJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.LogLevel as KtorLogLevel

/**
 * Build the app's [HttpClient].
 *
 * Everything a caller needs is wired here: JSON, timeouts, bearer auth,
 * request logging routed into the app [Logger], and — most importantly —
 * failure translation, so callers see [com.d1onyx.core.essentials.exceptions]
 * types and never a raw Ktor exception.
 *
 * ```
 * val client = createHttpClient(
 *     config = NetworkConfig(baseUrl = "https://api.example.com/", isDebug = true),
 *     logger = Logger,
 *     tokenProvider = { sessionStorage.token() },
 * )
 * ```
 *
 * @param exceptionMappers feature-level refinements of [BackendExceptionMapper]
 * @param engine defaults to the platform engine; tests pass Ktor's `MockEngine`
 */
public fun createHttpClient(
    config: NetworkConfig,
    logger: Logger,
    tokenProvider: AuthTokenProvider = AuthTokenProvider.None,
    exceptionMappers: Set<BackendExceptionMapper> = emptySet(),
    json: Json = createDefaultJson(config.isDebug),
    engine: HttpClientEngine = platformHttpClientEngine(),
): HttpClient = HttpClient(engine) {
    // Ktor's own success check is disabled: `validateResponse` below produces
    // richer, app-specific exceptions from the error body.
    expectSuccess = false

    install(ContentNegotiation) {
        json(json)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = config.timeout.inWholeMilliseconds
        connectTimeoutMillis = config.timeout.inWholeMilliseconds
        socketTimeoutMillis = config.timeout.inWholeMilliseconds
    }

    install(Logging) {
        this.logger = KtorLoggerBridge(logger)
        // Bodies may contain tokens and personal data — only log them in debug.
        level = if (config.isDebug) KtorLogLevel.BODY else KtorLogLevel.INFO
    }

    install(bearerAuthPlugin(tokenProvider))

    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
    }

    HttpResponseValidator {
        validateResponse { response ->
            if (response.status.isSuccess()) return@validateResponse
            val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
            throw AlreadyMappedException(
                mapHttpFailure(
                    statusCode = response.status.value,
                    rawBody = rawBody,
                    json = json,
                    mappers = exceptionMappers,
                ),
            )
        }
        handleResponseExceptionWithRequest { cause, _ ->
            throw mapTransportFailure(cause)
        }
    }
}

/**
 * Attaches `Authorization: Bearer <token>` when a token is available.
 *
 * A plugin rather than `defaultRequest`, because reading the token suspends —
 * `defaultRequest` cannot await it.
 */
private fun bearerAuthPlugin(tokenProvider: AuthTokenProvider) =
    createClientPlugin("CoreBearerAuth") {
        onRequest { request, _ ->
            tokenProvider.provideToken()?.let { token ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
