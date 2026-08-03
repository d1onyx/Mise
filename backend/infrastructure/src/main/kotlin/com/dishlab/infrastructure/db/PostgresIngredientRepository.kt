package com.dishlab.infrastructure.db

import com.dishlab.application.service.IngredientRepository
import com.dishlab.application.service.IngredientSearchResult
import com.dishlab.domain.model.Ingredient
import com.dishlab.domain.model.IngredientSubstitute
import com.dishlab.domain.model.NutritionFacts
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource

class PostgresIngredientRepository(
    private val dataSource: DataSource,
) : IngredientRepository {

    override fun save(ingredient: Ingredient): Ingredient {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val saved = upsertIngredient(connection, ingredient)
                replaceAllergens(connection, saved)
                replaceNutrition(connection, saved)
                connection.commit()
                return saved
            } catch (error: Exception) {
                connection.rollback()
                throw error
            }
        }
    }

    override fun findById(id: UUID): Ingredient? =
        dataSource.connection.use { connection ->
            findOne(connection, "i.id = ?", id)
        }

    override fun findAll(): List<Ingredient> =
        dataSource.connection.use { connection ->
            queryIngredients(connection, "TRUE", emptyList(), "ORDER BY lower(i.name)")
        }

    override fun findByName(name: String): Ingredient? =
        dataSource.connection.use { connection ->
            findOne(connection, "lower(trim(i.name)) = lower(trim(?))", name.trim())
        }

    override fun search(query: String, page: Int, pageSize: Int): IngredientSearchResult {
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, 100)
        val offset = (safePage - 1) * safePageSize
        val normalized = query.trim()
        dataSource.connection.use { connection ->
            val where = "trim(?) = '' OR lower(i.name) LIKE '%' || lower(trim(?)) || '%'"
            val total = connection.prepareStatement("SELECT count(*) FROM ingredients i WHERE $where").use { statement ->
                statement.setString(1, normalized)
                statement.setString(2, normalized)
                statement.executeQuery().use { result ->
                    result.next()
                    result.getInt(1)
                }
            }
            val items = queryIngredients(
                connection = connection,
                where = where,
                parameters = listOf(normalized, normalized, safePageSize, offset),
                suffix = "ORDER BY lower(i.name) LIMIT ? OFFSET ?",
            )
            return IngredientSearchResult(items, safePage, safePageSize, total)
        }
    }

    override fun findSubstitutes(ingredientId: UUID): List<Pair<Ingredient, IngredientSubstitute>> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT substitute_ingredient_id, reason
                FROM ingredient_substitutes
                WHERE ingredient_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, ingredientId)
                statement.executeQuery().use { result ->
                    return buildList {
                        while (result.next()) {
                            val substituteId = result.getObject("substitute_ingredient_id", UUID::class.java)
                            val substitute = findOne(connection, "i.id = ?", substituteId) ?: continue
                            add(
                                substitute to IngredientSubstitute(
                                    ingredientId = ingredientId,
                                    substituteIngredientId = substituteId,
                                    reason = result.getString("reason"),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun upsertIngredient(connection: Connection, ingredient: Ingredient): Ingredient {
        connection.prepareStatement(
            """
            INSERT INTO ingredients
                (id, name, default_unit, custom_created_by_user_id, barcode, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, now())
            ON CONFLICT ((lower(trim(name)))) DO UPDATE SET
                default_unit = CASE
                    WHEN ingredients.default_unit = '' THEN EXCLUDED.default_unit
                    ELSE ingredients.default_unit
                END,
                barcode = COALESCE(ingredients.barcode, EXCLUDED.barcode),
                updated_at = now()
            RETURNING id, name, default_unit, custom_created_by_user_id, barcode, created_at
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, ingredient.id)
            statement.setString(2, ingredient.name.trim())
            statement.setString(3, ingredient.defaultUnit.trim().lowercase())
            statement.setObject(4, ingredient.customCreatedByUserId)
            statement.setString(5, ingredient.barcode)
            statement.setTimestamp(6, Timestamp.from(ingredient.createdAt))
            statement.executeQuery().use { result ->
                result.next()
                return Ingredient(
                    id = result.getObject("id", UUID::class.java),
                    name = result.getString("name"),
                    defaultUnit = result.getString("default_unit"),
                    allergens = ingredient.allergens,
                    nutritionPer100g = ingredient.nutritionPer100g,
                    barcode = result.getString("barcode"),
                    customCreatedByUserId = result.getObject("custom_created_by_user_id", UUID::class.java),
                    createdAt = result.getTimestamp("created_at").toInstant(),
                )
            }
        }
    }

    private fun replaceAllergens(connection: Connection, ingredient: Ingredient) {
        connection.prepareStatement("DELETE FROM ingredient_allergens WHERE ingredient_id = ?").use {
            it.setObject(1, ingredient.id)
            it.executeUpdate()
        }
        ingredient.allergens.forEach { allergen ->
            connection.prepareStatement(
                "INSERT INTO ingredient_allergens (ingredient_id, allergen_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
            ).use {
                it.setObject(1, ingredient.id)
                it.setString(2, allergen)
                it.executeUpdate()
            }
        }
    }

    private fun replaceNutrition(connection: Connection, ingredient: Ingredient) {
        val nutrition = ingredient.nutritionPer100g
        if (nutrition == null) return
        connection.prepareStatement(
            """
            INSERT INTO ingredient_nutrition (ingredient_id, calories, protein, fat, carbs)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (ingredient_id) DO UPDATE SET
                calories = EXCLUDED.calories,
                protein = EXCLUDED.protein,
                fat = EXCLUDED.fat,
                carbs = EXCLUDED.carbs
            """.trimIndent(),
        ).use {
            it.setObject(1, ingredient.id)
            it.setObject(2, nutrition.calories)
            it.setObject(3, nutrition.protein)
            it.setObject(4, nutrition.fat)
            it.setObject(5, nutrition.carbs)
            it.executeUpdate()
        }
    }

    private fun findOne(connection: Connection, where: String, parameter: Any): Ingredient? =
        queryIngredients(connection, where, listOf(parameter), "LIMIT 1").firstOrNull()

    private fun queryIngredients(
        connection: Connection,
        where: String,
        parameters: List<Any>,
        suffix: String,
    ): List<Ingredient> {
        val sql =
            """
            SELECT i.id, i.name, i.default_unit, i.custom_created_by_user_id, i.barcode, i.created_at,
                   n.calories, n.protein, n.fat, n.carbs,
                   COALESCE(array_agg(a.allergen_code) FILTER (WHERE a.allergen_code IS NOT NULL), '{}') AS allergens
            FROM ingredients i
            LEFT JOIN ingredient_nutrition n ON n.ingredient_id = i.id
            LEFT JOIN ingredient_allergens a ON a.ingredient_id = i.id
            WHERE $where
            GROUP BY i.id, n.ingredient_id
            $suffix
            """.trimIndent()
        connection.prepareStatement(sql).use { statement ->
            parameters.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeQuery().use { result ->
                return buildList {
                    while (result.next()) add(result.toIngredient())
                }
            }
        }
    }

    private fun ResultSet.toIngredient(
        allergens: List<String>? = null,
        nutrition: NutritionFacts? = null,
    ): Ingredient {
        val loadedNutrition = nutrition ?: NutritionFacts(
            calories = getDouble("calories").takeUnless { wasNull() },
            protein = getDouble("protein").takeUnless { wasNull() },
            fat = getDouble("fat").takeUnless { wasNull() },
            carbs = getDouble("carbs").takeUnless { wasNull() },
        ).takeIf { listOf(it.calories, it.protein, it.fat, it.carbs).any { value -> value != null } }
        return Ingredient(
            id = getObject("id", UUID::class.java),
            name = getString("name"),
            defaultUnit = getString("default_unit"),
            allergens = allergens ?: (getArray("allergens")?.array as? Array<*>)?.filterIsInstance<String>().orEmpty(),
            nutritionPer100g = loadedNutrition,
            barcode = getString("barcode"),
            customCreatedByUserId = getObject("custom_created_by_user_id", UUID::class.java),
            createdAt = getTimestamp("created_at").toInstant(),
        )
    }
}
