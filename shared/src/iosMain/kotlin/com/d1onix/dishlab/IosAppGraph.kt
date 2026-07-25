package com.d1onix.dishlab

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.d1onyx.core.datastore.createPreferencesDataStore
import com.d1onyx.core.datastore.preferencesDataStorePath
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.Logger
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** The iOS object graph — same contract, platform-specific storage path. */
@DependencyGraph(AppScope::class)
interface IosAppGraph : AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): IosAppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDataStore(): DataStore<Preferences> =
        createPreferencesDataStore { preferencesDataStorePath() }

    @Provides
    fun provideLogger(): Logger = Logger

    @Provides
    fun provideExceptionHandler(logger: Logger): ExceptionHandler =
        ExceptionHandler { throwable ->
            logger.log(LogLevel.Error, "DishLab", throwable) { "Unhandled failure" }
        }
}
