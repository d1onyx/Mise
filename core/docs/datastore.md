# Key-value storage

Typed key-value storage on AndroidX DataStore, for settings and small state.


> **Not for secrets.** Values are stored unencrypted. Auth tokens and keys belong
> in the Android Keystore / iOS Keychain.

## Declare keys

Keys are values — declare them once per feature:

```kotlin
object SettingsKeys {
    val DarkTheme = PreferenceKey.BooleanKey("dark_theme")
    val LastSync = PreferenceKey.LongKey("last_sync_millis")
    val UserName = PreferenceKey.StringKey("user_name")
    val MutedChats = PreferenceKey.StringSetKey("muted_chats")
}
```

Types: `StringKey`, `IntKey`, `LongKey`, `BooleanKey`, `DoubleKey`,
`StringSetKey`.

## Read and write

```kotlin
@Inject
class SettingsRepository(private val storage: KeyValueStorage) {

    val darkTheme: Flow<Boolean> =
        storage.observe(SettingsKeys.DarkTheme).map { it ?: false }

    suspend fun setDarkTheme(enabled: Boolean) =
        storage.put(SettingsKeys.DarkTheme, enabled)

    suspend fun lastSync(): Long =
        storage.getOrDefault(SettingsKeys.LastSync, defaultValue = 0L)

    suspend fun onLogout() = storage.clear()
}
```

| Call | Returns |
|---|---|
| `observe(key)` | `Flow<T?>` — current value, then every change |
| `get(key)` | `T?` — one-shot read |
| `getOrDefault(key, default)` | `T` |
| `put(key, value)` | — |
| `remove(key)` | — |
| `clear()` | — drops everything, for logout |

`observe` emits `null` while a key is absent. A corrupted or unreadable file
degrades to "no value" and logs the error, rather than failing every screen.

## Wiring it up

`DataStoreBindings` binds `KeyValueStorage` for you. The `DataStore<Preferences>`
itself is platform-specific, so the platform graph provides it:

```kotlin
// androidMain
@Provides
@SingleIn(AppScope::class)
fun provideDataStore(context: Context): DataStore<Preferences> =
    createPreferencesDataStore { context.preferencesDataStorePath() }

// iosMain
@Provides
@SingleIn(AppScope::class)
fun provideDataStore(): DataStore<Preferences> =
    createPreferencesDataStore { preferencesDataStorePath() }
```

**Create it once per app.** DataStore forbids two live instances over one file:
the second will not see the first one's writes, and concurrent writes corrupt
it. `@SingleIn(AppScope::class)` is not optional here.

The factory takes a `CoroutineScope`; cancelling it releases the file. The
default scope lives for the process, which is what an app wants.

Use a separate file for an isolated concern:

```kotlin
createPreferencesDataStore { context.preferencesDataStorePath("onboarding.preferences_pb") }
```

The `.preferences_pb` suffix is mandatory — DataStore refuses other names.

## Testing

```kotlin
val storage = InMemoryKeyValueStorage()

// or seeded
val storage = InMemoryKeyValueStorage(mapOf(SettingsKeys.DarkTheme to true))
```

Same interface, no disk, no dispatcher setup.

## Storing objects

Only the six primitive kinds are supported. For a structured value, serialize it
yourself:

```kotlin
private val ProfileKey = PreferenceKey.StringKey("cached_profile")

suspend fun saveProfile(profile: Profile) =
    storage.put(ProfileKey, json.encodeToString(profile))

suspend fun cachedProfile(): Profile? =
    storage.get(ProfileKey)?.let { runCatching { json.decodeFromString<Profile>(it) }.getOrNull() }
```

If you find yourself doing this for collections, you want a database, not a
preferences file — DataStore holds the entire file in memory and rewrites it on
every write.
