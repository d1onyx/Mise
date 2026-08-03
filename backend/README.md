# DishLab Backend

Ktor service for identity, product lookup and normalization, ingredient
taxonomy, pantry state, recipes, cooking sessions, and health records. It is an
independent Gradle build and requires JDK 21.

## Structure

| Module | Responsibility |
|---|---|
| `domain` | Domain models, errors, and policies |
| `application` | Use cases, repository contracts, validation, and categorization |
| `infrastructure` | PostgreSQL, SQLite, Firebase, Open Food Facts, and OpenRouter adapters |
| `api` | Ktor DTOs, middleware, and `/api/v1` routes |
| `app` | Dependency wiring and Netty entry point |
| `migrations` | Flyway PostgreSQL migrations |

The imported Food.com catalog lives at `data/recipe-catalog.db` and contains
522,517 recipes with 4,129,433 normalized recipe-ingredient links. The database
is copied for local development but intentionally ignored by Git.

## Run Locally

```bash
cd backend
cp .env.example .env
docker compose up -d postgres
set -a && source .env && set +a
./gradlew flywayMigrate
./gradlew :app:run
```

The API listens on `http://localhost:8080`. Set `DEV_AUTH=true` to use the
development Firebase verifier. Run `./gradlew test` for unit and acceptance
tests, or `docker compose up --build` to run the complete stack.

## Product And Recipe Logic

Product data comes from Open Food Facts. The server validates barcodes,
normalizes product names into English ingredient tags, resolves broader related
tags, and categorizes newly approved tags. OpenRouter integrations are optional;
catalog-based fallbacks remain available without an API key.

Important authenticated endpoints:

```text
GET  /api/v1/products/barcode/{barcode}
POST /api/v1/products/resolve
GET  /api/v1/products/search?q=
GET  /api/v1/products/categories?q=
POST /api/v1/products/normalize
POST /api/v1/products/tags/validate
GET  /api/v1/recipe-catalog?q=&category=&ingredient=&page=&pageSize=
GET  /api/v1/recipe-catalog/{catalog:recipeId}
```

The mobile app fetches OFF v3.6 product data directly from the user's device,
then sends a bounded snapshot to `POST /api/v1/products/resolve` for validation
and canonicalization. This keeps normal barcode traffic off the backend's
public IP. The legacy barcode GET and server-side search remain available for
compatibility and operational tools.

With PostgreSQL configured, resolved products persist in `retail_products` and
append/update revision-aware `product_source_snapshots`. Canonical food identity
is stored separately in `food_concepts`, `food_aliases`, and `food_variants`;
variant facets currently include origin, preparation state, physical form,
carbonation, and preservation. Without PostgreSQL, equivalent in-memory stores
keep local development and acceptance tests operational.

Public user recipes are moderated when published. Unknown recipe ingredients
are validated before entering the shared PostgreSQL ingredient catalog; if AI
validation is unavailable, they remain recipe-local text.

## Configuration

Start from `.env.example`. Keep real Firebase and OpenRouter credentials only in
the ignored `.env` file. `RECIPE_CATALOG_DB` and
`INGREDIENT_CATALOG_JSON` default to files under `backend/data/`.
