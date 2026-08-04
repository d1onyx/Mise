# Device entry-point map

Physical-device run: 2026-08-04, `37705998` (Xiaomi M2101K6G). Every row is
derived from a captured Android UI hierarchy, not from the source code.

| Screen / capability | Reachable in this run? | Entry path on device | Exact entry element from hierarchy | Resulting hierarchy evidence |
| --- | --- | --- | --- | --- |
| Combination graph | Yes | Scanner → `Enter barcode manually` → enter `3017620422003` → `Look up barcode` → product result → add product | `Add to combination graph` | `t46-graph.xml`: heading `Combination graph` |
| Recipes | Yes | Combination graph → recipe search | `Find recipes (1)` | `t46-recipe-results.xml`: heading `Recipes` |
| Recipe filters | Yes | Combination graph → `Find recipes (1)` | `Find recipes (1)`; target contains `Difficulty`, `Category`, `Time` | `t46-recipe-results.xml` |
| Saved recipes | Yes | Combination graph → saved collection | `content-desc="Saved recipes"` | `t46-saved.xml`: heading `Saved` |
| Account / profile | Yes | Combination graph → profile control | visible text `AK` (bottom-right control) | `t46-profile.xml`: heading `Profile & settings`, user `Alex Kim` |
| Account sign-in | Yes | Home → any tested visible card (`Compare`, `Recipes`, or `?`) | visible text `Compare`, `Recipes`, or `?` | Each led to `t46-compare.xml` / `t46-recipes.xml` / `t46-help.xml`: heading `Your account` |
| Comparison | No — entry is misrouted | Home was reached through Scanner's not-found result → `back to home`; then the `Compare` tile was tapped | `Compare` (tile subtitle `Up to 5 products`) | `t46-home.xml` shows the tile; `t46-compare.xml` shows `Your account`, not a comparison screen |
| Cooking | No reachable entry in the tested state | The only reached recipe list was opened from the graph | `Find recipes (1)` | `t46-recipe-results.xml` renders `No recipes match those filters.` and no recipe item or cooking action |
| History | No reachable entry in the tested state | Inspected the reachable Scanner, Home, product result, graph, recipe list, Saved, and Profile screens | No `History` text or content description appeared in any captured hierarchy | Captures listed above; Profile ends at `Sign out` in `t46-profile-lower.xml` |

## Route notes relevant to t-22 and t-39

- Home is not the start screen. It is reached from the Scanner's not-found
  result with `back to home` (`t46-lookup-result.xml`).
- The reliable graph entry for this build is product result → `Add to
  combination graph`; the graph's `Find recipes (1)` is the reliable recipe
  and filter entry.
- The Home `Compare` and `Recipes` tiles did not lead to their named
  destinations on this physical device; both led to `Your account`.
