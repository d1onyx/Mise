package com.d1onix.dishlab.feature.products.presentation.graph.components

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDetails
import com.d1onix.dishlab.domain.model.ProductId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductDetailCardModelTest {

    @Test
    fun `full OFF snapshot maps into independent card sections`() {
        val card = fullProduct().toDetailCardModel()

        assertEquals(6, card.facts.size)
        assertEquals("Oats", card.ingredients)
        assertEquals(listOf("Gluten"), card.allergens)
        assertEquals(2, card.nutrients.size)
        assertFalse(card.notInDatabase)
        assertTrue(card.imageUrl!!.startsWith("https://"))
    }

    @Test
    fun `empty OFF fields produce no empty card sections`() {
        val card = fullProduct().copy(details = ProductDetails(brand = "Mill")).toDetailCardModel()

        assertEquals(listOf(ProductDetailFactKind.Brand), card.facts.map { it.kind })
        assertEquals(null, card.ingredients)
        assertTrue(card.allergens.isEmpty())
        assertTrue(card.categories.isEmpty())
        assertTrue(card.labels.isEmpty())
        assertEquals(null, card.imageUrl)
    }

    @Test
    fun `zero nutrition maps to the not yet in database state`() {
        val card = fullProduct().copy(nutrients = listOf(Nutrient("Energy", "0", "kcal"))).toDetailCardModel()

        assertTrue(card.notInDatabase)
        assertTrue(card.nutrients.isEmpty())
    }

    private fun fullProduct() = Product(
        id = ProductId("barcode:1"), barcode = "1", name = "Oats", category = "Cereals", score = 90,
        accentColor = 0, initial = "O", nutrients = listOf(Nutrient("Energy", "350", "kcal"), Nutrient("Protein", "12", "g")),
        summary = "", hasCompleteData = true, alternatives = emptyList(),
        details = ProductDetails(
            brand = "Mill", quantity = "500 g", servingSize = "50 g", ingredientsText = "Oats",
            allergens = listOf("Gluten"), categories = listOf("Cereals"), labels = listOf("Vegan"),
            nutriScore = "a", novaGroup = 1, ecoScore = "b", imageUrl = "https://example.test/oats.jpg",
        ),
    )
}
