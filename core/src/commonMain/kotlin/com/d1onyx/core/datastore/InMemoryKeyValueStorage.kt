package com.d1onyx.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A [KeyValueStorage] held entirely in memory.
 *
 * Ships in main rather than in test source so that every feature module can use
 * it as a fake without re-implementing one:
 *
 * ```
 * val storage = InMemoryKeyValueStorage()
 * val viewModel = SettingsViewModel(dependencies, storage)
 * ```
 */
public class InMemoryKeyValueStorage(
    initialValues: Map<PreferenceKey<*>, Any> = emptyMap(),
) : KeyValueStorage {

    private val values = MutableStateFlow(initialValues)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observe(key: PreferenceKey<T>): Flow<T?> =
        values.map { snapshot -> snapshot[key] as T? }

    override suspend fun <T : Any> put(key: PreferenceKey<T>, value: T) {
        values.value = values.value + (key to value)
    }

    override suspend fun <T : Any> remove(key: PreferenceKey<T>) {
        values.value = values.value - key
    }

    override suspend fun clear() {
        values.value = emptyMap()
    }
}
