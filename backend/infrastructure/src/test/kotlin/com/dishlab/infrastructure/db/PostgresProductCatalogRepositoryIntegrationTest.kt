package com.dishlab.infrastructure.db

import com.dishlab.application.service.FoodAlias
import com.dishlab.domain.model.CatalogNutrientValue
import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductNutrition
import com.dishlab.domain.model.CatalogProductSource
import com.dishlab.domain.model.FoodCarbonation
import com.dishlab.domain.model.FoodOrigin
import com.dishlab.domain.model.FoodVariant
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PostgresProductCatalogRepositoryIntegrationTest {

    @Test
    fun `taxonomy product and source revision persist atomically`() {
        val url = System.getenv("TEST_DATABASE_URL") ?: return
        val dataSource = DatabaseFactory.create(
            url = url,
            user = System.getenv("TEST_DATABASE_USER") ?: "dishlab",
            password = System.getenv("TEST_DATABASE_PASSWORD") ?: "dishlab",
        )
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val barcode = "9${suffix.filter(Char::isDigit).padEnd(11, '0').take(11)}"
        var conceptId: UUID? = null
        try {
            val taxonomy = PostgresFoodTaxonomyRepository(dataSource)
            val products = PostgresProductCatalogRepository(dataSource)
            val concept = taxonomy.upsertConcept("integration water $suffix", FoodOrigin.MINERAL)
            conceptId = concept.id
            taxonomy.saveAlias(
                FoodAlias(concept.id, "en", "Integration water", "integration water", "test", 1.0),
            )
            val variant = taxonomy.upsertVariant(
                FoodVariant(
                    id = UUID.randomUUID(),
                    conceptId = concept.id,
                    canonicalName = "lightly sparkling integration water",
                    origin = FoodOrigin.MINERAL,
                    carbonation = FoodCarbonation.LIGHTLY_SPARKLING,
                ),
            )
            val product = CatalogProduct(
                barcode = barcode,
                name = "Integration water",
                categories = listOf("Waters", "Sparkling waters"),
                nutrition = CatalogProductNutrition(
                    per = "100g",
                    preparation = "as_sold",
                    nutrients = mapOf("energy-kcal" to CatalogNutrientValue(0.0, unit = "kcal")),
                ),
                source = CatalogProductSource("open_food_facts", 1004, 987654321, 1785113472, true),
                canonicalTags = listOf("en:water"),
                foodConceptId = concept.id,
                foodVariantId = variant.id,
            )

            products.save(product)

            assertEquals(product, products.findByBarcode(barcode))
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    """
                    SELECT count(*)
                    FROM product_source_snapshots s
                    JOIN retail_products p ON p.id = s.retail_product_id
                    WHERE p.barcode = ? AND s.provider = ? AND s.source_revision = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, barcode)
                    statement.setString(2, "open_food_facts")
                    statement.setLong(3, 987654321)
                    statement.executeQuery().use { result ->
                        result.next()
                        assertEquals(1, result.getInt(1))
                    }
                }
            }
            assertNotNull(conceptId)
        } finally {
            dataSource.connection.use { connection ->
                connection.prepareStatement("DELETE FROM retail_products WHERE barcode = ?").use {
                    it.setString(1, barcode)
                    it.executeUpdate()
                }
                conceptId?.let { id ->
                    connection.prepareStatement("DELETE FROM food_concepts WHERE id = ?").use {
                        it.setObject(1, id)
                        it.executeUpdate()
                    }
                }
            }
            (dataSource as? HikariDataSource)?.close()
        }
    }
}
