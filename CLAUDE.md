# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

DishLab is a Kotlin Multiplatform Compose app (Android + iOS) — scan a product
barcode, see what it is made of, combine scans into a graph, cook the recipes
that come out of it. `backend/` is a separate Ktor/JVM service in the same
working tree.

See also: [README.md](README.md) (modules, running), [AGENTS.md](AGENTS.md)
(contribution conventions), [backend/README.md](backend/README.md).

README drifted behind the code: its version table still lists AGP/Gradle
9.1.0 / 9.3.1. Where it disagrees with this file, this file was verified against
the build. AGENTS.md carries the same Git rules as the section below — Codex
reads that one, Claude reads this one, and the two must stay identical.

## Commands

The machine's default `java` is a JRE — Gradle needs a JDK:

```bash
export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr

./gradlew build                     # all modules, Android + iOS klibs
./gradlew allTests                  # whole multiplatform suite
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug  # needs a device or emulator
```

A single test class or method runs on the JVM host-test task, **not** on
`allTests` (an aggregate lifecycle task that takes no `--tests` filter):

```bash
./gradlew :domain:testAndroidHostTest --tests "*FilterRecipesUseCaseTest*"
./gradlew :feature:products:testAndroidHostTest --tests "*GraphViewModelTest.adds*"
```

iOS tests cannot run on Linux (`kotlin.native.ignoreDisabledTargets=true`), and
`linkDebugFramework*` is SKIPPED without macOS — `iosMain` still compiles to a
klib here, so type errors are caught, but the framework and the Xcode preview
canvas are unverified on this machine.

The backend is an **independent Gradle build** with its own wrapper and a JDK 21
toolchain — root `./gradlew` does not reach it:

```bash
cd backend
cp .env.example .env
docker compose up -d postgres
set -a && source .env && set +a
./gradlew flywayMigrate && DEV_AUTH=true ./gradlew :app:run

./gradlew test                                          # unit + acceptance
./gradlew :application:test --tests "*ProductCatalogServiceTest*"
```

Sourcing `.env` is belt-and-braces: `env(key)` in `app/…/App.kt` falls back to
the first `.env` found by walking up four directories, so `:app:run` picks it up
even without `source`. Environment variables still win over the file.

The debug Android build points at `http://10.0.2.2:8080/` (the host machine as
seen from an emulator). Override for a physical device:
`./gradlew :androidApp:installDebug -PdishLabApiUrl=http://192.168.1.20:8080/`.

## Git workflow

Several agents work here at once, each in its own worktree. **`master` is the
only branch on the remote** — feature branches stay local, and their commits
reach GitHub through the merge, not through a push. Roles and task routing
(now four: `ceo-advisor`, `product-manager`, `backender`, `mobile-dev` — no
dedicated `tester`/`merger`) are detailed in
[HERDR-WORKFLOW.md](HERDR-WORKFLOW.md).

**One task, one branch, one worktree.**

```bash
herdr task claim t-13 --worktree
```

herdr creates the checkout itself, at `~/.herdr/worktrees/DishLab/task-t-13`,
and records it on the task — no manual `git worktree add`. This only picks up
the branch name `product-manager` set on the task at creation
(`--branch <type>/t-<id>-<slug>`); an unset `--branch` makes herdr invent its
own `task/t-<id>`, which breaks the naming convention below.

Name is `<type>/t-<id>-<slug>`, where `t-<id>` is the Herdr task ID. Types:
`feat`, `fix`, `chore`, `docs`. The branch lives exactly as long as the task.
A worktree is not optional — a checkout is global to the clone, so two agents
cannot sit on different branches in the same directory.

**Commits** follow Conventional Commits, with the task as a trailer:

```
feat(scanner): add torch toggle to the viewfinder

Task: t-13
```

