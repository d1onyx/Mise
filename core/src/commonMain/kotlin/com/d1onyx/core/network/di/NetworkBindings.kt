package com.d1onyx.core.network.di

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.network.NetworkConfig
import com.d1onyx.core.network.auth.AuthTokenProvider
import com.d1onyx.core.network.createHttpClient
import com.d1onyx.core.network.error.BackendExceptionMapper
import com.d1onyx.core.network.serialization.createDefaultJson
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Wires the HTTP stack into the app graph.
 *
 * The host application still has to provide [NetworkConfig] and, if requests
 * are authenticated, an [AuthTokenProvider] — both are app-specific and
 * deliberately not defaulted here.
 */
@ContributesTo(AppScope::class)
@BindingContainer
public interface NetworkBindings {

    /**
     * Declares the mapper set so the graph resolves even when no feature
     * contributes one.
     */
    @Multibinds(allowEmpty = true)
    public val backendExceptionMappers: Set<BackendExceptionMapper>

    public companion object {

        @Provides
        @SingleIn(AppScope::class)
        public fun provideJson(config: NetworkConfig): Json =
            createDefaultJson(config.isDebug)

        /**
         * A single client for the whole app: it owns a connection pool, and
         * creating one per call would leak sockets.
         */
        @Provides
        @SingleIn(AppScope::class)
        public fun provideHttpClient(
            config: NetworkConfig,
            logger: Logger,
            tokenProvider: AuthTokenProvider,
            exceptionMappers: Set<BackendExceptionMapper>,
            json: Json,
        ): HttpClient = createHttpClient(
            config = config,
            logger = logger,
            tokenProvider = tokenProvider,
            exceptionMappers = exceptionMappers,
            json = json,
        )
    }
}
