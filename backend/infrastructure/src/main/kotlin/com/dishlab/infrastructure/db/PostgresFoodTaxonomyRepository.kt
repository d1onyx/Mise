package com.dishlab.infrastructure.db

import com.dishlab.application.service.FoodAlias
import com.dishlab.application.service.FoodTaxonomyRepository
import com.dishlab.domain.model.FoodCarbonation
import com.dishlab.domain.model.FoodConcept
import com.dishlab.domain.model.FoodOrigin
import com.dishlab.domain.model.FoodPhysicalForm
import com.dishlab.domain.model.FoodPreparationState
import com.dishlab.domain.model.FoodPreservation
import com.dishlab.domain.model.FoodVariant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource

class PostgresFoodTaxonomyRepository(
    private val dataSource: DataSource,
    private val json: Json = Json,
) : FoodTaxonomyRepository {

    override fun upsertConcept(canonicalName: String, origin: FoodOrigin): FoodConcept =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO food_concepts (canonical_name, origin)
                VALUES (?, ?)
                ON CONFLICT (canonical_name) DO UPDATE SET
                    origin = CASE
                        WHEN food_concepts.origin = 'UNKNOWN' THEN EXCLUDED.origin
                        ELSE food_concepts.origin
                    END,
                    updated_at = now()
                RETURNING id, canonical_name, parent_id, origin, taxonomy_version
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, canonicalName.trim().lowercase())
                statement.setString(2, origin.name)
                statement.executeQuery().use { result ->
                    result.next()
                    result.toConcept()
                }
            }
        }

    override fun saveAlias(alias: FoodAlias) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO food_aliases
                    (concept_id, language, alias, normalized_alias, source, confidence)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (concept_id, language, normalized_alias) DO UPDATE SET
                    alias = EXCLUDED.alias,
                    source = EXCLUDED.source,
                    confidence = GREATEST(food_aliases.confidence, EXCLUDED.confidence)
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, alias.conceptId)
                statement.setString(2, alias.language)
                statement.setString(3, alias.value)
                statement.setString(4, alias.normalizedValue)
                statement.setString(5, alias.source)
                statement.setDouble(6, alias.confidence)
                statement.executeUpdate()
            }
        }
    }

    override fun upsertVariant(variant: FoodVariant): FoodVariant =
        dataSource.connection.use { connection ->
            val sortedFacets: Map<String, String> = variant.facets.toSortedMap()
            val facets = jsonb(json.encodeToString(sortedFacets))
            val inserted = connection.prepareStatement(
                """
                INSERT INTO food_variants
                    (id, concept_id, canonical_name, origin, preparation_state,
                     physical_form, carbonation, preservation, facets)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                RETURNING id, concept_id, canonical_name, origin, preparation_state,
                          physical_form, carbonation, preservation, facets
                """.trimIndent(),
            ).use { statement ->
                statement.bindVariant(variant, facets)
                statement.executeQuery().use { result -> if (result.next()) result.toVariant(json) else null }
            }
            inserted ?: connection.prepareStatement(
                """
                SELECT id, concept_id, canonical_name, origin, preparation_state,
                       physical_form, carbonation, preservation, facets
                FROM food_variants
                WHERE concept_id = ?
                  AND lower(canonical_name::text) = lower(?)
                  AND origin = ?
                  AND preparation_state = ?
                  AND physical_form = ?
                  AND carbonation = ?
                  AND preservation = ?
                  AND facets = ?
                LIMIT 1
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, variant.conceptId)
                statement.setString(2, variant.canonicalName)
                statement.setString(3, variant.origin.name)
                statement.setString(4, variant.preparationState.name)
                statement.setString(5, variant.physicalForm.name)
                statement.setString(6, variant.carbonation.name)
                statement.setString(7, variant.preservation.name)
                statement.setObject(8, facets)
                statement.executeQuery().use { result ->
                    check(result.next()) { "Food variant conflict was not readable after upsert" }
                    result.toVariant(json)
                }
            }
        }

    private fun java.sql.PreparedStatement.bindVariant(variant: FoodVariant, facets: PGobject) {
        setObject(1, variant.id)
        setObject(2, variant.conceptId)
        setString(3, variant.canonicalName)
        setString(4, variant.origin.name)
        setString(5, variant.preparationState.name)
        setString(6, variant.physicalForm.name)
        setString(7, variant.carbonation.name)
        setString(8, variant.preservation.name)
        setObject(9, facets)
    }

    private fun ResultSet.toConcept(): FoodConcept = FoodConcept(
        id = getObject("id", UUID::class.java),
        canonicalName = getString("canonical_name"),
        parentId = getObject("parent_id", UUID::class.java),
        origin = FoodOrigin.valueOf(getString("origin")),
        taxonomyVersion = getInt("taxonomy_version"),
    )

    private fun ResultSet.toVariant(json: Json): FoodVariant = FoodVariant(
        id = getObject("id", UUID::class.java),
        conceptId = getObject("concept_id", UUID::class.java),
        canonicalName = getString("canonical_name"),
        origin = FoodOrigin.valueOf(getString("origin")),
        preparationState = FoodPreparationState.valueOf(getString("preparation_state")),
        physicalForm = FoodPhysicalForm.valueOf(getString("physical_form")),
        carbonation = FoodCarbonation.valueOf(getString("carbonation")),
        preservation = FoodPreservation.valueOf(getString("preservation")),
        facets = json.decodeFromString(getString("facets")),
    )

    private fun jsonb(value: String): PGobject = PGobject().apply {
        type = "jsonb"
        this.value = value
    }
}
