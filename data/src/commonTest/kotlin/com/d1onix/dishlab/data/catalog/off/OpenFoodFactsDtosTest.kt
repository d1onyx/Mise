package com.d1onix.dishlab.data.catalog.off

import com.d1onyx.core.network.serialization.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenFoodFactsDtosTest {

    private val json = createDefaultJson(isDebug = false)

    @Test
    fun `v3_6 product maps structured nutrition ingredients packaging and provenance`() {
        val response = json.decodeFromString<OpenFoodFactsProductResponseDto>(PRODUCT_JSON)

        val product = assertNotNull(response.toSnapshot("4009900552387"))

        assertEquals("4009900552387", product.barcode)
        assertEquals("Mineral water", product.name)
        assertEquals("en:water", product.categories.last())
        assertEquals(0.5, product.ingredients.single().percentEstimate)
        assertEquals(0.0, product.nutrition?.nutrients?.get("fat")?.value)
        assertEquals("en:pet-1-polyethylene-terephthalate", product.packaging.single().material)
        assertEquals(1004, product.sourceSchemaVersion)
        assertEquals(42L, product.sourceRevision)
        assertEquals("2023", product.nutriScoreVersion)
        assertEquals("en:waters", product.comparedToCategory)
        assertEquals("12-2026", product.expirationDate)
        assertEquals("Local spring", product.originNote)
        // pnns_groups_1/2 carry a digit right after the underscore, which the
        // SnakeCase naming strategy alone would map to "pnns_groups1" — this
        // guards the @SerialName override that keeps the real OFF field name.
        assertEquals("Beverages", product.pnnsGroup)
        assertEquals("Waters and flavored waters", product.pnnsSubgroup)
    }

    @Test
    fun `a product with no name is kept rather than dropped`() {
        val response = json.decodeFromString<OpenFoodFactsProductResponseDto>(
            """{ "status": "success", "product": { "code": "0000000000017" } }""",
        )

        val product = assertNotNull(response.toSnapshot("0000000000017"))

        assertEquals("0000000000017", product.name)
    }

    @Test
    fun `the raw OFF product payload is preserved verbatim`() {
        val response = json.decodeFromString<OpenFoodFactsProductResponseDto>(PRODUCT_JSON)
        val rawProduct = json.parseToJsonElement(PRODUCT_JSON).jsonObject["product"]

        val product = assertNotNull(response.toSnapshot("4009900552387", rawProductJson = rawProduct))

        assertEquals(rawProduct.toString(), product.rawSourceJson)
    }

    private companion object {
        const val PRODUCT_JSON = """
            {
              "status": "success",
              "product": {
                "code": "4009900552387",
                "product_name": "Mineral water",
                "generic_name": "Water",
                "lang": "en",
                "brands": "Artesie",
                "quantity": "750 ml",
                "product_quantity": 750,
                "product_quantity_unit": "ml",
                "categories_tags": ["en:beverages", "en:waters", "en:water"],
                "ingredients_text": "Water, carbon dioxide",
                "ingredients": [
                  {
                    "id": "en:carbon-dioxide",
                    "text": "carbon dioxide",
                    "percent_estimate": 0.5,
                    "vegan": "yes"
                  }
                ],
                "allergens_tags": [],
                "nutrition": {
                  "aggregated_set": {
                    "per": "100g",
                    "preparation": "as_sold",
                    "nutrients": {
                      "energy-kcal": {"value": 0, "unit": "kcal", "source": "packaging", "source_per": "100g"},
                      "fat": {"value": 0, "unit": "g", "source": "packaging", "source_per": "100g"}
                    }
                  }
                },
                "nutriscore_grade": "a",
                "nutriscore_version": "2023",
                "compared_to_category": "en:waters",
                "expiration_date": "12-2026",
                "origin": "Local spring",
                "pnns_groups_1": "Beverages",
                "pnns_groups_2": "Waters and flavored waters",
                "environmental_score_grade": "b",
                "packagings": [
                  {
                    "number_of_units": 1,
                    "quantity_per_unit": "750 ml",
                    "material": {"id": "en:pet-1-polyethylene-terephthalate"},
                    "shape": {"id": "en:bottle"},
                    "recycling": {"id": "en:recycle"}
                  }
                ],
                "schema_version": 1004,
                "rev": 42,
                "last_modified_t": 1785113472
              }
            }
        """
    }
}
