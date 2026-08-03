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

## Git Workflow

Several agents work here at once, each in its own worktree. **`master` is the
only branch on the remote** — feature branches stay local, and their commits
reach GitHub through the merge, not through a push.

**One task, one branch, one worktree.**

```bash
git worktree add /tmp/dishlab-t13 -b feat/t-13-remove-startup-auth
```

Name is `<type>/t-<id>-<slug>`, where `t-<id>` is the Herdr task ID. Types:
`feat`, `fix`, `chore`, `docs`. The branch lives exactly as long as the task.
A worktree is not optional — a checkout is global to the clone, so two agents
cannot sit on different branches in the same directory.

**Commits** follow Conventional Commits, with the task as a trailer:

```
feat(scanner): add torch toggle to the viewfinder

Task: t-13
```

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

**Only the `merger` role merges.** Developer agents stop after committing. They
do not merge, do not push, do not touch `master`. The merge is always:

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

**Never run `git add -A`.** Stage explicit paths. This tree carries roughly 4 GB
of untracked data (`FoodData_Central_csv_2026-04-30/`, `backend/data/`) plus
`androidApp/google-services.json`.

**Do not edit the planning documents** — `PRD-MVP.md`, `MVP-PLAN.md`,
`DECISIONS-MVP.md`. They belong to the owner and the `product-manager` role.

**Never** `git push --force`, `git reset --hard`, or rebase a pushed branch
unless explicitly asked.

Releases are **tags** on `master` (`v0.1.0`, SemVer) plus a GitHub Release
carrying the APK. There is no long-lived release branch.

## Security & Configuration Tips

Do not commit local SDK paths, secrets, generated build outputs, or machine-specific IDE files. `androidApp/google-services.json` and `backend/.env` hold real credentials and stay out of the history; the build reads them from disk, so a fresh clone needs them supplied locally.
