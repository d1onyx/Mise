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
            }
        }
        repository = SqliteRecipeCatalogRepository(dbFile)
    }

    @AfterTest
    fun tearDown() {
        Files.deleteIfExists(dbFile)
    }

    @Test
    fun `multi-tag pantry match scores and orders by match percent without a group or exact filter`() {
        val page = repository.findByPantryIngredients(
            firebaseUid = "anonymous",
            ingredientNames = listOf("en:flour", "en:water", "en:egg"),
            category = null,
            tags = emptyList(),
            strictTags = false,
            page = 1,
            pageSize = 10,
            partialMatchOnly = false,
            exactMatch = false,
            exactProductGroups = emptyList(),
        )

        // Inactive recipe 5 never appears even though its ingredients are a full match.
        // Recipe 6 (unrelated quantity-normalization fixture, see below) is active with its
        // own ingredients, so it surfaces here too — with zero overlap against this pantry.
        // Recipe 4 (zero recorded ingredients) sorts dead last, after recipe 6 (real
        // ingredients, just none matched) — a recipe with no ingredient data at all is a worse
        // result than one that simply doesn't match this pantry. (Pre-t-98, this comparison used
        // a `totals` CTE column also literally named `total_count`; ORDER BY's bare `total_count`
        // resolved to that raw joined column — NULL for recipe 4 — instead of the SELECT list's
        // COALESCE(...,0) alias, so `CASE WHEN total_count = 0` was UNKNOWN, not TRUE, for recipe
        // 4 specifically. That accidental short-circuit is what put it ahead of recipe 6, not the
        // documented "zero-ingredient recipes sort last" intent — t-98's r.ingredient_count column
        // has no such name collision, so it now sorts by what the CASE expression actually says.)
        assertEquals(listOf(1L, 2L, 3L, 6L, 4L), page.items.map { it.recipe.id })
        assertEquals(5, page.total)

        val byId = page.items.associateBy { it.recipe.id }
        assertEquals(3, byId.getValue(1L).matchedCount)
        assertEquals(3, byId.getValue(1L).totalIngredients)
        assertEquals(1, byId.getValue(2L).matchedCount)
        assertEquals(2, byId.getValue(2L).totalIngredients)
        assertEquals(0, byId.getValue(3L).matchedCount)
        assertEquals(2, byId.getValue(3L).totalIngredients)
        assertEquals(0, byId.getValue(4L).matchedCount)
        assertEquals(0, byId.getValue(4L).totalIngredients)
        assertEquals(0, byId.getValue(6L).matchedCount)
        assertEquals(4, byId.getValue(6L).totalIngredients)
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
