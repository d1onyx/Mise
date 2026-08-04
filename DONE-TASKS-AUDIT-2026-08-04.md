# Audit of closed tasks — 2026-08-04

Scope: the eight tasks explicitly named in t-41 (`t-3`, `t-4`, `t-9`, `t-11`,
`t-12`, `t-13`, `t-14`, `t-27`). This is a verification of current `master`
at `82fcfcd`, not a review of commit messages. The planning tasks `t-0`–`t-2`
are excluded by t-41.

| Task | DoD from task body | Actual state | Verdict | Evidence |
| --- | --- | --- | --- | --- |
| t-3 | Active Google Play account; Play Console opens. | This is an external founder-owned state. There is no repository artefact or automated test that can establish the account's current state. | Partially completed — unverified | `herdr task get t-3`; no code/test can prove this external DoD. Founder must confirm directly in Play Console. |
| t-4 | A file in the repository listing EANs. | No tracked EAN/barcode list exists in the current tree or anywhere in Git history. | **Not completed** | `git ls-files` and `git log --all --name-only --pretty=format: | rg -i '(^|/)(.*ean|.*barcode).*\\.(md|txt|csv|json)$'` return no EAN-list artefact. |
| t-9 | Clean status; safe staging; history report; required ignore entries; Firebase example and README instruction. | Required ignore rules, example file, and README instruction exist. A reproducible history-report artefact is absent; `git add -A` was not run because repository rules prohibit it. | Partially completed | [`.gitignore`](.gitignore) lines 22–25; [`README.md`](README.md) lines 56–60; `git check-ignore -v` confirms all four required patterns. No tracked history report found. |
| t-11 | Acceptance test: each of the three core routes returns 200 without `Authorization`. | Routes contain the anonymous-user implementation, but the supplied acceptance test is red: requests to the core routes return 404 rather than 200. | **Not completed** | [`RecipeCatalogAcceptanceTest`](backend/app/src/test/kotlin/com/dishlab/backend/RecipeCatalogAcceptanceTest.kt) lines 22–39; `cd backend && ./gradlew :app:test --tests com.dishlab.backend.RecipeCatalogAcceptanceTest` fails (`expected 200 OK but was 404 Not Found`). |
| t-12 | Exceeding the IP limit on the three public routes returns 429; test is green. | Rate-limit code exists, but its public-route acceptance test is red because the initial requests return 404 rather than 200. Therefore the DoD's required green test is not met. | **Not completed** | [`RecipeCatalogAcceptanceTest`](backend/app/src/test/kotlin/com/dishlab/backend/RecipeCatalogAcceptanceTest.kt) lines 43–57; same focused Gradle run fails. |
| t-13 | Fresh install opens Scan or Graph, with no login. Login remains optional and screens remain. | The nav host starts at `ScanRoute`. Auth and onboarding routes still exist, and Profile click sends an unauthenticated user to Auth; they are not start destinations. This is case **(b)** from t-41. | Completed | [`AppNavHost.kt`](shared/src/commonMain/kotlin/com/d1onix/dishlab/navigation/AppNavHost.kt) line 63; [`HomeViewModel.kt`](feature/home/src/commonMain/kotlin/com/d1onix/dishlab/feature/home/presentation/HomeViewModel.kt) lines 58–73. `./gradlew allTests` succeeds. |
| t-14 | A scanned product has non-empty canonical tags that survive app restart. | Backend DTO carries tags to `Product`; the durable cache test creates a new cache from the same storage and retains both tags. | Completed | [`CatalogRepositories.kt`](data/src/commonMain/kotlin/com/d1onix/dishlab/data/catalog/CatalogRepositories.kt) lines 237–279; `BackendProductRepositoryTest.canonical tags survive reconstructing the durable product cache`; `./gradlew allTests` succeeds. |
| t-27 | No `body` / DoD was supplied. | A task without a measurable DoD cannot be audited against a concrete intended result. | Partially completed — unverified | `herdr task get t-27` returns no `body`; no acceptance criterion exists to test. |

## Not completed — recommend PM re-open

- **t-4** — add the repository EAN-list artefact required by its DoD.
- **t-11** — restore the three public core routes so the no-Authorization acceptance test returns 200.
- **t-12** — restore public-route reachability and make the rate-limit acceptance test green with 429 after the limit.

## Notes requiring product/process follow-up

- **t-13 is not a failed task.** The owner seeing an account screen is consistent with optional login. Any decision to hide or relocate that entry point is a scope decision for `ceo-advisor`, not a repair to t-13.
- **t-3** needs founder confirmation in Play Console; it cannot be proven from source code.
- **t-9** needs a durable, reproducible history report if that portion of its DoD is still required.
- **t-27** needs a measurable DoD before its status can be trusted.

## Test runs

- `export JAVA_HOME=/home/denis/work/IDE/android-studio/jbr && ./gradlew allTests` — **BUILD SUCCESSFUL**.
- `cd backend && ./gradlew test` — **BUILD FAILED**: three `IngredientNameCatalogTest` failures (`NoSuchElementException` at line 59).
- `cd backend && ./gradlew :app:test --tests com.dishlab.backend.RecipeCatalogAcceptanceTest` — **BUILD FAILED**: all five tests fail; the route checks report `expected 200 OK but was 404 Not Found`.
