package com.dishlab.infrastructure.db

import com.dishlab.application.service.CatalogRecipePage
import com.dishlab.application.service.PantryMatchPage
import com.dishlab.application.service.RecipeCatalogRepository
import com.dishlab.domain.model.CatalogNutrition
import com.dishlab.domain.model.CatalogRecipe
import com.dishlab.domain.model.CatalogRecipeIngredient
import com.dishlab.domain.model.CatalogRecipeStep
import com.dishlab.domain.model.PantryMatchedRecipe
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.text.Normalizer
import java.util.Locale

class SqliteRecipeCatalogRepository(databasePath: Path) : RecipeCatalogRepository {
    private val jdbcUrl = "jdbc:sqlite:${databasePath.toAbsolutePath()}"

    init {
        require(databasePath.toFile().isFile) {
            "Recipe catalog database does not exist: ${databasePath.toAbsolutePath()}"
        }
        connection().use { conn ->
            conn.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = 5000")
                statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_recipes_category_name ON recipes(category, name COLLATE NOCASE)",
                )
                runCatching {
                    statement.execute("ALTER TABLE recipes ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
                    statement.execute(
                        "UPDATE recipes SET is_active = 0 WHERE images IS NULL OR trim(images) = '' OR trim(images) IN ('NA', 'NULL', 'character(0)')",
                    )
                }
                statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_recipes_active_rating ON recipes(is_active, aggregated_rating DESC)",
                )
            }
        }
    }

    override fun search(
        firebaseUid: String,
        query: String?,
        category: String?,
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage = connection().use { conn ->
        val ingredientId = ingredient?.let { findIngredientId(conn, it) }
        if (ingredient != null && ingredientId == null) {
            return@use CatalogRecipePage(emptyList(), page, pageSize, 0)
        }

        val conditions = mutableListOf("r.is_active = 1")
        val parameters = mutableListOf<Any>()
        if (query != null) {
            conditions += "(r.name LIKE ? ESCAPE '!' COLLATE NOCASE OR r.category LIKE ? ESCAPE '!' COLLATE NOCASE)"
            val pattern = "%${escapeLike(query)}%"
            parameters += pattern
            parameters += pattern
        }
        if (category != null) {
            conditions += "(r.category = ? COLLATE NOCASE OR r.keywords LIKE ? ESCAPE '!' COLLATE NOCASE)"
            parameters += category
            parameters += "%${escapeLike(category)}%"
        }
        if (ingredientId != null) {
            conditions += "EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id = r.id AND ri.ingredient_id = ?)"
            parameters += ingredientId
        }
        val where = conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ", prefix = "WHERE ").orEmpty()

        val total = conn.prepareStatement("SELECT COUNT(*) FROM recipes r $where").use { statement ->
            statement.bind(parameters)
            statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
        }
        val offset = (page - 1) * pageSize
        val sql =
            """
            SELECT r.*
            FROM recipes r
            $where
            ORDER BY r.aggregated_rating IS NULL, r.aggregated_rating DESC, r.id
            LIMIT ? OFFSET ?
            """.trimIndent()
        val items = conn.prepareStatement(sql).use { statement ->
            statement.bind(parameters, startIndex = 1)
            statement.setInt(parameters.size + 1, pageSize)
            statement.setInt(parameters.size + 2, offset)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(rows.toRecipe())
                }
            }
        }
        CatalogRecipePage(items, page, pageSize, total)
    }

    override fun findById(firebaseUid: String, recipeId: Long): CatalogRecipe? = connection().use { conn ->
        val loaded = conn.prepareStatement(
            """
            SELECT r.*
            FROM recipes r
            WHERE r.id = ? AND r.is_active = 1
            """.trimIndent(),
        ).use { statement ->
            statement.setLong(1, recipeId)
            statement.executeQuery().use { rows ->
                if (rows.next()) {
                    LoadedRecipe(
                        recipe = rows.toRecipe(),
                        instructions = rows.getString("instructions"),
                        instructionTimes = rows.getString("instruction_times_seconds"),
                    )
                } else {
                    null
                }
            }
        } ?: return@use null

        loaded.recipe.copy(
            ingredients = loadIngredients(conn, recipeId),
            steps = loadSteps(loaded.instructions, loaded.instructionTimes),
        )
    }

    override fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        category: String?,
        tags: List<String>,
        strictTags: Boolean,
        page: Int,
        pageSize: Int,
        partialMatchOnly: Boolean,
        exactMatch: Boolean,
        exactProductGroups: List<List<String>>,
    ): PantryMatchPage = connection().use { conn ->
        val normalizedNames = ingredientNames.map { IngredientNameNormalizer.canonicalize(it) }

        // Build WHERE conditions for category / tags filtering
        val conditions = mutableListOf("r.is_active = 1")
        val condParams = mutableListOf<Any>()
        if (category != null) {
            conditions += "(LOWER(r.category) = LOWER(?) OR r.keywords LIKE ? ESCAPE '!' COLLATE NOCASE)"
            condParams += category
            condParams += "%${escapeLike(category)}%"
        }
        if (tags.isNotEmpty()) {
            if (strictTags) {
                // All selected tags must appear somewhere in keywords / category
                tags.forEach { tag ->
                    conditions += "(r.keywords LIKE ? ESCAPE '!' COLLATE NOCASE OR LOWER(r.category) = LOWER(?))"
                    condParams += "%${escapeLike(tag)}%"
                    condParams += tag
                }
            } else {
                val tagOr = tags.joinToString(" OR ") {
                    "(r.keywords LIKE ? ESCAPE '!' COLLATE NOCASE OR LOWER(r.category) = LOWER(?))"
                }
                conditions += "($tagOr)"
                tags.forEach { tag ->
                    condParams += "%${escapeLike(tag)}%"
                    condParams += tag
                }
            }
        }
        // Exact match at PRODUCT granularity: each group is one selected product's synonym tags
        // (e.g. flour = [flour, wheat flour]). The recipe must contain at least one tag from EVERY
        // group. This avoids over-constraining when a single pantry product carries several tags
        // (flour+wheat-flour, water+mineral-water…) which no recipe lists all of. Existence-based,
        // so duplicate ingredient rows are tolerated. Results still span 100%→low coverage because
        // extra non-selected ingredients lower match_percent.
        val canonicalGroups = exactProductGroups
            .map { group -> group.map { IngredientNameNormalizer.canonicalize(it) }.filter(String::isNotBlank).distinct() }
            .filter { it.isNotEmpty() }
        if (canonicalGroups.isNotEmpty()) {
            val groupConditions = canonicalGroups.map { group ->
                val placeholders = group.joinToString(",") { "?" }
                condParams.addAll(group)
                "EXISTS(SELECT 1 FROM recipe_ingredients ri_g" +
                    " JOIN ingredients i_g ON i_g.id = ri_g.ingredient_id" +
                    " WHERE ri_g.recipe_id = r.id AND LOWER(i_g.canonical_name) IN ($placeholders))"
            }
            val allGroupsMatch = groupConditions.joinToString(" AND ")
            val anyPlainIngredientMatches = "EXISTS(SELECT 1 FROM recipe_ingredients ri_p" +
                " JOIN ingredients i_p ON i_p.id = ri_p.ingredient_id" +
                " WHERE ri_p.recipe_id = r.id" +
                " AND EXISTS(SELECT 1 FROM pantry_names p WHERE LOWER(i_p.canonical_name) = LOWER(p.norm)))"
            // Graph components are AND clauses; isolated products are OR clauses. `exactMatch`
            // preserves the legacy grouped-exact endpoint, where plain ingredient tags are only
            // used for scoring and every group must match.
            conditions += if (exactMatch || normalizedNames.isEmpty()) {
                "($allGroupsMatch)"
            } else {
                "(($allGroupsMatch) OR $anyPlainIngredientMatches)"
            }
        } else if (exactMatch && normalizedNames.isNotEmpty()) {
            // Fallback without group info: every selected tag must be present in the recipe.
            val missingSelectedExists = "EXISTS(SELECT 1 FROM pantry_names p WHERE NOT EXISTS(" +
                "SELECT 1 FROM recipe_ingredients ri_e JOIN ingredients i_e ON i_e.id = ri_e.ingredient_id" +
                " WHERE ri_e.recipe_id = r.id AND LOWER(i_e.canonical_name) = LOWER(p.norm)))"
            conditions += "NOT $missingSelectedExists"
        } else if (partialMatchOnly && normalizedNames.isNotEmpty()) {
            // Non-exact: the recipe contains AT LEAST ONE selected product (full 100% matches
            // included). Uses pantry_names CTE — no extra params needed.
            val matchedSub = "(SELECT COUNT(*) FROM recipe_ingredients ri_m" +
                " JOIN ingredients i_m ON i_m.id = ri_m.ingredient_id" +
                " WHERE ri_m.recipe_id = r.id" +
                " AND EXISTS(SELECT 1 FROM pantry_names p WHERE LOWER(i_m.canonical_name) = LOWER(p.norm)))"
            conditions += "$matchedSub >= 1"
        }
        val where = conditions.takeIf { it.isNotEmpty() }
            ?.joinToString(" AND ", prefix = "WHERE ")
            .orEmpty()

        // Build pantry CTE
        val cteValues = if (normalizedNames.isEmpty()) "SELECT NULL WHERE 0" // empty set
        else normalizedNames.joinToString(", ") { "(?)" }.let { "VALUES $it" }

        // Resolve pantry names to ingredient ids up front so match scoring can use
        // idx_recipe_ingredients_ingredient(ingredient_id, recipe_id) directly. The previous
        // per-recipe correlated EXISTS (recipe_ingredients JOIN ingredients, filtered by a LOWER()
        // comparison against pantry_names) had to run once for every row in the WHERE-filtered
        // candidate set — for the common multi-tag request that set is the whole active catalog
        // (500k+ rows), which is what pushed the endpoint past the client's 10s timeout.
        val matchedIngredientIds: List<Long> = if (normalizedNames.isEmpty()) {
            emptyList()
        } else {
            val placeholders = normalizedNames.joinToString(",") { "?" }
            conn.prepareStatement("SELECT id FROM ingredients WHERE LOWER(canonical_name) IN ($placeholders)").use { stmt ->
                normalizedNames.forEachIndexed { i, name -> stmt.setString(i + 1, name) }
                stmt.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getLong(1)) } }
            }
        }
        val matchedCte = if (matchedIngredientIds.isEmpty()) {
            "SELECT NULL, NULL WHERE 0"
        } else {
            val placeholders = matchedIngredientIds.joinToString(",") { "?" }
            """
            SELECT ri.recipe_id, COUNT(DISTINCT ri.ingredient_id)
            FROM recipe_ingredients ri
            WHERE ri.ingredient_id IN ($placeholders)
            GROUP BY ri.recipe_id
            """.trimIndent()
        }

        val sql = """
            WITH pantry_names(norm) AS ($cteValues),
                 matched(recipe_id, matched_count) AS ($matchedCte),
                 totals(recipe_id, total_count) AS (
                     SELECT recipe_id, COUNT(*) FROM recipe_ingredients GROUP BY recipe_id
                 )
            SELECT r.*,
                   COALESCE(t.total_count, 0) AS total_count,
                   COALESCE(m.matched_count, 0) AS matched_count
            FROM recipes r
            LEFT JOIN matched m ON m.recipe_id = r.id
            LEFT JOIN totals t ON t.recipe_id = r.id
            $where
            ORDER BY
                CASE WHEN total_count = 0 THEN 1 ELSE 0 END,
                CASE WHEN total_count = 0 THEN 0.0 ELSE CAST(matched_count AS FLOAT) / total_count END DESC,
                r.aggregated_rating IS NULL,
                r.aggregated_rating DESC
        """.trimIndent()

        val countSql = """
            WITH pantry_names(norm) AS ($cteValues)
            SELECT COUNT(*) FROM recipes r $where
        """.trimIndent()

        val allParams: List<Any> = normalizedNames + condParams
        val total = conn.prepareStatement(countSql).use { stmt ->
            stmt.bind(allParams)
            stmt.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
        }

        val offset = (page - 1) * pageSize
        val items = conn.prepareStatement("$sql LIMIT ? OFFSET ?").use { stmt ->
            var idx = 1
            normalizedNames.forEach { stmt.setString(idx++, it) }
            matchedIngredientIds.forEach { stmt.setLong(idx++, it) }
            condParams.forEach { stmt.setObject(idx++, it) }
            stmt.setInt(idx++, pageSize)
            stmt.setInt(idx, offset)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            PantryMatchedRecipe(
                                recipe = rs.toRecipe(),
                                matchedCount = rs.getInt("matched_count"),
                                totalIngredients = rs.getInt("total_count"),
                            ),
                        )
                    }
                }
            }
        }
        PantryMatchPage(items, page, pageSize, total)
    }

    override fun getCategories(): List<String> = connection().use { conn ->
        conn.prepareStatement(
            "SELECT DISTINCT category FROM recipes WHERE is_active = 1 AND category IS NOT NULL AND trim(category) != '' ORDER BY category"
        ).use { statement ->
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.getString("category")) }
            }
        }
    }

    private fun connection(): Connection = DriverManager.getConnection(jdbcUrl)

    private fun findIngredientId(conn: Connection, query: String): Long? {
        val normalizedAlias = IngredientNameNormalizer.normalizeAlias(query)
        val canonicalName = IngredientNameNormalizer.canonicalize(query)
        return conn.prepareStatement(
            """
            SELECT DISTINCT i.id
            FROM ingredients i
            LEFT JOIN ingredient_aliases a ON a.ingredient_id = i.id
            WHERE a.normalized_alias = ? OR i.canonical_name = ?
            ORDER BY CASE WHEN a.normalized_alias = ? THEN 0 ELSE 1 END
            LIMIT 1
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, normalizedAlias)
            statement.setString(2, canonicalName)
            statement.setString(3, normalizedAlias)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getLong(1) else null }
        }
    }

    private fun loadIngredients(conn: Connection, recipeId: Long): List<CatalogRecipeIngredient> =
        conn.prepareStatement(
            """
            SELECT ri.original_text, ri.quantity, i.canonical_name
            FROM recipe_ingredients ri
            JOIN ingredients i ON i.id = ri.ingredient_id
            WHERE ri.recipe_id = ?
            ORDER BY ri.position
            """.trimIndent(),
        ).use { statement ->
            statement.setLong(1, recipeId)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) {
                        val canonicalTag = rows.getString("canonical_name").toEnglishIngredientTaxonomyTag()
                        add(
                            CatalogRecipeIngredient(
                                name = rows.getString("original_text"),
                                quantity = rows.getString("quantity"),
                                canonicalTags = listOf(canonicalTag).filter(String::isNotBlank),
                            ),
                        )
                    }
                }
            }
        }

    private fun loadSteps(instructionText: String?, instructionTimes: String?): List<CatalogRecipeStep> {
        val instructions = RVectorParser.parse(instructionText)
        val timers = parseTimers(instructionTimes)
        return instructions.mapIndexed { index, text ->
            CatalogRecipeStep(index + 1, text, timers.getOrNull(index))
        }
    }

    private fun ResultSet.toRecipe(): CatalogRecipe {
        return CatalogRecipe(
            id = getLong("id"),
            title = getString("name"),
            authorName = getString("author_name"),
            cookTime = getString("cook_time"),
            prepTime = getString("prep_time"),
            totalTime = getString("total_time"),
            description = getString("description"),
            images = RVectorParser.parse(getString("images")),
            category = getString("category"),
            tags = RVectorParser.parse(getString("keywords")),
            rating = nullableDouble("aggregated_rating"),
            nutrition = CatalogNutrition(
                calories = nullableDouble("calories"),
                fat = nullableDouble("fat_content"),
                saturatedFat = nullableDouble("saturated_fat_content"),
                cholesterol = nullableDouble("cholesterol_content"),
                sodium = nullableDouble("sodium_content"),
                carbohydrates = nullableDouble("carbohydrate_content"),
                fiber = nullableDouble("fiber_content"),
                sugar = nullableDouble("sugar_content"),
                protein = nullableDouble("protein_content"),
            ),
        )
    }

    private fun ResultSet.nullableDouble(column: String): Double? {
        val value = getDouble(column)
        return value.takeUnless { wasNull() }
    }

    private fun java.sql.PreparedStatement.bind(values: List<Any>, startIndex: Int = 1) {
        values.forEachIndexed { index, value -> setObject(startIndex + index, value) }
    }

    private fun escapeLike(value: String): String =
        value.replace("!", "!!").replace("%", "!%").replace("_", "!_")

    private fun parseTimers(value: String?): List<Int?> {
        if (value.isNullOrBlank()) return emptyList()
        return value.removePrefix("(").removeSuffix(")")
            .split(',')
            .map { token -> token.trim().takeUnless { it.equals("null", true) || it.isEmpty() }?.toIntOrNull() }
    }

    private data class LoadedRecipe(
        val recipe: CatalogRecipe,
        val instructions: String?,
        val instructionTimes: String?,
    )
}

private object RVectorParser {
    fun parse(value: String?): List<String> {
        val text = value?.trim().orEmpty()
        if (text.isEmpty() || text in setOf("NA", "NULL", "character(0)")) return emptyList()
        if (!text.startsWith("c(") || !text.endsWith(")")) return listOf(text.trim('"', '\''))

        val result = mutableListOf<String>()
        var index = 2
        val end = text.length - 1
        while (index < end) {
            while (index < end && (text[index].isWhitespace() || text[index] == ',')) index++
            if (index >= end) break
            if (text.startsWith("NA", index)) {
                result += ""
                index += 2
                continue
            }
            val quote = text[index]
            if (quote != '"' && quote != '\'') break
            index++
            val item = StringBuilder()
            while (index < end) {
                val char = text[index++]
                if (char == quote) break
                if (char == '\\' && index < end) {
                    val escaped = text[index++]
                    item.append(when (escaped) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> escaped
                    })
                } else {
                    item.append(char)
                }
            }
            result += item.toString()
        }
        return result
    }
}

private object IngredientNameNormalizer {
    private val spaces = Regex("\\s+")
    private val nonWord = Regex("[^a-z0-9%+ ]+")
    private val languageTag = Regex("^[a-z]{2,3}:")
    private val descriptors = setOf(
        "baking", "cooking", "crisp", "dessert", "dried", "fresh", "frozen", "green",
        "ground", "large", "medium", "minced", "peeled", "raw", "red", "ripe", "sliced",
        "small", "sour", "sweet", "tart", "yellow",
    )
    private val exceptions = setOf("asparagus", "couscous", "glass", "grass", "molasses")
    private val irregular = mapOf(
        "berries" to "berry", "cherries" to "cherry", "cloves" to "clove",
        "leaves" to "leaf", "loaves" to "loaf", "mangoes" to "mango",
        "potatoes" to "potato", "tomatoes" to "tomato",
    )

    fun normalizeAlias(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        return decomposed.trim().trim('"', '\'').lowercase(Locale.ROOT)
            .let { languageTag.replace(it, "") }
            .replace('_', ' ')
            .replace('-', ' ')
            .let { nonWord.replace(it, " ") }
            .let { spaces.replace(it, " ") }
            .trim()
    }

    fun canonicalize(value: String): String {
        val words = normalizeAlias(value).split(' ').filter(String::isNotBlank).toMutableList()
        while (words.size > 1 && words.first() in descriptors) words.removeAt(0)
        if (words.isNotEmpty()) words[words.lastIndex] = singularize(words.last())
        return words.joinToString(" ")
    }

    private fun singularize(word: String): String = when {
        word in exceptions -> word
        word in irregular -> irregular.getValue(word)
        word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"
        listOf("ches", "shes", "xes", "zes").any(word::endsWith) -> word.dropLast(2)
        word.endsWith("ses") && !word.endsWith("sses") -> word.dropLast(1)
        word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us") -> word.dropLast(1)
        else -> word
    }
}
