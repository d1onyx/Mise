package com.dishlab.infrastructure.db

import com.dishlab.domain.model.CatalogIngredient
import com.dishlab.domain.model.CatalogNutrientValue
import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductNutrition
import com.dishlab.domain.model.CatalogProductSource
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductPersistenceDtosTest {

    @Test
    fun `detailed canonical product survives jsonb payload round trip`() {
        val json = Json { encodeDefaults = true }
        val original = CatalogProduct(
            barcode = "4009900552387",
            name = "Mineral water",
            genericName = "Water",
            categories = listOf("Waters", "Sparkling waters"),
            ingredients = listOf(CatalogIngredient(id = "en:carbon-dioxide", percentEstimate = 0.5)),
            allergens = listOf("en:milk"),
            nutrition = CatalogProductNutrition(
                per = "100g",
                preparation = "as_sold",
                nutrients = mapOf("energy-kcal" to CatalogNutrientValue(0.0, unit = "kcal")),
            ),
            source = CatalogProductSource("open_food_facts", 1004, 42, 1785113472, true),
            canonicalTags = listOf("en:sparkling-water", "en:water"),
            foodConceptId = UUID.randomUUID(),
            foodVariantId = UUID.randomUUID(),
        )

        val restored = json.decodeFromString<StoredCatalogProduct>(json.encodeToString(original.toStored())).toDomain()

        assertEquals(original, restored)
    }
}
