# Networking

Ktor HTTP client, preconfigured: JSON, timeouts, bearer auth, logging, and
translation of every failure into `core` exception types.


## Create a client

```kotlin
val client = createHttpClient(
    config = NetworkConfig(baseUrl = "https://api.example.com/", isDebug = true),
    logger = Logger,
    tokenProvider = { sessionStorage.token() },
)
```

Through DI, `NetworkBindings` already provides `HttpClient` and `Json` scoped to
`AppScope`. You supply the two app-specific pieces:

```kotlin
@Provides
fun provideNetworkConfig() = NetworkConfig(BuildConfig.API_URL, BuildConfig.DEBUG)

@Provides
fun provideTokenProvider(storage: KeyValueStorage) =
    AuthTokenProvider { storage.get(AuthKeys.Token) }
```

Use `AuthTokenProvider.None` for unauthenticated clients.

## Making requests

Plain Ktor from here:

```kotlin
@Inject
class ProfileApi(private val client: HttpClient) {

    suspend fun fetchProfile(id: UserId): ProfileDto =
        client.get("users/$id").body()

    suspend fun updateProfile(profile: ProfileDto) {
        client.put("users/${profile.id}") { setBody(profile) }
    }
}
```

Base URL and `Content-Type: application/json` are already applied.

## Failures

Callers never see a Ktor exception:

| Response | Exception |
|---|---|
| 401 | `AuthException` |
| 429 | `RateLimitException` |
| other non-2xx | `BackendException(httpCode, serverCode, backendMessage)` |
| unparseable body | `InvalidBackendResponseException` |
| timeout, no connection | `ConnectionException` |

`serverCode` and `backendMessage` come from the error body:

```json
{ "errcode": "M_ROOM_MISSING", "error": "room not found" }
```

A malformed error body never masks the status code — parsing degrades to empty.

### Feature-specific exceptions

Contribute a `BackendExceptionMapper`:

```kotlin
@ContributesIntoSet(AppScope::class)
@Inject
class RoomErrorMapper : BackendExceptionMapper by BackendExceptionMapper.forServerCode(
    serverCode = "M_ROOM_MISSING",
    transform = { RoomMissingException(it) },
)
```

`forHttpCode(404) { ... }` matches by status instead. Mappers run in order; the
first one that returns something different wins. Return the original exception
to decline.

> Replaces the Retrofit `@MapHttpCodeToException` / `@MapServerCodeToException`
> annotations, which were read reflectively — impossible on Kotlin/Native.

## JSON

`createDefaultJson(isDebug)` gives snake_case naming, `ignoreUnknownKeys = true`
(a new backend field must never break a shipped client), `explicitNulls = false`,
and contextual serializers for `Id`, `UserId`, `EventId`.

```kotlin
@Serializable
data class ProfileDto(
    val firstName: String,          // reads "first_name"
    @Contextual val id: UserId,     // reads a plain string
)
```

Add your own:

```kotlin
createDefaultJson(
    isDebug = true,
    extraModule = SerializersModule { contextual(ChatId::class, ChatIdSerializer) },
)
```

For a custom id type, extend `IdSerializer`:

```kotlin
object ChatIdSerializer : IdSerializer<ChatId>(
    serialName = "com.example.ChatId",
    idFactory = ChatId::invoke,
)
```

## Logging

HTTP traffic goes through the app `Logger` under the `Http` tag, so it lands in
the same sinks — including the crash reporter.

Bodies are logged only when `isDebug`; they carry tokens and personal data.

## Testing

Pass Ktor's `MockEngine`:

```kotlin
val engine = MockEngine { respond("""{"first_name":"Ada"}""", HttpStatusCode.OK, jsonHeaders) }
val client = createHttpClient(config, DefaultLogger(sink), engine = engine)
```

Assert what was sent through `engine.requestHistory`:

```kotlin
assertEquals("Bearer token", engine.requestHistory.single().headers[HttpHeaders.Authorization])
```

## Platform engines

OkHttp on Android, Darwin (NSURLSession) on iOS, selected automatically by
`platformHttpClientEngine()`. Override via the `engine` parameter.

## Notes

- Timeouts come from `NetworkConfig` (default 10s for request, connect, socket).
- The client is scoped to `AppScope` — it owns a connection pool, so do not
  create one per call.
- `longPollTimeout` / `longPollRetryTimeout` exist on `NetworkConfig` but are not
  wired into the default client; build a second client for long-polling.
