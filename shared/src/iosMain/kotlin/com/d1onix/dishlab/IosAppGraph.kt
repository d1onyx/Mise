package com.d1onix.dishlab

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.d1onyx.core.datastore.createPreferencesDataStore
import com.d1onyx.core.datastore.preferencesDataStorePath
import com.d1onix.dishlab.data.session.GraphDatabase
import com.d1onix.dishlab.data.session.buildGraphDatabase
import com.d1onix.dishlab.data.session.graphDatabaseBuilder
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.network.NetworkConfig
import com.d1onyx.core.network.auth.AuthTokenProvider
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** The iOS object graph — same contract, platform-specific storage path. */
@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides backendConfig: BackendRuntimeConfig): IosAppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDataStore(): DataStore<Preferences> =
        createPreferencesDataStore { preferencesDataStorePath() }

    @Provides
    @SingleIn(AppScope::class)
    fun provideGraphDatabase(): GraphDatabase =
        buildGraphDatabase(graphDatabaseBuilder())

    @Provides
    fun provideLogger(): Logger = Logger

    @Provides
    @SingleIn(AppScope::class)
    fun provideNetworkConfig(config: BackendRuntimeConfig): NetworkConfig = NetworkConfig(
        baseUrl = config.baseUrl,
        isDebug = config.isDebug,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideAuthTokenProvider(config: BackendRuntimeConfig): AuthTokenProvider =
        AuthTokenProvider { config.developmentToken }

    @Provides
    fun provideExceptionHandler(logger: Logger): ExceptionHandler =
        ExceptionHandler { throwable ->
            logger.log(LogLevel.Error, "DishLab", throwable) { "Unhandled failure" }
        }
}
