package com.d1onyx.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.d1onyx.core.essentials.coroutines.DefaultDispatcherProvider
import com.d1onyx.core.essentials.coroutines.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath

/**
 * The default store file name. DataStore requires the `.preferences_pb`
 * suffix — it refuses to open a file without it.
 */
public const val DEFAULT_DATA_STORE_FILE_NAME: String = "app-data-store.preferences_pb"

/**
 * Create the app's preferences store at [producePath].
 *
 * The path is a parameter because there is no shared notion of "app storage":
 * Android derives it from `Context.filesDir`, iOS from the documents directory.
 * Each platform source set has a helper that produces it.
 *
 * Create this **once per app**. DataStore does not allow two live instances
 * over the same file — the second one will not observe the first one's writes,
 * and concurrent writes can corrupt the file. Scope it to the app graph.
 *
 * @param dispatchers supplies the IO dispatcher the store runs its reads and
 * writes on; overridable in tests
 * @param scope owns the store's background work; cancelling it releases the file
 */
public fun createPreferencesDataStore(
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    scope: CoroutineScope = CoroutineScope(dispatchers.io + SupervisorJob()),
    producePath: () -> String,
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    scope = scope,
    produceFile = { producePath().toPath() },
)
