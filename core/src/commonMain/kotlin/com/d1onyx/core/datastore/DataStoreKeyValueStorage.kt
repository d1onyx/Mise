package com.d1onyx.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

/**
 * [KeyValueStorage] backed by AndroidX DataStore Preferences, which is
 * multiplatform as of 1.1.
 */
public class DataStoreKeyValueStorage(
    private val dataStore: DataStore<Preferences>,
    override val logger: Logger = Logger,
) : KeyValueStorage, Loggable {

    override val logTag: String = "DataStore"

    override fun <T : Any> observe(key: PreferenceKey<T>): Flow<T?> =
        dataStore.data
            .catch { throwable ->
                if (throwable is CancellationException) throw throwable
                // A corrupted or unreadable file must degrade to "no value
                // stored" rather than take down every screen observing it.
                logE(throwable) { "failed to read '${key.name}', falling back to empty" }
                emit(emptyPreferences())
            }
            .map { preferences -> preferences[key.toPlatformKey()] }

    override suspend fun <T : Any> put(key: PreferenceKey<T>, value: T) {
        dataStore.edit { preferences -> preferences[key.toPlatformKey()] = value }
    }

    override suspend fun <T : Any> remove(key: PreferenceKey<T>) {
        dataStore.edit { preferences -> preferences.remove(key.toPlatformKey()) }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> PreferenceKey<T>.toPlatformKey(): Preferences.Key<T> = when (this) {
    is PreferenceKey.StringKey -> stringPreferencesKey(name)
    is PreferenceKey.IntKey -> intPreferencesKey(name)
    is PreferenceKey.LongKey -> longPreferencesKey(name)
    is PreferenceKey.BooleanKey -> booleanPreferencesKey(name)
    is PreferenceKey.DoubleKey -> doublePreferencesKey(name)
    is PreferenceKey.StringSetKey -> stringSetPreferencesKey(name)
} as Preferences.Key<T>
