package com.dishlab.infrastructure.db

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.io.path.absolutePathString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (1, 'flour')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (2, 'water')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (3, 'egg')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (4, 'sugar')")
                statement.execute("INSERT INTO ingredients (id, canonical_name) VALUES (5, 'cocoa')")

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
        assertEquals(listOf(1L, 2L, 3L, 4L), page.items.map { it.recipe.id })
        assertEquals(4, page.total)

        val byId = page.items.associateBy { it.recipe.id }
        assertEquals(3, byId.getValue(1L).matchedCount)
        assertEquals(3, byId.getValue(1L).totalIngredients)
        assertEquals(1, byId.getValue(2L).matchedCount)
        assertEquals(2, byId.getValue(2L).totalIngredients)
        assertEquals(0, byId.getValue(3L).matchedCount)
        assertEquals(2, byId.getValue(3L).totalIngredients)
        assertEquals(0, byId.getValue(4L).matchedCount)
        assertEquals(0, byId.getValue(4L).totalIngredients)
    }
}
