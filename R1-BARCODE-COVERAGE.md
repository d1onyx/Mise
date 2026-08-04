# R1: Czech barcode coverage spike

Run on 2026-08-04 against Open Food Facts and the local Food.com SQLite
catalogue. The backend ran with `DEV_AUTH=true` and
`RECIPE_CATALOG_DB=/home/denis/work/Projects/DishLab/backend/data/recipe-catalog.db`.

| EAN | OFF product | Canonical tags from `/resolve` | `pantry-match` total | Diagnosis |
| --- | --- | --- | ---: | --- |
| 8590421041536 | Tmavý vícezrnný chléb toustový (Albertovo pekařství, Penam) | `en:bread-flour`, `en:flour` | 20,480 | Tags and catalogue matches present |
| 8590421074572 | Peanut butter (worlds market) | `en:peanut-butter`, `en:butter`, `en:peanut` | 42,197 | Tags and catalogue matches present |
| 20405007 | Pitná voda jemně perlivá (Saguaro) | `en:water` | 25,269 | Tags and catalogue matches present |

Conclusion: **the plan is valid** — all three real shelf barcodes reached
non-empty Food.com recipe matches.

## Reproducible commands

```bash
# Fetch each OFF snapshot, then send its product name/brand/categories to resolve.
curl -fsS https://world.openfoodfacts.org/api/v2/product/8590421041536.json
curl -i -X POST http://localhost:8080/api/v1/products/resolve \
  -H 'Authorization: Bearer :spike' -H 'Content-Type: application/json' \
  -d '{"barcode":"8590421041536","name":"Tmavý vícezrnný chléb toustový","brand":"Albertovo pekařství, Penam","categories":["en:flours","en:breads","en:bread-flours"]}'
curl -i -G http://localhost:8080/api/v1/recipe-catalog/pantry-match \
  -H 'Authorization: Bearer :spike' --data-urlencode ingredient=en:bread-flour \
  --data-urlencode ingredient=en:flour --data-urlencode partialMatchOnly=true
```

The other two rows used the identical sequence with their displayed EAN and
canonical tags. `partialMatchOnly=true` measures recipes matching at least one
canonical tag; each response returned HTTP 200 and the totals in the table.
