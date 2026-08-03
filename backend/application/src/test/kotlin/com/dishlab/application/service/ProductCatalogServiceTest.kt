package com.dishlab.application.service

import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductPage
import com.dishlab.domain.model.CatalogNutrientValue
import com.dishlab.domain.model.CatalogProductNutrition
import com.dishlab.domain.model.CatalogProductSource
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProductCatalogServiceTest {
    @Test
    fun `barcode lookup replaces taxonomy guess with normalized recipe tag`() = runBlocking {
        val service = ProductCatalogService(
            provider = FakeCatalogProvider,
            canonicalizer = canonicalizerReturning("table salt", "salt"),
        )

        val product = service.findByBarcode("user", "482000000001", "uk")

        assertEquals("Сіль кухонна", product?.name)
        assertEquals(listOf("en:table-salt", "en:salt"), product?.canonicalTags)
    }

    @Test
    fun `client OFF snapshot is canonicalized without provider lookup and keeps provenance`() = runBlocking {
        var providerCalled = false
        val service = ProductCatalogService(
            provider = object : ProductCatalogProvider by FakeCatalogProvider {
                override fun findByBarcode(barcode: String, language: String?): CatalogProduct {
                    providerCalled = true
                    return FakeCatalogProvider.findByBarcode(barcode, language)
                }
            },
            canonicalizer = canonicalizerReturning("sparkling water", "water"),
        )
        val snapshot = CatalogProduct(
            barcode = "4009900552387",
            name = "Artesie jemne perliva",
            categories = listOf("Beverages", "Waters"),
            nutrition = CatalogProductNutrition(
                per = "100g",
                preparation = "as_sold",
                nutrients = mapOf("energy-kcal" to CatalogNutrientValue(value = 0.0, unit = "kcal")),
            ),
            source = CatalogProductSource(
                provider = "open_food_facts",
                schemaVersion = 1004,
                revision = 42,
                clientProvided = true,
            ),
        )

        val resolved = service.resolveClientProduct("user", snapshot)

        assertEquals(false, providerCalled)
        assertEquals(listOf("en:sparkling-water", "en:water"), resolved.canonicalTags)
        assertEquals(1004, resolved.source?.schemaVersion)
        assertEquals(true, resolved.source?.clientProvided)
        assertNotNull(resolved.foodConceptId)
        assertNotNull(resolved.foodVariantId)

        val stored = service.findByBarcode("user", snapshot.barcode, "en")
        assertEquals(false, providerCalled)
        assertEquals(resolved.foodVariantId, stored?.foodVariantId)
    }

    private fun canonicalizerReturning(vararg names: String): ProductCanonicalizationService =
        ProductCanonicalizationService(
            normalizer = object : ProductNameNormalizer {
                override suspend fun normalize(
                    products: List<ProductNormalizationInput>,
                ): List<NormalizedProductName> = products.map {
                    NormalizedProductName(it.name, names.toList())
                }
            },
            taxonomy = InMemoryFoodTaxonomyRepository(),
        )

    private object FakeCatalogProvider : ProductCatalogProvider {
        override fun findByBarcode(barcode: String, language: String?): CatalogProduct =
            CatalogProduct(
                barcode = barcode,
                name = "Сіль кухонна",
                categories = listOf("Foods", "Salts"),
                canonicalTags = listOf("en:seasoning"),
            )

        override fun search(
            query: String,
            country: String?,
            language: String?,
            page: Int,
            pageSize: Int,
        ): CatalogProductPage = CatalogProductPage(emptyList(), page, false)

        override fun searchCategories(query: String, language: String): List<String> = emptyList()
    }
}
