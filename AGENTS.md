# Repository Guidelines

## Project Structure & Module Organization

DishLab is a Kotlin Multiplatform Compose app for Android and iOS. Shared Kotlin code uses KMP source sets such as `src/commonMain`, `src/commonTest`, `src/androidMain`, and `src/iosMain`.

- `androidApp`: Android `Application`, `MainActivity`, manifest, and resources.
- `iosApp`: Xcode wrapper around the shared `MainViewController`.
- `shared`: app host, navigation graph, app-level DI, and feature router implementations.
- `domain`: models, repository contracts, and use cases. Keep it free of UI and networking.
- `data`: demo catalogue repositories and JSON under `data/src/commonMain/composeResources/files/`.
- `feature/home`, `feature/scanner`, `feature/products`, `feature/recipes`: isolated UI and presentation flows.
- `design-system`: theme, reusable Compose components, icons, animations, and resources.
- `core` and `navigation`: vendored template modules; avoid local edits unless updating the template copy.

Features should not depend on other features. Move shared contracts to `domain`; wire cross-feature routing in `shared`.

## Build, Test, and Development Commands

Set a JDK before running Gradle on this machine:

```bash
export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr
./gradlew build
./gradlew allTests
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

`build` compiles all modules, including Android and iOS klibs. `allTests` runs the multiplatform test suite. `assembleDebug` creates the Android debug APK, and `installDebug` deploys it to a connected emulator or device. For iOS, open `iosApp` in Xcode.

## Coding Style & Naming Conventions

Use Kotlin 2.4.10, Compose Multiplatform, Metro DI, and Kotlin serialization versions from `gradle/libs.versions.toml`. Follow standard Kotlin formatting: four-space indentation, `PascalCase` types/composables, `camelCase` functions and properties, and `UPPER_SNAKE_CASE` constants. Name preview-only Android files with the existing `*Previews.android.kt` pattern. Keep resources in the owning module's `composeResources` or Android `res` directory.

## Testing Guidelines

Tests use `kotlin.test`, JUnit where needed, and `kotlinx-coroutines-test`. Place common tests in `src/commonTest/kotlin`; platform tests belong in source sets such as `androidHostTest` or `iosTest`. Name test classes after the subject, for example `FilterRecipesUseCaseTest` or `ScanViewModelTest`. Run `./gradlew allTests` before submitting behavior changes.

## Commit & Pull Request Guidelines

The visible Git history currently contains only `Initial commit`, so there is no established convention. Use short imperative subjects, for example `Add recipe filtering tests`, and keep unrelated changes separate. Pull requests should describe the user-visible change, mention affected modules, link issues when available, include screenshots or recordings for UI changes, and list the Gradle/Xcode checks performed.

## Security & Configuration Tips

Do not commit local SDK paths, secrets, generated build outputs, or machine-specific IDE files. Keep demo-mode changes explicit in `data/.../demo/DemoMode.kt`, and document when a branch depends on `DemoMode.ALWAYS_RESOLVE_SCANS`.
