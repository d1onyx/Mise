package com.dishlab.infrastructure.db

import com.dishlab.application.service.ProductCatalogRepository
import com.dishlab.domain.model.CatalogProduct
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

class PostgresProductCatalogRepository(
    private val dataSource: DataSource,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : ProductCatalogRepository {

    override fun findByBarcode(barcode: String): CatalogProduct? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT product_payload FROM retail_products WHERE barcode = ? LIMIT 1",
            ).use { statement ->
                statement.setString(1, barcode.filter(Char::isDigit))
                statement.executeQuery().use { result ->
                    if (!result.next()) null
                    else json.decodeFromString<StoredCatalogProduct>(result.getString("product_payload")).toDomain()
                }
            }
        }

    override fun save(product: CatalogProduct): CatalogProduct {
        val payload = json.encodeToString(product.toStored())
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                val retailProductId = upsertProduct(connection, product, payload)
                if (product.source != null) upsertSnapshot(connection, retailProductId, product, payload)
                connection.commit()
                return product
            } catch (error: Exception) {
                connection.rollback()
                throw error
            }
        }
    }

    private fun upsertProduct(connection: Connection, product: CatalogProduct, payload: String): UUID =
        connection.prepareStatement(
            """
            INSERT INTO retail_products
                (barcode, concept_id, variant_id, name, brand, generic_name, language,
                 category, source_provider, source_revision, product_payload)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (barcode) DO UPDATE SET
                concept_id = EXCLUDED.concept_id,
                variant_id = EXCLUDED.variant_id,
                name = EXCLUDED.name,
                brand = EXCLUDED.brand,
                generic_name = EXCLUDED.generic_name,
                language = EXCLUDED.language,
                category = EXCLUDED.category,
                source_provider = EXCLUDED.source_provider,
                source_revision = EXCLUDED.source_revision,
                product_payload = EXCLUDED.product_payload,
                updated_at = now()
            RETURNING id
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, product.barcode.filter(Char::isDigit))
            statement.setObject(2, product.foodConceptId)
            statement.setObject(3, product.foodVariantId)
            statement.setString(4, product.name)
            statement.setString(5, product.brand)
            statement.setString(6, product.genericName)
            statement.setString(7, product.language)
            statement.setString(8, product.category)
            statement.setString(9, product.source?.provider)
            statement.setObject(10, product.source?.revision)
            statement.setObject(11, jsonb(payload))
            statement.executeQuery().use { result ->
                result.next()
                result.getObject("id", UUID::class.java)
            }
        }

    private fun upsertSnapshot(
        connection: Connection,
        retailProductId: UUID,
        product: CatalogProduct,
        payload: String,
    ) {
        val source = requireNotNull(product.source)
        connection.prepareStatement(
            """
            INSERT INTO product_source_snapshots
                (retail_product_id, provider, schema_version, source_revision,
                 source_updated_at, client_provided, payload)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (retail_product_id, provider, source_revision) DO UPDATE SET
                schema_version = EXCLUDED.schema_version,
                source_updated_at = EXCLUDED.source_updated_at,
                client_provided = EXCLUDED.client_provided,
                payload = EXCLUDED.payload,
                received_at = now()
            """.trimIndent(),
        ).use { statement ->
            statement.setObject(1, retailProductId)
            statement.setString(2, source.provider)
            statement.setObject(3, source.schemaVersion)
            statement.setLong(4, source.revision ?: 0L)
            statement.setTimestamp(5, source.sourceUpdatedAtEpochSeconds?.let { Timestamp.from(Instant.ofEpochSecond(it)) })
            statement.setBoolean(6, source.clientProvided)
            statement.setObject(7, jsonb(payload))
            statement.executeUpdate()
        }
    }

    private fun jsonb(value: String): PGobject = PGobject().apply {
        type = "jsonb"
        this.value = value
    }
}
