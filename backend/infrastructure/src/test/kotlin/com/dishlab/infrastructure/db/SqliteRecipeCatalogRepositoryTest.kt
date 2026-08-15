package com.dishlab.infrastructure.db

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.io.path.absolutePathString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqliteRecipeCatalogRepositoryTest {
    private val dbFile = Files.createTempFile("recipe-catalog-test", ".db")
    private lateinit var repository: SqliteRecipeCatalogRepository

    @BeforeTest
    fun setUp() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePathString()}").use { conn ->
            conn.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE recipes (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        author_name TEXT,
                        cook_time TEXT,
                        prep_time TEXT,
                        total_time TEXT,
                        description TEXT,
                        images TEXT,
                        category TEXT,
                        keywords TEXT,
                        aggregated_rating REAL,
                        calories REAL,
                        fat_content REAL,
                        saturated_fat_content REAL,
                        cholesterol_content REAL,
                        sodium_content REAL,
                        carbohydrate_content REAL,
                        fiber_content REAL,
                        sugar_content REAL,
                        protein_content REAL,
                        instructions TEXT,
                        instruction_times_seconds TEXT,
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE ingredients (
                        id INTEGER PRIMARY KEY,
                        canonical_name TEXT NOT NULL UNIQUE
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE ingredient_aliases (
                        normalized_alias TEXT PRIMARY KEY,
                        original_alias TEXT NOT NULL,
                        ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
                        source TEXT NOT NULL,
                        confidence REAL NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    """
                    CREATE TABLE recipe_ingredients (
                        recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
                        position INTEGER NOT NULL,
                        ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
                        original_text TEXT NOT NULL,
                        quantity TEXT,
                        PRIMARY KEY (recipe_id, position)
                    )
                    """.trimIndent(),
                )
                statement.execute("CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients(ingredient_id, recipe_id)")

                // Recipes span the buckets a multi-tag pantry-match must rank correctly: a full
                // match, a partial match, a recipe with no overlap at all, and a recipe with zero
                // recorded ingredients (the total_count = 0 ordering edge case).
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (1, 'Full Match Pancakes', 1)")
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (2, 'Partial Match Bread', 1)")
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (3, 'No Match Cake', 1)")
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (4, 'Empty Ingredients Mystery', 1)")
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (5, 'Inactive Full Match', 0)")
                statement.execute("INSERT INTO recipes (id, name, is_active) VALUES (6, 'Quantity Normalization Muffins', 1)")

                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (1, 'flour')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (2, 'water')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (3, 'egg')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (4, 'sugar')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (5, 'cocoa')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (6, 'quantity-test-flour')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (7, 'quantity-test-egg')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (8, 'quantity-test-water')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (9, 'quantity-test-cornstarch')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (10, 'quantity-test-butter')")
                // t-102: a genuine synonym recorded only as an alias, never as sugar's own
                // canonical_name — "granulated sugar" canonicalizes to itself, not to "sugar".
                statement.execute(
                    "INSERT INTO ingredient_aliases (normalized_alias, original_alias, ingredient_id, source, confidence) " +
                        "VALUES ('granulated sugar', 'granulated sugar', 4, 'test', 1.0)",
                )

                fun link(recipeId: Int, position: Int, ingredientId: Int) {
                    statement.execute(
                        "INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, original_text) " +
                            "VALUES ($recipeId, $position, $ingredientId, 'x')",
                    )
                }
                // recipe 1: flour + water + egg -> 3/3 matched against the pantry below
                link(1, 1, 1); link(1, 2, 2); link(1, 3, 3)
                // recipe 2: flour + sugar -> 1/2 matched
                link(2, 1, 1); link(2, 2, 4)
                // recipe 3: sugar + cocoa -> 0/2 matched
                link(3, 1, 4); link(3, 2, 5)
                // recipe 4: no recipe_ingredients rows at all
                // recipe 5: same ingredients as recipe 1, but inactive -> excluded entirely
                link(5, 1, 1); link(5, 2, 2); link(5, 3, 3)

                // recipe 6: quantities as ingestion actually stores them (t-91) — a bare
                // fraction/number in `quantity`, the unit only surviving in `original_text`.
                fun linkWithQuantity(position: Int, ingredientId: Int, quantity: String, originalText: String) {
                    statement.execute(
                        "INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, original_text, quantity) " +
                            "VALUES (6, $position, $ingredientId, '$originalText', '$quantity')",
                    )
                }
                linkWithQuantity(1, 6, "0.5", "0.5 cup flour")
                linkWithQuantity(2, 7, "2", "2 eggs")
                linkWithQuantity(3, 8, "", "water to taste")
                linkWithQuantity(4, 9, "1", "1 T cornstarch")
                // t-105: raw floating-point division result from the ingest pipeline, standing
                // in for what was originally "2/3 cup" in the source recipe.
                linkWithQuantity(5, 10, "0.66666668653488", "0.66666668653488 cup butter")
            }
        }
        repository = SqliteRecipeCatalogRepository(dbFile)
    }

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(dbFile)
    }

    @Test
    fun `multi-tag pantry match excludes zero-match recipes by default and orders by match percent`() {
        val page = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = listOf("en:flour", "en:water", "en:egg"),
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
            partialMatchOnly = false,
            exactMatch = false,
            exactProductGroups = emptyList(),
        )

        // t-102: recipes with NO overlap against the pantry no longer surface at all with no
        // flags set — "at least one selected product" is now the unconditional default (it used
        // to require partialMatchOnly=true; see the comment on that branch in
        // SqliteRecipeCatalogRepository.findByPantryIngredients). Recipe 3 (sugar+cocoa, 0/2
        // matched), recipe 4 (no ingredients recorded), and recipe 6 (unrelated
        // quantity-normalization fixture) all drop out; inactive recipe 5 was already excluded
        // regardless. Only recipes 1 (full match) and 2 (partial match) remain.
        assertEquals(listOf(1L, 2L), page.items.map { it.recipe.id })
        assertEquals(2, page.total)

        val byId = page.items.associateBy { it.recipe.id }
        assertEquals(3, byId.getValue(1L).matchedCount)
        assertEquals(3, byId.getValue(1L).totalIngredients)
        assertEquals(1, byId.getValue(2L).matchedCount)
        assertEquals(2, byId.getValue(2L).totalIngredients)
    }

    @Test
    fun `pantry match flags matched ingredients individually, not just the recipe-level count`() {
        // Only flour + water from recipe 1's flour+water+egg — a partial, per-ingredient match.
        val page = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = listOf("en:flour", "en:water"),
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
            partialMatchOnly = false,
            exactMatch = false,
            exactProductGroups = emptyList(),
        )

        val recipe1 = page.items.single { it.recipe.id == 1L }.recipe
        assertEquals(3, recipe1.ingredients.size)
        assertEquals(2, recipe1.ingredients.count { it.matched })
        assertEquals(1, recipe1.ingredients.count { !it.matched })
    }

    @Test
    fun `recipe detail flags ingredients matched against the caller-supplied products`() {
        val withProducts = requireNotNull(
            repository.findById(firebaseUid = "anonymous", recipeId = 1, ingredientNames = listOf("en:flour", "en:water")),
        )
        assertEquals(2, withProducts.ingredients.count { it.matched })
        assertEquals(1, withProducts.ingredients.count { !it.matched })

        // No products supplied — same default as before this field existed: nothing highlighted.
        val withoutProducts = requireNotNull(repository.findById(firebaseUid = "anonymous", recipeId = 1))
        assertTrue(withoutProducts.ingredients.none { it.matched })
    }

    @Test
    fun `pantry match resolves a genuine synonym recorded only as an ingredient alias`() {
        // "granulated sugar" canonicalizes to itself (no plural/descriptor to strip) — it only
        // resolves to sugar's ingredient row via the ingredient_aliases table t-102 wired in.
        val page = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = listOf("granulated sugar"),
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
            partialMatchOnly = false,
            exactMatch = false,
            exactProductGroups = emptyList(),
        )

        // Recipes 2 and 3 both contain sugar; nothing else does.
        assertEquals(setOf(2L, 3L), page.items.map { it.recipe.id }.toSet())
        assertTrue(page.items.all { it.matchedCount == 1 })
    }

    @Test
    fun `search combines multi-select filter groups as OR within a group and AND across groups`() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePathString()}").use { conn ->
            conn.createStatement().use { statement ->
                statement.execute(
                    "UPDATE recipes SET category = 'Dinner', " +
                        "keywords = 'c(\"Dinner\", \"Mexican\", \"oven\", \"bake\")' WHERE id = 1",
                )
                statement.execute(
                    "UPDATE recipes SET category = 'Dessert', " +
                        "keywords = 'c(\"Dessert\", \"Thai\", \"wok\", \"stir-fry\")' WHERE id = 2",
                )
                statement.execute("UPDATE recipes SET category = 'Breakfast' WHERE id = 3")
            }
        }

        // Two categories OR together: both Dinner (1) and Dessert (2) recipes come back.
        val byCategory = repository.search(
            firebaseUid = "anonymous", query = null,
            categories = listOf("Dinner", "Dessert"), ingredient = null, page = 1, pageSize = 10,
        )
        assertEquals(setOf(1L, 2L), byCategory.items.map { it.id }.toSet())

        // category AND cuisine: recipe 1 is Dinner but not Thai, recipe 2 is Thai but not
        // Dinner — no recipe satisfies both groups at once.
        val crossGroup = repository.search(
            firebaseUid = "anonymous", query = null,
            categories = listOf("Dinner"), cuisines = listOf("Thai"), ingredient = null, page = 1, pageSize = 10,
        )
        assertTrue(crossGroup.items.isEmpty(), crossGroup.items.toString())

        // equipment filter alone, case-insensitive.
        val byEquipment = repository.search(
            firebaseUid = "anonymous", query = null,
            equipment = listOf("WOK"), ingredient = null, page = 1, pageSize = 10,
        )
        assertEquals(listOf(2L), byEquipment.items.map { it.id })
    }

    @Test
    fun `pantry-match applies category and technique filter groups alongside ingredient matching`() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePathString()}").use { conn ->
            conn.createStatement().use { statement ->
                statement.execute(
                    "UPDATE recipes SET category = 'Dinner', keywords = 'c(\"bake\")' WHERE id = 1",
                )
                statement.execute(
                    "UPDATE recipes SET category = 'Dessert', keywords = 'c(\"fry\")' WHERE id = 2",
                )
            }
        }

        // Both recipes 1 and 2 have a flour+? overlap with this pantry, but only recipe 1 is
        // in the selected category group.
        val page = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = listOf("en:flour"),
            categories = listOf("Dinner"),
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
            partialMatchOnly = true,
        )
        assertEquals(listOf(1L), page.items.map { it.recipe.id })

        // technique filter alone narrows to recipe 2 regardless of category.
        val byTechnique = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = emptyList(),
            techniques = listOf("fry"),
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
        )
        assertEquals(listOf(2L), byTechnique.items.map { it.recipe.id })
    }

    @Test
    fun `recipe detail attaches an explicit unit to a raw fractional quantity`() {
        val recipe = requireNotNull(repository.findById(firebaseUid = "anonymous", recipeId = 6))
        val byPosition = recipe.ingredients.associateBy { it.name }

        // A bare fraction with a real unit in the source text gets that unit back.
        assertEquals("0.5 cup", byPosition.getValue("0.5 cup flour").quantity)
        // A bare count with no recognisable unit still gets a meaningful one, not a raw number.
        assertEquals("2 pcs", byPosition.getValue("2 eggs").quantity)
        // An empty quantity (nothing to normalize) stays empty rather than gaining a fake unit.
        assertEquals("", byPosition.getValue("water to taste").quantity)
        // The dataset's own "T" = tablespoon / "t" = teaspoon convention resolves case-sensitively.
        assertEquals("1 tbsp", byPosition.getValue("1 T cornstarch").quantity)
        // t-105: a raw floating-point division result ("2/3 cup" stored as .66666668653488)
        // rounds to a normal 2-decimal quantity instead of leaking ingest internals to the client.
        assertEquals("0.67 cup", byPosition.getValue("0.66666668653488 cup butter").quantity)
    }

    @Test
    fun `getFilters groups categories cuisines equipment and techniques, and refreshes only after the catalog changes`() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePathString()}").use { conn ->
            conn.createStatement().use { statement ->
                statement.execute(
                    "UPDATE recipes SET category = 'Dinner', " +
                        "keywords = 'c(\"Dinner\", \"Mexican\", \"oven\", \"bake\")' WHERE id = 1",
                )
            }
        }

        val first = repository.getFilters()
        assertEquals(listOf("Dinner"), first.categories)
        assertEquals(listOf("Mexican"), first.cuisines)
        assertEquals(listOf("oven"), first.equipment)
        assertEquals(listOf("bake"), first.techniques)

        // Second call with nothing changed must be served from cache: same instance, no rescan.
        assertTrue(first === repository.getFilters())

        // A write through a completely separate connection still bumps PRAGMA data_version, which
        // is the whole point of keying the cache on it instead of an in-process write counter.
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePathString()}").use { conn ->
            conn.createStatement().use { statement ->
                statement.execute(
                    "UPDATE recipes SET keywords = 'c(\"Dinner\", \"Thai\", \"wok\", \"stir-fry\")' WHERE id = 2",
                )
            }
        }

        val second = repository.getFilters()
        assertTrue(second !== first)
        assertTrue(second.cuisines.contains("Thai"), second.cuisines.toString())
        assertTrue(second.equipment.contains("wok"), second.equipment.toString())
        assertTrue(second.techniques.contains("stir-fry"), second.techniques.toString())
    }
}
