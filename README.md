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
| `data` | The bundled demo catalogue (`composeResources/files/*.json`) and the repositories over it |
| `feature/home` | Home |
| `feature/scanner` | Camera viewfinder (CameraK + barcode plugin), manual entry, "not found" |
| `feature/products` | Combination graph, product sheet, scan history |
| `feature/recipes` | Recipe list, Saved, recipe detail, cooking mode |
| `shared` | App host: `AppNavHost`, `AppGraph`, feature router implementations, platform graphs |
| `androidApp` | `Application` (composition root), `MainActivity`, manifest |
| `iosApp` | Xcode wrapper around `MainViewController` |

Dependency rules: a feature never depends on another feature — anything shared
lives in `domain`; the router implementations live in `shared`, the only module
that knows every route.

`core` and `navigation` are copied from the templates unchanged, so updating them
is a plain folder copy. Everything else takes its versions from
`gradle/libs.versions.toml`.

## Data

The product and recipe data is a bundled demo catalogue, read from
`data/src/commonMain/composeResources/files/`. Replacing it with OpenFoodFacts
means implementing `ProductRepository` in `data` — no feature changes.

### Demo mode

While `DemoMode.ALWAYS_RESOLVE_SCANS` is on (`data/.../demo/DemoMode.kt`), the
app is walkable end to end without a product API:

- any barcode resolves — an unknown one is mapped deterministically onto a
  catalogue product instead of failing;
- barcode `000000000000` still reports «not found», so that screen stays
  reachable — the Scan screen's `simulate "not found"` action uses it;
- «Capture scan» adds the next catalogue product without touching the camera,
  so an emulator or a denied permission is not a dead end;
- Saved and History are seeded once per installation by `DemoDataSeeder`, so no
  screen opens empty.

Turn the flag off and delete `demo/` when the real client lands.

## Running

The machine's default `java` is a JRE; Gradle needs a JDK:

```bash
export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr

./gradlew build                    # everything, Android + iOS klibs
./gradlew allTests                 # 109 tests
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug # needs a device or emulator
```

iOS: open [`/iosApp`](./iosApp) in Xcode and run from there (needs a Mac).

## Requirements

| | |
|---|---|
| Kotlin | 2.4.10 — Metro is a compiler plugin, the versions move together |
| AGP / Gradle | 9.1.0 / 9.3.1 |
| compileSdk / minSdk | 37 / 24 |
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |
