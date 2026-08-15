package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDetails
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductIngredient
import com.d1onix.dishlab.domain.model.ProductPackagingComponent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The post-scan review screen renders its own detail tiles, independent of the
 * graph's product sheet — a field added to one does not automatically reach the
 * other, so this mirrors that surface's coverage directly.
 */
class ScanDetailsTest {

    @Test
    fun `traces additives origin and packaging appear as review tiles`() {
        val details = scannedProduct().scanDetails().toMap()

        assertTrue(details.containsKey("May contain traces of"))
        assertTrue(details.containsKey("Additives"))
        assertTrue(details.containsKey("Sold in"))
        assertTrue(details.containsKey("Ingredient origin"))
        assertTrue(details.containsKey("Packaging"))
    }

    @Test
    fun `structured ingredients are summarized with their percent`() {
        val details = scannedProduct().scanDetails().toMap()

        assertTrue(details.getValue("Ingredients breakdown").contains("90%"))
    }

    @Test
    fun `nutrient levels food groups and sourcing appear as review tiles`() {
        val details = scannedProduct().copy(
            details = scannedProduct().details.copy(
                nutrientLevels = mapOf("fat" to "High"),
                foodGroups = listOf("en:beverages"),
                manufacturingPlaces = listOf("en:france"),
                purchasePlaces = listOf("en:france"),
                stores = listOf("en:carrefour"),
            ),
        ).scanDetails().toMap()

        assertTrue(details.containsKey("Nutrient levels"))
        assertTrue(details.containsKey("Food groups"))
        assertTrue(details.containsKey("Manufactured in"))
        assertTrue(details.containsKey("Purchased in"))
        assertTrue(details.containsKey("Stores"))
    }

    @Test
    fun `empty optional fields do not produce tiles`() {
        val details = Product(
            id = ProductId("barcode:1"), barcode = "1", name = "Water", category = "Beverages", score = 90,
            accentColor = 0, initial = "W", nutrients = emptyList(), summary = "", hasCompleteData = true,
            alternatives = emptyList(), details = ProductDetails(brand = "Spring"),
        ).scanDetails().toMap()

        assertTrue(details.keys == setOf("Brand"))
    }

    private fun scannedProduct() = Product(
        id = ProductId("barcode:1"), barcode = "1", name = "Sparkling Water", category = "Beverages", score = 90,
        accentColor = 0, initial = "S", nutrients = emptyList(), summary = "", hasCompleteData = true,
        alternatives = emptyList(),
        details = ProductDetails(
            brand = "Spring",
            ingredients = listOf(ProductIngredient(name = "Water", percentEstimate = 90.0)),
            traces = listOf("Nuts"),
            additives = listOf("E290"),
            countries = listOf("France"),
            origins = listOf("Brittany"),
            packaging = listOf(
                ProductPackagingComponent(numberOfUnits = 1, quantityPerUnit = "750 ml", material = "PET", shape = "Bottle", recycling = "Recycle"),
            ),
        ),
    )
}