Commit messages are English going forward (owner's rule, 2026-08-06). Existing
Ukrainian-language commits in history stay as they are — do not rewrite past
commits to comply.

Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`. Scope is a Gradle
module name, and only these — do not invent new ones:

```
androidApp  iosApp  shared  domain  data  design-system  core  navigation
home  scanner  products  recipes        (feature modules, without the prefix)
backend                                 (the whole separate build)
```

A change spanning several modules drops the scope: `feat: …`.

Every commit must leave the build green on its own: history is preserved through
merges, and one broken intermediate commit poisons `git bisect` for everyone
after it.

Conventional Commits are not decoration here — the Play Store "What's new" text
is generated from `feat:` and `fix:` subjects since the previous tag.

**Only `product-manager` merges.** `backender` and `mobile-dev` stop after
committing — they do not merge, do not push, do not touch `master`.
`product-manager` runs the mechanical gates from
[HERDR-WORKFLOW.md](HERDR-WORKFLOW.md) (build green, test-file count did not
drop) and merges:

```bash
git merge --no-ff feat/t-13-remove-startup-auth -m "Merge t-13: remove startup auth"
git push origin master
```

`--no-ff` keeps every commit of the branch inside `master`'s history — that is
what makes local-only branches safe. **`--squash` and rebase-merge are
forbidden**: both collapse the history this setup exists to preserve.

**No pull requests.** `origin` holds exactly one branch: `master`. Never
`git push -u origin <branch>`, never open a PR, never use the GitHub merge
button. This is not a style preference — the owner chose local merges over the
PR flow deliberately. It went the other way once, before the rule existed
(PR #1), and the stray remote branch had to be deleted by hand afterwards.

**Never `git add -A`.** Stage explicit paths — this tree carries ~4 GB of
untracked data (`FoodData_Central_csv_2026-04-30/`, `backend/data/`) plus
`androidApp/google-services.json`.

**Do not edit the planning documents** — `PRD-MVP.md`, `MVP-PLAN.md`,
`DECISIONS-MVP.md`. They belong to the owner and the `product-manager` role, and
they are the only files every agent would otherwise edit at once.

**Never** `git push --force`, `git reset --hard`, or rebase a pushed branch
unless explicitly asked.

Releases are **tags** on `master` (`v0.1.0`, SemVer) plus a GitHub Release
carrying the APK. There is no long-lived release branch.

## Architecture

### Module graph

`androidApp` / `iosApp` → `shared` → `feature/*` → `design-system`, `domain`,
`core`, `navigation`; `data` implements `domain`'s contracts.

**A feature never depends on another feature.** Anything two features need moves
to `domain`. Cross-feature navigation is expressed as a per-feature router
interface, implemented in `shared`.

### DI — Metro, with a platform-split graph

`AppGraph` (`shared/commonMain`) is an interface listing exactly what the UI may
pull out: the router holder, dialogs, and one accessor per view-model. The real
`@DependencyGraph` is per-platform (`AndroidAppGraph`, `IosAppGraph`) because a
common graph cannot see Android-only contributions such as the DataStore path or
`FirebaseAuth`. `createAndroidAppGraph(...)` is the seam that keeps the Metro
compiler plugin out of `androidApp`.

View-model accessors are **unscoped** — every `viewModel { graph.x }` call in the
nav host gets a fresh instance owned by its navigation entry. Screens that take
an argument get an assisted `Factory` instead (`scanNotFoundViewModelFactory`,
`recipeDetailViewModelFactory`, …).

Bindings are contributed with `@ContributesBinding(AppScope::class)`, so adding
an implementation rarely means editing a graph.

### Navigation

Each feature declares its own `@Serializable` routes and a `<Feature>Router`
interface (what it is allowed to reach). `shared/navigation/AppRouters.kt` holds
every implementation — the only module that knows all routes. `AppNavHost` is the
one place mapping route → view-model, and it attaches the live `AppRouter` to
`AppRouterHolder` in a `DisposableEffect` so injected code can navigate.

`AppRouterHolder` must stay `@SingleIn(AppScope::class)`; unscoped, every
injection site gets its own holder and all navigation commands are silently
dropped while unit tests still pass.

### Screen convention (MVI)

Non-negotiable in this repo:

1. **One `onAction(action)`**, not a list of constructor callbacks. The `when`
   has **no `else`**, so a new action fails the build until it is handled.
2. **`Screen()` / `Content()` split.** `<Name>Screen(viewModel)` resolves state
   via `collectAsStateWithLifecycle()` and delegates to
   `internal fun <Name>Content(state, onAction, modifier)`. No view-model inside
   `Content`. `internal`, not `private` — the previews live in other source sets.
3. **Previews are mandatory** for screens and reusable components (see below).
4. **No inline user-visible strings** — everything goes through `stringResource`
   from the module's own `composeResources/values/strings.xml`.

View-models extend `AbstractViewModel(dependencies)` from `core` and implement
`WithMviState<T>`; `WithInitCallback.onInitialized()` is the place for suspend
setup (the base class yields first, so it never runs before the subclass
constructor finishes).

### Previews are platform-split across three source sets

```
commonMain/…/PreviewStates.kt              internal object <Name>PreviewStates — shared fixtures
androidMain/…/<Feat>Previews.android.kt    @Preview functions, <Name>ContentPreview
iosMain/…/<Feat>Previews.ios.kt            <name>PreviewController(): UIViewController
```

Fixtures are shared deliberately: any difference between the platforms is then a
*rendering* difference, never a difference in the input. Android carries the full
per-state matrix through the multipreview annotations in
`design-system/androidMain/…/preview/MisePreviewAnnotations.kt`
(`@MiseScreenPreviews`, `@MiseComponentPreview`, `@MiseWidthPreviews`). iOS
carries a curated subset — each one builds and boots the framework, so it covers
only what a device can answer (safe-area insets, keyboard, type rasterisation).

`@Preview` is imported from **`androidx.compose.ui.tooling.preview`** — since
CMP 1.11 that artifact is itself multiplatform. It still belongs in `androidMain`
only, because the renderer does.

An iOS preview is invisible to Xcode until it is exported: feature modules are
`implementation` dependencies of `:shared`, so a controller must be re-exposed
through `object DishLabPreviews` in `shared/iosMain/…/Previews.kt` and consumed
from `iosApp/iosApp/Previews.swift` (`DishLabPreviews.shared.home()`). The Xcode
project uses `objectVersion = 77` with synchronized file groups, so new `.swift`
files need no `.pbxproj` edit.

### Data — the product lookup is device-first, not server-first

The order matters and is easy to get backwards:

1. `OpenFoodFactsProductDataSource` calls **Open Food Facts from the device**
   (`api/v3.6/product/{barcode}.json`). It builds its own `HttpClient` with
   `AuthTokenProvider.None` — deliberately, so the DishLab bearer token can
   never reach a third party. Do not give it the shared authenticated client.
2. The resulting `ClientProductSnapshotDto` is POSTed to the DishLab backend at
   `api/v1/products/resolve`, which validates and canonicalises it.

So normal barcode traffic never touches the backend's public IP. The backend's
`GET /api/v1/products/barcode/{barcode}` still exists for compatibility and ops
tools, but the app does not use it for scans.

`Product.dataOrigin` records which half of that succeeded:
`ProductDataOrigin.Canonical` (the backend canonicalised it) versus
`DeviceFallback` (canonicalisation was unreachable and raw OFF data is standing
in). A `DeviceFallback` product is a stand-in for an outage, so it must not be
treated as final — otherwise one outage pins a product to raw OFF data for the
life of the install.

Room (`GraphDatabase`, expect/actual per platform) keeps a local snapshot so
products already on the graph stay available offline. Bundled `recipes.json` is
temporary until the recipe API lands. There is no simulated scan flow.

### Backend — hexagonal, and every dependency is optional

Six modules, dependencies pointing inward: `domain` depends on nothing;
`application` → `domain`; `infrastructure` → `application`, `domain`
(+ `runtimeOnly(:migrations)`); `api` → all three; `app` → everything.
`domain` and `application` therefore know nothing about Ktor, Postgres, or
Firebase — the adapters all live in `infrastructure`.

DI is **plain constructor wiring in `Application.appModule(authVerifier)`**
(`app/src/main/kotlin/com/dishlab/backend/App.kt`) — no Metro, no Koin. That one
function is the composition root; adding a service means editing it.

The pattern that runs through the whole file: **each backing service is absent-safe.**

| Missing | Falls back to |
|---|---|
| `DATABASE_URL` | `InMemory*Repository` for users, ingredients, recipes, products, taxonomy |
| `RECIPE_CATALOG_DB` (SQLite) | `recipeCatalogService` stays `null`, the routes degrade |
| `OPENROUTER_API_KEY` or a per-feature model var | `NoOp*Provider` / `null` validator, catalog-based fallbacks |

That is why `./gradlew test` needs no Docker and no credentials. Keep new
integrations on the same contract — a required dependency breaks the test suite
for everyone.

Auth in tests: `TestModule.kt` exposes `Application.testModule()`, which is
`appModule(DevFirebaseAuthVerifier())`. That verifier accepts `Bearer :<uid>`
tokens and skips `FirebaseInitializer.init()`. Acceptance tests live in
`app/src/test` and are named by delivery phase — `Phase5PantryAcceptanceTest`,
`RecipeCatalogAcceptanceTest` — not by class under test; unit tests in the other
modules do follow the `<Subject>Test` convention.

Both ends set `JsonNamingStrategy.SnakeCase` — the server in `appModule`, the
client in `core/…/network/serialization/JsonFactory.kt`. So a `val imageUrl`
goes over the wire as `image_url` with no annotation, and hand-adding
`@SerialName("imageUrl")` to "fix" a mismatch breaks the contract instead.

## Traps

- **`com.d1onyx.*` vs `com.d1onix.dishlab.*`** — one letter apart and easy to
  mistype. `d1onyx` is the vendored template code (`core`, `navigation`);
  `d1onix` is DishLab's own.
- **`core/` and `navigation/` are vendored byte-for-byte** from
  `~/work/templates`, so updating them is a plain folder copy. Do not patch them
  locally — fix the template and re-copy, or the next update silently reverts it.
- **`androidResources { enable = true }`** is required in every module holding
  `composeResources`, otherwise the strings never reach the APK and lookups fail
  only at runtime.
- **`compileSdk` must name the minor API level.** API 37 ships only as
  `android-37.0`; the bare number builds from the CLI but breaks Android Studio
  sync, which then leaves the module without an SDK and kills the preview panel.
- **Each module needs its own `compose.resources { packageOfResClass = … }`**,
  or the generated `Res` classes collide.
- `androidRuntimeClasspath(libs.compose.uiTooling)` is what the preview panel
  actually loads — separate from the `androidMain` compile dependency.
- **`git add -A` is unsafe in this working tree.** `androidApp/google-services.json`,
  `backend/`, `samples/`, and the ~1 GB `FoodData_Central_csv_2026-04-30/` are all
  untracked *and* absent from `.gitignore` — 3.1 GB and 968 MB respectively for
  the two data trees (`backend/data/` holds the SQLite recipe catalog and the
  Postgres volume). Stage paths explicitly.
  `google-services.json` is not optional for the build — `androidApp` applies the
  `googleServices` plugin, so the Android build fails without it.
- **`backend/src/main/kotlin/` is dead scaffolding** (`main.kt`, `Routing.kt`,
  `Database.kt`, …). `backend/settings.gradle.kts` includes only the six modules
  and the root project never applies `kotlin.jvm`, so nothing there compiles.
  Real entry point: `backend/app/src/main/kotlin/com/dishlab/backend/App.kt`.
- Backend and root builds have **separate wrappers on different Gradle versions**
  (9.5.0 vs 9.4.1) and separate version catalogs. `backend/` also pulls
  `ktorLibs` from the published `io.ktor:ktor-version-catalog`, so Ktor versions
  are not in its `libs.versions.toml`.

## Requirements

| | |
|---|---|
| Kotlin | 2.4.10 — Metro is a compiler plugin, the versions move together |
| AGP / Gradle | 9.3.1 / 9.5.0 |
| compileSdk / minSdk / targetSdk | 37.0 / 24 / 36 |
| Targets | `androidTarget`, `iosArm64`, `iosSimulatorArm64` |
| Backend | separate build — JDK 21 toolchain, own wrapper on Gradle 9.4.1, Ktor 3.5 via `ktorLibs` |
