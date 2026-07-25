# Getting started

How to stand up a new KMP app on this core. Follow it top to bottom once; after
that, use the per-module guides.

## 1. Install the template

Copy the `core/` folder into your project and follow
[core/README.md](../README.md) — three steps: `include(":core")`, plugin
versions in the root build file, and the two repositories.

Then depend on it:

```kotlin
// feature/auth/build.gradle.kts
kotlin.sourceSets.commonMain.dependencies {
    implementation(project(":core"))
}
```

Your app must use Kotlin **2.4.10**. Metro is a compiler plugin, so a mismatch
fails the build. See [Versions](#versions).

## 2. Apply the plugins

Every module that compiles Metro annotations needs the Metro plugin:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("dev.zacsweers.metro")
}
```

## 3. Install the logger

Do this first, before anything else runs — most core modules log through it.

```kotlin
// Application.onCreate() / iOS app init
Logger.install(
    DefaultLogger(
        if (BuildConfig.DEBUG) {
            platformLogSink()
        } else {
            MinLevelLogSink(crashReporterSink, LogLevel.Warn)
        }
    )
)
```

Skipping this is safe — the logger falls back to stdout — but you lose LogCat
tags and crash-reporter breadcrumbs.

## 4. Declare the graph

Scopes live in `core`: `AppScope` and `SessionScope`.

Define a shared contract in `commonMain`, and the real graph per platform —
a common graph cannot see platform-only contributions:

```kotlin
// commonMain
interface AppGraph {
    val notifications: NotificationController
    val storage: KeyValueStorage
}

// androidMain
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
    @SingleIn(AppScope::class)
    fun provideNotifications(context: Context): NotificationController =
        AndroidNotificationController(
            context = context,
            config = AndroidPushConfig(MainActivity::class.java, R.drawable.ic_notification),
        )

    @Provides
    fun provideNetworkConfig(): NetworkConfig =
        NetworkConfig(baseUrl = BuildConfig.API_URL, isDebug = BuildConfig.DEBUG)

    @Provides
    fun provideTokenProvider(storage: KeyValueStorage): AuthTokenProvider =
        AuthTokenProvider { storage.get(AuthKeys.Token) }

    @Provides
    fun provideLogger(): Logger = Logger
}
```

Create it **once**, at the composition root:

```kotlin
class App : Application() {
    lateinit var graph: AndroidAppGraph

    override fun onCreate() {
        super.onCreate()
        graph = createGraphFactory<AndroidAppGraph.Factory>().create(this)
    }
}
```

Never pass the graph itself into features — that turns compile-time DI back
into a service locator. Pass the dependencies.

## 5. Write a feature

```kotlin
// domain
interface LoginRepository {
    suspend fun login(credentials: Credentials): Session
}

// data — bound into the graph automatically
@ContributesBinding(AppScope::class)
@Inject
class LoginRepositoryImpl(
    private val client: HttpClient,
) : LoginRepository, Loggable {

    override val logTag: String = "Auth"

    override suspend fun login(credentials: Credentials): Session =
        logged("login") {
            client.post("login") { setBody(credentials.toDto()) }.body<SessionDto>().toDomain()
        }
}

// presentation
@Inject
class LoginViewModel(
    dependencies: CommonDependencies,
    private val repository: LoginRepository,
) : AbstractViewModel(dependencies), WithMviState<LoginUiState> {

    fun onSubmit(credentials: Credentials) = launch("login") {
        repository.login(credentials)
    }
}
```

That already logs:

```
D/LoginViewModel: created
D/LoginViewModel: → login
D/Auth: → login
D/Auth: ← login (142ms)
D/LoginViewModel: ← login (144ms)
```

## 6. Test it

No DI container is started in tests — an `@Inject` class is just a class:

```kotlin
val sink = RecordingLogSink()
val dependencies = CommonDependencies(DefaultLogger(sink), ExceptionHandler { })

val viewModel = LoginViewModel(dependencies, FakeLoginRepository())
```

Fakes that ship in main source, ready to use: `RecordingLogSink`,
`InMemoryKeyValueStorage`, `RecordingNotificationController`,
`FixedDateTimeProvider`.

## Versions

| | |
|---|---|
| Kotlin | 2.4.10 |
| AGP | 9.1.0 |
| Gradle | 9.3.1 |
| Metro | 1.3.2 |
| Ktor | 3.5.1 |
| DataStore | 1.2.1 |
| Compose Multiplatform | 1.11.1 |
| navigation-compose (JetBrains) | 2.9.2 |
| minSdk / compileSdk | 24 / 37 |

**Kotlin and Metro move together.** Metro is a compiler plugin with no stable
API guarantee; check its
[compatibility table](https://zacsweers.github.io/metro/latest/compatibility/)
before bumping either.

## Building

The default `java` on this machine is a JRE. Gradle needs a JDK:

```bash
JAVA_HOME=/home/denis/work/IDE/android-studio/jbr ./gradlew build
```

iOS targets cross-compile on Linux since Kotlin 2.4.10, so `iosMain` code is
verified here. Running iOS *tests* still requires a Mac.

## Guides

- [logging-and-errors.md](logging-and-errors.md) — logger, errors, entities, load state
- [presentation.md](presentation.md) — view-models and mixins
- [network.md](network.md) — HTTP client and failure mapping
- [datastore.md](datastore.md) — key-value storage
- [pushes.md](pushes.md) — notifications
- [navigation](../../navigation/README.md) — separate template
