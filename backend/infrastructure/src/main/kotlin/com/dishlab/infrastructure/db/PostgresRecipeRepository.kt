package com.dishlab.infrastructure.db

import com.dishlab.application.service.RecipeRepository
import com.dishlab.domain.model.Recipe
import com.dishlab.domain.model.RecipeIngredient
import com.dishlab.domain.model.RecipeReport
import com.dishlab.domain.model.RecipeReview
import com.dishlab.domain.model.RecipeStatus
import com.dishlab.domain.model.RecipeStep
import com.dishlab.domain.model.RecipeVersion
import com.dishlab.domain.model.RecipeVisibility
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class PostgresRecipeRepository(private val ds: DataSource) : RecipeRepository {

    // ── write ──────────────────────────────────────────────────────────────────

    override fun save(recipe: Recipe): Recipe {
        ds.connection.use { conn ->
            conn.autoCommit = false
            try {
                upsertRecipeRow(conn, recipe)
                recipe.versions.forEach { v ->
                    upsertVersionRow(conn, v)
                    replaceSubData(conn, v)
                }
                setCurrentVersionId(conn, recipe.id, recipe.currentVersion.id)
                replaceBookmarks(conn, recipe.id, recipe.bookmarkedByUserIds)
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        return recipe
    }

    override fun delete(id: UUID) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM recipes WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
            }
        }
    }

    // ── read ───────────────────────────────────────────────────────────────────

    override fun findById(id: UUID): Recipe? {
        ds.connection.use { conn ->
            val row = queryOne(conn, "SELECT * FROM recipes WHERE id = ?", id) { readRecipeRow(it) }
                ?: return null
            val versions = loadVersions(conn, listOf(id))
            val bookmarks = loadBookmarks(conn, listOf(id))
            return assemble(row, versions[id].orEmpty(), bookmarks[id].orEmpty())
        }
    }

    override fun list(): List<Recipe> {
        ds.connection.use { conn ->
            val rows = queryAll(conn, "SELECT * FROM recipes ORDER BY updated_at DESC") { readRecipeRow(it) }
            if (rows.isEmpty()) return emptyList()
            val ids = rows.map { it.id }
            val versions = loadVersions(conn, ids)
            val bookmarks = loadBookmarks(conn, ids)
            return rows.mapNotNull { row ->
                runCatching { assemble(row, versions[row.id].orEmpty(), bookmarks[row.id].orEmpty()) }.getOrNull()
            }
        }
    }

    // ── reviews ────────────────────────────────────────────────────────────────

    override fun saveReview(review: RecipeReview): RecipeReview {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO recipe_reviews (id, recipe_id, user_id, rating, comment, created_at)
                   VALUES (?, ?, ?, ?, ?, ?)
                   ON CONFLICT (recipe_id, user_id) DO UPDATE SET
                       rating = EXCLUDED.rating, comment = EXCLUDED.comment""",
            ).use { ps ->
                ps.setObject(1, review.id)
                ps.setObject(2, review.recipeId)
                ps.setObject(3, review.userId)
                ps.setInt(4, review.rating)
                ps.setString(5, review.comment)
                ps.setTimestamp(6, Timestamp.from(review.createdAt))
                ps.executeUpdate()
            }
        }
        return review
    }

    override fun reviews(recipeId: UUID): List<RecipeReview> =
        ds.connection.use { conn ->
            queryAll(conn, "SELECT * FROM recipe_reviews WHERE recipe_id = ? ORDER BY created_at DESC", recipeId) { readReview(it) }
        }

    // ── reports ────────────────────────────────────────────────────────────────

    override fun saveReport(report: RecipeReport): RecipeReport {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO recipe_reports (id, recipe_id, user_id, reason, comment, status, created_at, resolved_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                   ON CONFLICT (id) DO UPDATE SET
                       status = EXCLUDED.status, resolved_at = EXCLUDED.resolved_at""",
            ).use { ps ->
                ps.setObject(1, report.id)
                ps.setObject(2, report.recipeId)
                ps.setObject(3, report.userId)
                ps.setString(4, report.reason)
                ps.setString(5, report.comment)
                ps.setString(6, report.status)
                ps.setTimestamp(7, Timestamp.from(report.createdAt))
                ps.setTimestamp(8, report.resolvedAt?.let { Timestamp.from(it) })
                ps.executeUpdate()
            }
        }
        return report
    }

    override fun reports(): List<RecipeReport> =
        ds.connection.use { conn ->
            queryAll(conn, "SELECT * FROM recipe_reports ORDER BY created_at DESC") { readReport(it) }
        }

    override fun findReportById(reportId: UUID): RecipeReport? =
        ds.connection.use { conn ->
            queryOne(conn, "SELECT * FROM recipe_reports WHERE id = ?", reportId) { readReport(it) }
        }

    // ── upsert internals ───────────────────────────────────────────────────────

    private fun upsertRecipeRow(conn: Connection, r: Recipe) {
        conn.prepareStatement(
            """INSERT INTO recipes
                   (id, owner_user_id, forked_from_recipe_id, status, visibility,
                    moderation_status, moderation_note, created_at, updated_at, published_at)
               VALUES (?, ?, ?, ?, ?, 'NOT_REQUIRED', NULL, ?, ?, ?)
               ON CONFLICT (id) DO UPDATE SET
                   status      = EXCLUDED.status,
                   visibility  = EXCLUDED.visibility,
                   updated_at  = EXCLUDED.updated_at,
                   published_at = EXCLUDED.published_at""",
        ).use { ps ->
            ps.setObject(1, r.id)
            ps.setObject(2, r.ownerUserId)
            ps.setObject(3, r.forkedFromRecipeId)
            ps.setString(4, r.status.name)
            ps.setString(5, r.visibility.name)
            ps.setTimestamp(6, Timestamp.from(r.createdAt))
            ps.setTimestamp(7, Timestamp.from(r.updatedAt))
            ps.setTimestamp(8, r.publishedAt?.let { Timestamp.from(it) })
            ps.executeUpdate()
        }
    }

    private fun upsertVersionRow(conn: Connection, v: RecipeVersion) {
        conn.prepareStatement(
            """INSERT INTO recipe_versions
                   (id, recipe_id, version_number, changelog, title, description, servings, image_url, created_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT (id) DO UPDATE SET
                   title       = EXCLUDED.title,
                   description = EXCLUDED.description,
                   servings    = EXCLUDED.servings,
                   changelog   = EXCLUDED.changelog,
                   image_url   = EXCLUDED.image_url""",
        ).use { ps ->
            ps.setObject(1, v.id)
            ps.setObject(2, v.recipeId)
            ps.setInt(3, v.versionNumber)
            ps.setString(4, v.changelog)
            ps.setString(5, v.title)
            ps.setString(6, v.description)
            ps.setInt(7, v.servings)
            ps.setString(8, v.imageUrl)
            ps.setTimestamp(9, Timestamp.from(v.createdAt))
            ps.executeUpdate()
        }
    }

    private fun replaceSubData(conn: Connection, v: RecipeVersion) {
        exec(conn, "DELETE FROM recipe_ingredients WHERE recipe_version_id = ?", v.id)
        v.ingredients.forEachIndexed { idx, ing ->
            conn.prepareStatement(
                "INSERT INTO recipe_ingredients (id, recipe_version_id, ingredient_id, name, amount, unit, note, position, canonical_tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { ps ->
                ps.setObject(1, UUID.randomUUID())
                ps.setObject(2, v.id)
                ps.setObject(3, ing.ingredientId)
                ps.setString(4, ing.name)
                ps.setBigDecimal(5, BigDecimal.valueOf(ing.amount))
                ps.setString(6, ing.unit)
                ps.setString(7, ing.note)
                ps.setInt(8, idx + 1)
                ps.setArray(9, conn.createArrayOf("text", ing.canonicalTags.toTypedArray()))
                ps.executeUpdate()
            }
        }

        exec(conn, "DELETE FROM recipe_steps WHERE recipe_version_id = ?", v.id)
        v.steps.forEach { step ->
            conn.prepareStatement(
                "INSERT INTO recipe_steps (id, recipe_version_id, position, text, timer_seconds) VALUES (?, ?, ?, ?, ?)",
            ).use { ps ->
                ps.setObject(1, UUID.randomUUID())
                ps.setObject(2, v.id)
                ps.setInt(3, step.position)
                ps.setString(4, step.text)
                val timer = step.timerSeconds
                if (timer != null) ps.setInt(5, timer) else ps.setNull(5, Types.INTEGER)
                ps.executeUpdate()
            }
        }

        exec(conn, "DELETE FROM recipe_tags WHERE recipe_version_id = ?", v.id)
        v.tags.forEach { tag ->
            conn.prepareStatement(
                "INSERT INTO recipe_tags (recipe_version_id, tag) VALUES (?, ?) ON CONFLICT DO NOTHING",
            ).use { ps ->
                ps.setObject(1, v.id)
                ps.setString(2, tag)
                ps.executeUpdate()
            }
        }

        exec(conn, "DELETE FROM recipe_equipment WHERE recipe_version_id = ?", v.id)
        v.equipment.forEach { key ->
            conn.prepareStatement(
                "INSERT INTO recipe_equipment (recipe_version_id, equipment_key) VALUES (?, ?) ON CONFLICT DO NOTHING",
            ).use { ps ->
                ps.setObject(1, v.id)
                ps.setString(2, key)
                ps.executeUpdate()
            }
        }
    }

    private fun setCurrentVersionId(conn: Connection, recipeId: UUID, versionId: UUID) {
        conn.prepareStatement("UPDATE recipes SET current_version_id = ? WHERE id = ?").use { ps ->
            ps.setObject(1, versionId)
            ps.setObject(2, recipeId)
            ps.executeUpdate()
        }
    }

    private fun replaceBookmarks(conn: Connection, recipeId: UUID, userIds: Set<UUID>) {
        exec(conn, "DELETE FROM recipe_bookmarks WHERE recipe_id = ?", recipeId)
        userIds.forEach { userId ->
            conn.prepareStatement(
                "INSERT INTO recipe_bookmarks (recipe_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            ).use { ps ->
                ps.setObject(1, recipeId)
                ps.setObject(2, userId)
                ps.executeUpdate()
            }
        }
    }

    // ── batch load ─────────────────────────────────────────────────────────────

    private fun loadVersions(conn: Connection, recipeIds: List<UUID>): Map<UUID, List<RecipeVersion>> {
        if (recipeIds.isEmpty()) return emptyMap()
        val ph = recipeIds.inPlaceholders()

        data class VRow(
            val id: UUID, val recipeId: UUID, val versionNumber: Int,
            val changelog: String, val title: String, val description: String?,
            val servings: Int, val imageUrl: String?, val createdAt: Instant,
        )

        val vRows = mutableListOf<VRow>()
        conn.prepareStatement("SELECT * FROM recipe_versions WHERE recipe_id IN $ph ORDER BY recipe_id, version_number").use { ps ->
            recipeIds.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) vRows.add(
                    VRow(
                        id = rs.uuid("id"),
                        recipeId = rs.uuid("recipe_id"),
                        versionNumber = rs.getInt("version_number"),
                        changelog = rs.getString("changelog"),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        servings = rs.getInt("servings"),
                        imageUrl = rs.getString("image_url"),
                        createdAt = rs.instant("created_at"),
                    ),
                )
            }
        }
        if (vRows.isEmpty()) return emptyMap()

        val vids = vRows.map { it.id }
        val vph = vids.inPlaceholders()

        val ingByV = mutableMapOf<UUID, MutableList<RecipeIngredient>>()
        conn.prepareStatement("SELECT * FROM recipe_ingredients WHERE recipe_version_id IN $vph ORDER BY position").use { ps ->
            vids.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    ingByV.getOrPut(rs.uuid("recipe_version_id")) { mutableListOf() }.add(
                        RecipeIngredient(
                            ingredientId = rs.getObject("ingredient_id", UUID::class.java),
                            name = rs.getString("name"),
                            amount = rs.getDouble("amount"),
                            unit = rs.getString("unit"),
                            note = rs.getString("note"),
                            canonicalTags = (rs.getArray("canonical_tags")?.array as? Array<*>)
                                ?.filterIsInstance<String>()
                                .orEmpty(),
                        ),
                    )
                }
            }
        }

        val stepByV = mutableMapOf<UUID, MutableList<RecipeStep>>()
        conn.prepareStatement("SELECT * FROM recipe_steps WHERE recipe_version_id IN $vph ORDER BY position").use { ps ->
            vids.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    stepByV.getOrPut(rs.uuid("recipe_version_id")) { mutableListOf() }.add(
                        RecipeStep(
                            position = rs.getInt("position"),
                            text = rs.getString("text"),
                            timerSeconds = rs.getInt("timer_seconds").takeIf { !rs.wasNull() },
                        ),
                    )
                }
            }
        }

        val tagByV = mutableMapOf<UUID, MutableList<String>>()
        conn.prepareStatement("SELECT * FROM recipe_tags WHERE recipe_version_id IN $vph").use { ps ->
            vids.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) tagByV.getOrPut(rs.uuid("recipe_version_id")) { mutableListOf() }.add(rs.getString("tag"))
            }
        }

        val eqByV = mutableMapOf<UUID, MutableList<String>>()
        conn.prepareStatement("SELECT * FROM recipe_equipment WHERE recipe_version_id IN $vph").use { ps ->
            vids.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) eqByV.getOrPut(rs.uuid("recipe_version_id")) { mutableListOf() }.add(rs.getString("equipment_key"))
            }
        }

        return vRows.groupBy { it.recipeId }.mapValues { (_, rows) ->
            rows.map { v ->
                RecipeVersion(
                    id = v.id,
                    recipeId = v.recipeId,
                    versionNumber = v.versionNumber,
                    changelog = v.changelog,
                    title = v.title,
                    description = v.description,
                    servings = v.servings,
                    imageUrl = v.imageUrl,
                    ingredients = ingByV[v.id].orEmpty(),
                    steps = stepByV[v.id].orEmpty(),
                    tags = tagByV[v.id].orEmpty(),
                    equipment = eqByV[v.id].orEmpty(),
                    createdAt = v.createdAt,
                )
            }
        }
    }

    private fun loadBookmarks(conn: Connection, recipeIds: List<UUID>): Map<UUID, Set<UUID>> {
        if (recipeIds.isEmpty()) return emptyMap()
        val ph = recipeIds.inPlaceholders()
        val result = mutableMapOf<UUID, MutableSet<UUID>>()
        conn.prepareStatement("SELECT recipe_id, user_id FROM recipe_bookmarks WHERE recipe_id IN $ph").use { ps ->
            recipeIds.forEachIndexed { i, id -> ps.setObject(i + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) result.getOrPut(rs.uuid("recipe_id")) { mutableSetOf() }.add(rs.uuid("user_id"))
            }
        }
        return result
    }

    // ── row readers ────────────────────────────────────────────────────────────

    private data class RecipeRow(
        val id: UUID, val ownerUserId: UUID, val forkedFromRecipeId: UUID?,
        val status: RecipeStatus, val visibility: RecipeVisibility,
        val currentVersionId: UUID?,
        val createdAt: Instant, val updatedAt: Instant, val publishedAt: Instant?,
    )

    private fun readRecipeRow(rs: ResultSet) = RecipeRow(
        id = rs.uuid("id"),
        ownerUserId = rs.uuid("owner_user_id"),
        forkedFromRecipeId = rs.getObject("forked_from_recipe_id", UUID::class.java),
        status = RecipeStatus.valueOf(rs.getString("status")),
        visibility = RecipeVisibility.valueOf(rs.getString("visibility")),
        currentVersionId = rs.getObject("current_version_id", UUID::class.java),
        createdAt = rs.instant("created_at"),
        updatedAt = rs.instant("updated_at"),
        publishedAt = rs.getTimestamp("published_at")?.toInstant(),
    )

    private fun readReview(rs: ResultSet) = RecipeReview(
        id = rs.uuid("id"),
        recipeId = rs.uuid("recipe_id"),
        userId = rs.uuid("user_id"),
        rating = rs.getInt("rating"),
        comment = rs.getString("comment"),
        createdAt = rs.instant("created_at"),
    )

    private fun readReport(rs: ResultSet) = RecipeReport(
        id = rs.uuid("id"),
        recipeId = rs.uuid("recipe_id"),
        userId = rs.uuid("user_id"),
        reason = rs.getString("reason"),
        comment = rs.getString("comment"),
        status = rs.getString("status"),
        resolvedAt = rs.getTimestamp("resolved_at")?.toInstant(),
        createdAt = rs.instant("created_at"),
    )

    private fun assemble(row: RecipeRow, versions: List<RecipeVersion>, bookmarks: Set<UUID>): Recipe {
        val current = versions.find { it.id == row.currentVersionId }
            ?: versions.maxByOrNull { it.versionNumber }
            ?: error("Recipe ${row.id} has no versions in the database")
        return Recipe(
            id = row.id,
            ownerUserId = row.ownerUserId,
            forkedFromRecipeId = row.forkedFromRecipeId,
            status = row.status,
            visibility = row.visibility,
            currentVersion = current,
            versions = versions,
            bookmarkedByUserIds = bookmarks,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            publishedAt = row.publishedAt,
        )
    }

    // ── JDBC utilities ─────────────────────────────────────────────────────────

    private fun exec(conn: Connection, sql: String, param: Any) {
        conn.prepareStatement(sql).use { ps -> ps.setObject(1, param); ps.executeUpdate() }
    }

    private fun <T> queryOne(conn: Connection, sql: String, vararg params: Any?, mapper: (ResultSet) -> T): T? =
        conn.prepareStatement(sql).use { ps ->
            params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
            ps.executeQuery().use { rs -> if (rs.next()) mapper(rs) else null }
        }

    private fun <T> queryAll(conn: Connection, sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> =
        conn.prepareStatement(sql).use { ps ->
            params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
            ps.executeQuery().use { rs ->
                val list = mutableListOf<T>()
                while (rs.next()) list.add(mapper(rs))
                list
            }
        }

    private fun List<UUID>.inPlaceholders() = joinToString(",", "(", ")") { "?" }

    private fun ResultSet.uuid(col: String): UUID = getObject(col, UUID::class.java)
    private fun ResultSet.instant(col: String): Instant = getTimestamp(col).toInstant()
}
