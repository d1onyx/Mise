package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.designsystem.component.MiseFact
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDetails
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductIngredient
import com.d1onix.dishlab.domain.model.ProductPackagingComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The post-scan review screen renders its own fact rows, independent of the
 * graph's product sheet — a field added to one does not automatically reach the
 * other, so this mirrors that surface's coverage directly. Split across three
 * functions because the screen now spreads them across separate swipeable tabs.
 *
 * Every row is always present (a blank value means the sheet shows a
 * placeholder instead of hiding the row), so these tests assert on values, not
 * key presence.
 */
class ScanDetailsTest {

    @Test
    fun `overview page shows brand quantity ingredients text and allergens`() {
        val facts = scannedProduct().copy(
            details = scannedProduct().details.copy(allergens = listOf("en:gluten")),
        ).overviewFacts().toMap()

        assertEquals("Spring", facts.getValue("Brand"))
        assertEquals("gluten", facts.getValue("Allergens"))
    }

    @Test
    fun `composition page covers traces additives origin and packaging`() {
        val facts = scannedProduct().compositionFacts().toMap()

        assertEquals("Nuts", facts.getValue("May contain traces of"))
        assertEquals("E290", facts.getValue("Additives"))
        assertEquals("France", facts.getValue("Sold in"))
        assertEquals("Brittany", facts.getValue("Ingredient origin"))
        assertTrue(facts.getValue("Packaging").isNotBlank())
    }

    @Test
    fun `structured ingredients are summarized with their percent on the composition page`() {
        val facts = scannedProduct().compositionFacts().toMap()

        assertTrue(facts.getValue("Ingredients breakdown").contains("90%"))
    }

    @Test
    fun `nutrition page covers nova and nutrient levels`() {
        val facts = scannedProduct().copy(
            details = scannedProduct().details.copy(novaGroup = 4, nutrientLevels = mapOf("fat" to "High")),
        ).nutritionFacts()

        assertTrue(facts.any { it.label == "NOVA" && it.value.startsWith("4") })
        assertTrue(facts.any { it.label == "fat" && it.value == "High" })
    }

    @Test
    fun `composition page covers food classification and sourcing`() {
        val facts = scannedProduct().copy(
            details = scannedProduct().details.copy(
                foodGroups = listOf("en:beverages"),
                manufacturingPlaces = listOf("en:france"),
                purchasePlaces = listOf("en:france"),
                stores = listOf("en:carrefour"),
            ),
        ).compositionFacts().toMap()

        assertEquals("beverages", facts.getValue("Food classification"))
        assertEquals("france", facts.getValue("Manufactured in"))
        assertEquals("france", facts.getValue("Purchased in"))
        assertEquals("carrefour", facts.getValue("Stores"))
    }

    @Test
    fun `empty optional fields still produce a row just with a blank value`() {
        val facts = Product(
            id = ProductId("barcode:1"), barcode = "1", name = "Water", category = "Beverages", score = 90,
            accentColor = 0, initial = "W", nutrients = emptyList(), summary = "", hasCompleteData = true,
            alternatives = emptyList(), details = ProductDetails(brand = "Spring"),
        ).overviewFacts().toMap()

        assertEquals("Spring", facts.getValue("Brand"))
        assertEquals("", facts.getValue("Quantity"))
        assertEquals("", facts.getValue("Allergens"))
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

private fun List<MiseFact>.toMap(): Map<String, String> = associate { it.label to it.value }
