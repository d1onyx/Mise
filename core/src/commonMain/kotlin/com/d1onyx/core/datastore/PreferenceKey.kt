package com.d1onyx.core.datastore

/**
 * A typed key into [KeyValueStorage].
 *
 * Keys are values, so a feature declares its own once and reuses it:
 *
 * ```
 * object SettingsKeys {
 *     val DarkTheme = PreferenceKey.BooleanKey("dark_theme")
 *     val LastSyncMillis = PreferenceKey.LongKey("last_sync_millis")
 * }
 * ```
 *
 * The sealed hierarchy is what keeps the storage API typed without reflection —
 * the implementation maps each case to its platform key in an exhaustive `when`,
 * so adding a type here fails to compile until it is handled everywhere.
 */
public sealed interface PreferenceKey<T : Any> {

    public val name: String

    public data class StringKey(override val name: String) : PreferenceKey<String>

    public data class IntKey(override val name: String) : PreferenceKey<Int>

    public data class LongKey(override val name: String) : PreferenceKey<Long>

    public data class BooleanKey(override val name: String) : PreferenceKey<Boolean>

    public data class DoubleKey(override val name: String) : PreferenceKey<Double>

    public data class StringSetKey(override val name: String) : PreferenceKey<Set<String>>
}
