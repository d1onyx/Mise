package com.d1onyx.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Key-value storage for app settings and small pieces of state.
 *
 * Features depend on this interface rather than on DataStore directly, so a
 * unit test can swap in [InMemoryKeyValueStorage] without touching the disk.
 *
 * **Not for secrets.** Values are stored unencrypted. Auth tokens, refresh
 * tokens and keys belong in the platform keystore (Android Keystore /
 * EncryptedSharedPreferences, iOS Keychain), not here.
 */
public interface KeyValueStorage {

    /**
     * Observe a value, emitting the current one immediately and again on each
     * change. Emits `null` while the key is absent.
     */
    public fun <T : Any> observe(key: PreferenceKey<T>): Flow<T?>

    /**
     * Store [value] under [key].
     */
    public suspend fun <T : Any> put(key: PreferenceKey<T>, value: T)

    /**
     * Remove [key], if present.
     */
    public suspend fun <T : Any> remove(key: PreferenceKey<T>)

    /**
     * Remove everything. Intended for logout.
     */
    public suspend fun clear()
}

/**
 * Read a value once.
 */
public suspend fun <T : Any> KeyValueStorage.get(key: PreferenceKey<T>): T? =
    observe(key).first()

/**
 * Read a value once, falling back to [defaultValue] when absent.
 */
public suspend fun <T : Any> KeyValueStorage.getOrDefault(
    key: PreferenceKey<T>,
    defaultValue: T,
): T = get(key) ?: defaultValue
