# DishLab — Mise Scanner

Kotlin Multiplatform app (Android + iOS) built on Compose Multiplatform: scan a
product barcode, see what it is made of and how it rates, combine several scans
into a graph, and cook the recipes that come out of it.

## Modules

| Module | What it holds |
|---|---|
| `core` | Vendored from `~/work/templates` — Metro DI, logger, errors, `AbstractViewModel`, Ktor, key-value storage |
| `navigation` | Vendored — `AppRouter`, `Route`, Compose dialogs |
| `design-system` | Mise theme (dark only), Space Grotesk / IBM Plex Mono, components, icons, animations |
| `domain` | Models, repository interfaces, use cases — no UI, no networking |
| `data` | Ktor product repository, local product cache, persistence, and temporary recipe data |
| `feature/home` | Home |
| `feature/scanner` | Camera viewfinder (CameraK + barcode plugin), manual entry, "not found" |
| `feature/products` | Combination graph, product sheet, scan history |
| `feature/recipes` | Recipe list, Saved, recipe detail, cooking mode |
| `shared` | App host: `AppNavHost`, `AppGraph`, feature router implementations, platform graphs |
| `androidApp` | `Application` (composition root), `MainActivity`, manifest |
| `iosApp` | Xcode wrapper around `MainViewController` |
| `backend` | Independent Ktor/JVM service for products, taxonomy, recipes, pantry, and user data |

Dependency rules: a feature never depends on another feature — anything shared
lives in `domain`; the router implementations live in `shared`, the only module
that knows every route.

`core` and `navigation` are copied from the templates unchanged, so updating them
is a plain folder copy. Everything else takes its versions from
`gradle/libs.versions.toml`.

## Data

Products are resolved through the local Ktor backend, which queries Open Food
Facts and returns the normalised DishLab product contract. The client keeps a
small local snapshot cache so products already added to the graph and scan
history remain available offline. There is no bundled product catalogue or
simulated scan flow. The first launch after this change removes legacy demo
products from the graph and scan history. Bundled recipes are temporary until
the recipe API is connected.

## Running

The machine's default `java` is a JRE; Gradle needs a JDK:

```bash
export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr

./gradlew build                    # everything, Android + iOS klibs
./gradlew allTests                 # 109 tests
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug # needs a device or emulator
```

Start the product API before scanning real barcodes:

```bash
cd backend
DEV_AUTH=true ./gradlew :app:run
```

The debug Android build defaults to `http://10.0.2.2:8080/`, which is the host
machine from an Android emulator. For a physical device, expose the Ktor server
on your LAN and install with its address:

```bash
./gradlew :androidApp:installDebug -PdishLabApiUrl=http://192.168.1.20:8080/
```

`DEV_AUTH=true` is local development only. Release builds do not include the
development token and must provide a real Firebase authentication token.

iOS: open [`/iosApp`](./iosApp) in Xcode and run from there (needs a Mac).

The backend has its own Gradle wrapper and JDK 21 toolchain:

```bash
cd backend
cp .env.example .env
docker compose up -d postgres
set -a && source .env && set +a
./gradlew flywayMigrate :app:run
```

See [`backend/README.md`](./backend/README.md) for API and configuration details.

## Requirements

| | |
|---|---|
| Kotlin | 2.4.10 — Metro is a compiler plugin, the versions move together |
| AGP / Gradle | 9.1.0 / 9.3.1 |
| compileSdk / minSdk | 37 / 24 |
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |
