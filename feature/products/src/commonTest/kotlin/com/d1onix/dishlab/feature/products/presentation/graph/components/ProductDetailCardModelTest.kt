package com.d1onix.dishlab.feature.products.presentation.graph.components

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDetails
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductIngredient
import com.d1onix.dishlab.domain.model.ProductPackagingComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductDetailCardModelTest {

    @Test
    fun `full OFF snapshot maps into independent card sections`() {
        val card = fullProduct().toDetailCardModel()

        assertEquals(6, card.facts.size)
        assertEquals("Oats", card.ingredients)
        assertEquals(listOf("Gluten"), card.allergens)
        assertEquals(2, card.nutrients.size)
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
    fun `zero nutrition is omitted without showing a database status`() {
        val card = fullProduct().copy(nutrients = listOf(Nutrient("Energy", "0", "kcal"))).toDetailCardModel()

        assertTrue(card.nutrients.isEmpty())
    }

    @Test
    fun `invalid product fields are omitted from the detail card`() {
        val card = fullProduct().copy(
            details = ProductDetails(
                brand = " null ",
                nutriScore = "unknown",
                novaGroup = 7,
                ecoScore = "?",
                imageUrl = "http://example.test/oats.jpg",
                allergens = listOf("", " null "),
            ),
        ).toDetailCardModel()

        assertTrue(card.facts.isEmpty())
        assertTrue(card.allergens.isEmpty())
        assertEquals(null, card.imageUrl)
    }

    @Test
    fun `invalid nutrition values are omitted`() {
        val card = fullProduct().copy(
            nutrients = listOf(
                Nutrient("Energy", "NaN", "kcal"),
                Nutrient("Protein", "-2", "g"),
                Nutrient("", "12", "g"),
            ),
        ).toDetailCardModel()

        assertTrue(card.nutrients.isEmpty())
    }

    @Test
    fun `taxonomy prefixes are removed from product details`() {
        val card = fullProduct().copy(
            details = fullProduct().details.copy(
                allergens = listOf("en:gluten", "fr:oeufs"),
                categories = listOf("en:plant-based-foods", "en:oat-milks"),
                labels = listOf("en:organic", "en:no-added-sugar"),
            ),
        ).toDetailCardModel()

        assertEquals(listOf("Gluten", "Oeufs"), card.allergens)
        assertEquals(listOf("Plant Based Foods", "Oat Milks"), card.categories)
        assertEquals(listOf("Organic", "No Added Sugar"), card.labels)
    }

    @Test
    fun `traces additives countries origins and packaging reach the card`() {
        val card = fullProduct().copy(
            details = fullProduct().details.copy(
                traces = listOf("en:nuts"),
                additives = listOf("en:e330"),
                countries = listOf("en:france"),
                origins = listOf("en:brittany"),
                packaging = listOf(
                    ProductPackagingComponent(numberOfUnits = 1, quantityPerUnit = "500 g", material = "PET", shape = "Bottle", recycling = "Recycle"),
                ),
            ),
        ).toDetailCardModel()

        assertEquals(listOf("Nuts"), card.traces)
        assertEquals(listOf("E330"), card.additives)
        assertEquals(listOf("France"), card.countries)
        assertEquals(listOf("Brittany"), card.origins)
        assertEquals(1, card.packaging.size)
    }

    @Test
    fun `structured ingredients with a percent are summarized`() {
        val card = fullProduct().copy(
            details = fullProduct().details.copy(
                ingredients = listOf(
                    ProductIngredient(name = "Water", percentEstimate = 90.0),
                    ProductIngredient(name = "Carbon dioxide", percent = 0.5),
                ),
            ),
        ).toDetailCardModel()

        assertEquals("Water (90%), Carbon dioxide (0%)", card.ingredientsBreakdown)
    }

    @Test
    fun `the full nutrient set from details replaces the four headline nutrients`() {
        val card = fullProduct().copy(
            details = fullProduct().details.copy(
                nutrients = listOf(
                    Nutrient("Energy", "350", "kcal"),
                    Nutrient("Sugars", "5", "g"),
                    Nutrient("Salt", "1", "g"),
                ),
            ),
        ).toDetailCardModel()

        assertEquals(3, card.nutrients.size)
        assertEquals(listOf("Energy", "Sugars", "Salt"), card.nutrients.map(Nutrient::name))
    }

    @Test
    fun `nutrient levels food groups sourcing and extra photos reach the card`() {
        val card = fullProduct().copy(
            details = fullProduct().details.copy(
                nutrientLevels = mapOf("fat" to "High", "sugars" to "Low"),
                foodGroups = listOf("en:legumes"),
                manufacturingPlaces = listOf("en:romania"),
                purchasePlaces = listOf("en:france"),
                stores = listOf("en:carrefour"),
                ingredientsImageUrl = "https://example.test/oats-ingredients.jpg",
                nutritionImageUrl = "https://example.test/oats-nutrition.jpg",
                packagingImageUrl = "https://example.test/oats-packaging.jpg",
            ),
        ).toDetailCardModel()

        assertEquals(listOf("Fat" to "High", "Sugars" to "Low"), card.nutrientLevels)
        assertEquals(listOf("Legumes"), card.foodGroups)
        assertEquals(listOf("Romania"), card.manufacturingPlaces)
        assertEquals(listOf("France"), card.purchasePlaces)
        assertEquals(listOf("Carrefour"), card.stores)
        assertTrue(card.ingredientsImageUrl!!.startsWith("https://"))
        assertTrue(card.nutritionImageUrl!!.startsWith("https://"))
        assertTrue(card.packagingImageUrl!!.startsWith("https://"))
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
