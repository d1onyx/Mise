package com.d1onyx.core.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.d1onyx.core.datastore.DataStoreKeyValueStorage
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.logger.Logger
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Binds [KeyValueStorage] to the DataStore implementation.
 *
 * The `DataStore<Preferences>` itself is **not** provided here: building it
 * needs a platform-specific file path (`Context.filesDir` on Android, the
 * documents directory on iOS), so the final platform graph supplies it:
 *
 * ```
 * @DependencyGraph(AppScope::class)
 * interface AndroidAppGraph {
 *     @DependencyGraph.Factory
 *     fun interface Factory {
 *         fun create(@Provides context: Context): AndroidAppGraph
 *     }
 *
 *     @Provides
 *     @SingleIn(AppScope::class)
 *     fun provideDataStore(context: Context): DataStore<Preferences> =
 *         createPreferencesDataStore { context.preferencesDataStorePath() }
 * }
 * ```
 */
@ContributesTo(AppScope::class)
@BindingContainer
public interface DataStoreBindings {

    public companion object {

        @Provides
        @SingleIn(AppScope::class)
        public fun provideKeyValueStorage(
            dataStore: DataStore<Preferences>,
            logger: Logger,
        ): KeyValueStorage = DataStoreKeyValueStorage(dataStore, logger)
    }
}
