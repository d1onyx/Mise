package com.d1onix.dishlab

import android.content.Context
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
import dev.zacsweers.metro.createGraphFactory

/**
 * The Android object graph. Declared here, not in `commonMain`, because a common
 * graph cannot see Android-only contributions such as the DataStore path.
 */
@DependencyGraph(AppScope::class)
interface AndroidAppGraph : AppGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidAppGraph
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDataStore(context: Context): DataStore<Preferences> =
        createPreferencesDataStore { context.preferencesDataStorePath() }

    @Provides
    fun provideLogger(): Logger = Logger

    /**
     * Failures that reach the user have no UI of their own yet, so they are
     * logged. Swapping this for a dialog is a one-line change here.
     */
    @Provides
    fun provideExceptionHandler(logger: Logger): ExceptionHandler =
        ExceptionHandler { throwable ->
            logger.log(LogLevel.Error, "DishLab", throwable) { "Unhandled failure" }
        }
}

/**
 * Builds the graph for the Android composition root, so the app module does not
 * need the Metro compiler plugin just to call `createGraphFactory`.
 */
fun createAndroidAppGraph(context: Context): AppGraph =
    createGraphFactory<AndroidAppGraph.Factory>().create(context)
