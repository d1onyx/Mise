# core

Reusable Kotlin Multiplatform foundation: logging, error handling, view-models,
HTTP, key-value storage and notifications. Targets **Android + iOS**.

Self-contained — no version catalog, no convention plugins. Copy the folder,
add two things to the host project, done.

## Install

**1.** Copy this `core/` folder into your project.

**2.** Add it to `settings.gradle.kts`:

```kotlin
include(":core")
```

**3.** Declare the plugin versions in the root `build.gradle.kts` — the module
declares these plugins without a version:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.4.10" apply false
    id("com.android.kotlin.multiplatform.library") version "9.1.0" apply false
    id("dev.zacsweers.metro") version "1.3.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
}
```

**4.** Make sure `settings.gradle.kts` has both repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then depend on it:

```kotlin
// feature/auth/build.gradle.kts
kotlin.sourceSets.commonMain.dependencies {
    implementation(project(":core"))
}
```

### Requirements

| | |
|---|---|
| Kotlin | 2.4.10 — Metro is a compiler plugin, the version must match |
| AGP | 9.1.0 |
| compileSdk | 37 (`androidx.lifecycle` 2.11 requires it) |
| minSdk | 24 |
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |

Add to `gradle.properties`, or AGP warns about the compile SDK:

```properties
android.suppressUnsupportedCompileSdk=37
```

## What you get

| Package | Contents |
|---|---|
| `essentials.logger` | `Logger`, `Loggable`, `logged { }`, platform sinks |
| `essentials.exceptions` | error hierarchy with localized messages, `ExceptionHandler` |
| `essentials.loading` | `LoadState<T>` |
| `essentials.entities` | `Id`, `UserId`, `HttpCode`, `ImageSource` |
| `essentials.datetime` | `DateTimeProvider` on `kotlin.time.Instant` |
| `essentials.di` | `AppScope`, `SessionScope` |
| `presentation` | `AbstractViewModel`, mixins, MVI progress |
| `network` | Ktor client, JSON, bearer auth, failure mapping |
| `datastore` | typed key-value storage |
| `pushes` | local notifications |

## First steps

Install the logger before anything else runs — most of the module logs through it:

```kotlin
Logger.install(
    DefaultLogger(
        if (isDebug) platformLogSink()
        else MinLevelLogSink(crashReporterSink, LogLevel.Warn)
    )
)
```

Then a feature becomes traceable by implementing one interface:

```kotlin
@ContributesBinding(AppScope::class)
@Inject
class LoginRepositoryImpl(
    private val client: HttpClient,
) : LoginRepository, Loggable {

    override val logTag: String = "Auth"

    override suspend fun login(credentials: Credentials): Session =
        logged("login") {
            client.post("login") { setBody(credentials) }.body<SessionDto>().toDomain()
        }
}
```

```
D/Auth: → login
D/Auth: ← login (142ms)
```

## Guides

- [getting-started.md](docs/getting-started.md) — full walkthrough: DI graph, a
  feature end to end, testing
- [logging-and-errors.md](docs/logging-and-errors.md) — logger, sinks, error
  types, `LoadState`, entities, date/time
- [presentation.md](docs/presentation.md) — `AbstractViewModel`, mixins
- [network.md](docs/network.md) — HTTP client, failure mapping
- [datastore.md](docs/datastore.md) — key-value storage
- [pushes.md](docs/pushes.md) — notifications

## Testing

Fakes ship in main source, so features assert against them without extra deps:
`RecordingLogSink`, `InMemoryKeyValueStorage`, `RecordingNotificationController`,
`FixedDateTimeProvider`.

No DI container is started in tests — an `@Inject` class is just a class.

## Notes

- **DI is Metro**, a compiler plugin. No KSP, no KAPT, no runtime container;
  a missing binding fails the build. Kotlin and Metro versions move together —
  check Metro's compatibility table before bumping either.
- **iOS targets cross-compile on Linux** since Kotlin 2.4.10, so `iosMain` is
  verified anywhere. Running iOS *tests* still needs a Mac.
- **No `iosX64`** — the Intel simulator is being dropped across the ecosystem.
- Navigation lives in a separate [`navigation`](../navigation) template.
