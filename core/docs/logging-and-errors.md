# Logging, errors and foundations

Foundation types every other module builds on. Pure Kotlin — no framework,
no platform APIs except one logging sink per platform.


## Logging

### Set it up once

```kotlin
Logger.install(DefaultLogger(platformLogSink()))
```

Release builds usually want less, and want it in the crash reporter:

```kotlin
Logger.install(
    DefaultLogger(
        CompositeLogSink(
            MinLevelLogSink(platformLogSink(), LogLevel.Info),
            MinLevelLogSink(crashReporterSink, LogLevel.Warn),
        )
    )
)
```

Without `install`, logging still works and goes to stdout.

### Log from a class

Implement `Loggable` — the tag defaults to the class name:

```kotlin
class SyncScheduler : Loggable {
    fun schedule() {
        logD { "scheduling next sync" }
    }
}
```

Override `logTag` to group a whole feature under one tag, which is what makes
LogCat filtering useful:

```kotlin
class LoginRepositoryImpl : Loggable {
    override val logTag: String = "Auth"
}
```

Available: `logV`, `logD`, `logI`, `logW`, `logE`. The last two take an optional
`Throwable`.

Messages are lambdas, so a filtered-out record costs nothing — never build the
string yourself:

```kotlin
logD { "loaded ${items.size} items" }   // lambda not evaluated if dropped
```

### Trace an operation

`logged` wraps a call with entry, exit, duration and failure:

```kotlin
suspend fun login(credentials: Credentials): Session =
    logged("login") {
        api.login(credentials).toSession()
    }
```

```
D/Auth: → login
D/Auth: ← login (142ms)
```

On failure it logs at error level with the stack trace and rethrows.
Cancellation is logged at debug level and rethrown — a cancelled coroutine is
normal control flow, not an error.

`loggedBlocking` is the non-suspending variant.

### Test what was logged

```kotlin
val sink = RecordingLogSink()
Logger.install(DefaultLogger(sink))

assertTrue(sink.records.any { it.level == LogLevel.Error })
```

### Sinks

| Sink | Use |
|---|---|
| `platformLogSink()` | LogCat / NSLog |
| `ConsoleLogSink` | stdout; the default, fine for tests |
| `NoOpLogSink` | discard everything |
| `MinLevelLogSink(sink, level)` | raise the bar for an existing sink |
| `CompositeLogSink(a, b)` | fan out to several |
| `RecordingLogSink` | keep records in memory for assertions |

Write your own by implementing `LogSink` — that is how a crash reporter or a
file logger plugs in.

## Errors

A hierarchy that carries a user-facing localized message.

| Exception | Meaning |
|---|---|
| `AuthException` | session expired |
| `ConnectionException` | network unreachable |
| `BackendException` | server error, carries `httpCode`, `serverCode`, `backendMessage` |
| `InvalidBackendResponseException` | response could not be parsed |
| `RateLimitException` | throttled |
| `UnknownException` | unexpected |

the network package produces these automatically — see [network](network.md).

### Localized messages

Implement `CoreStringProvider` per platform and register it:

```kotlin
StringProviderStore(mapOf(CoreStringProvider::class to AndroidCoreStringProvider(context)))
```

Then any core exception can render itself:

```kotlin
val text = (exception as? WithLocalizedMessage)?.getLocalizedErrorMessage(store)
```

### Handling

```kotlin
// Bind one per app; core:presentation routes failures to it.
fun interface ExceptionHandler { fun handleException(exception: Throwable) }
```

`ExceptionToMessageMapper` turns an exception into display text; the default
returns `exception.message`.

## LoadState

Models an asynchronously loaded value.

```kotlin
sealed interface LoadState<out T> {
    data object Loading
    data class Success<T>(val value: T, val isStale: Boolean = false)
    data class Failure(val exception: Throwable)
}
```

`isStale` lets the UI show cached content *and* a refresh indicator at once.

```kotlin
val profile: Flow<LoadState<Profile>> = loadStateOf("Profile") { api.fetchProfile() }

// or wrap an existing stream — failures are logged under the tag
repository.observeProfile().asLoadState("Profile")
```

Operators: `map`, `fold`, `getOrNull`, `exceptionOrNull`, `onSuccess`,
`onFailure`, `isLoading` / `isSuccess` / `isFailure`.

```kotlin
state.fold(
    onLoading = { Spinner() },
    onSuccess = { ProfileCard(it) },
    onFailure = { ErrorView(it) },
)
```

> Replaces `com.elveum:container`, which is JVM-only.

## Entities

`Id` and its subtypes (`UserId`, `EventId`) wrap a `String` with type safety:

```kotlin
val id: UserId = UserId("u-42")
```

Define your own by extending `AbstractId`. the network package has matching JSON
serializers.

`HttpCode` / `ServerCode` are value classes. `ImageSource` is a sealed type
(`Empty` / `Remote(url)` / `Local(key)`) consumed by shared image components.

## Date and time

```kotlin
DateTimeProvider.now()               // kotlinx.datetime.Instant
DateTimeProvider.currentTimeMillis()
instant.toLocalDateTimeInAppZone()
```

For deterministic tests:

```kotlin
DateTimeProvider.install(FixedDateTimeProvider(Instant.parse("2026-01-01T00:00:00Z")))
// ...
DateTimeProvider.reset()
```

## Other abstractions

Interfaces with no core implementation — the app supplies one per platform:

- `Dialogs` / `DialogConfig` — alert dialogs, `suspend` result
- `Toaster` — short messages
- `PermissionRequester` / `Permission` / `PermissionStatus`
- `IntentLauncher` — open app or notification settings
- `WithAppLifecycle` — run something once on app start

## DI scopes

```kotlin
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
```

`AppScope` — the app process. `SessionScope` — an authenticated session, meant
to be used via a `@GraphExtension` so logout discards everything at once.

Both are marker classes, not `@Scope` annotations — that is what Metro expects
for aggregation.

## Dispatchers

Inject `DispatcherProvider` into repositories and data sources instead of using
`Dispatchers.*` directly:

```kotlin
@ContributesBinding(AppScope::class, binding = binding<ChatRepository>())
@Inject
class ChatRepositoryImpl(
    private val api: ChatApi,
    private val dispatchers: DispatcherProvider,
) : ChatRepository {
    override suspend fun messages(id: ChatId) =
        withContext(dispatchers.io) { api.fetch(id).map { it.toDomain() } }
}
```

Two reasons this exists:

- **`Dispatchers.IO` does not exist on Kotlin/Native.** Hardcoding it builds on
  Android and fails to resolve on iOS. `dispatchers.io` maps to a real IO pool
  on the JVM and to `Dispatchers.Default` on Native, which is correct there.
- **Tests can replace it.** `TestDispatcherProvider` (ships in main source)
  points every dispatcher at one `StandardTestDispatcher`, so data-layer
  coroutines run on the test scheduler under `runTest`:

```kotlin
val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
val repository = ChatRepositoryImpl(FakeChatApi(), dispatchers)
```

`DefaultDispatcherProvider` is bound in the graph automatically, so a repository
just declares the parameter.

## Utilities

```kotlin
items.mapAsync { fetch(it) }        // concurrent map, order preserved
items.mapNotNullAsync { fetch(it) } // same, drops nulls
flow.throttle(periodMillis = 500)   // first item through, then latest per window
```

Paging contracts: `PagedData<T>` and `PageToken`.
