package com.d1onix.dishlab.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackendProductMapperTest {

    @Test
    fun `Open Food Facts product preserves barcode and maps Nutri-Score`() {
        val product = BackendProductDto(
            barcode = "4820000000001",
            name = "Natural yogurt",
            brand = "DishLab Farm",
            category = "Dairy",
            calories = "61.4",
            protein = "3.5",
            fat = "3.2",
            carbs = "4.7",
            nutritionGrade = "b",
        ).toDomain()

        assertEquals("barcode:4820000000001", product.id.value)
        assertEquals(75, product.score)
        assertEquals(listOf("Energy", "Carbs", "Protein", "Fat"), product.nutrients.map { it.name })
        assertTrue(product.hasCompleteData)
        assertTrue(product.summary.contains("Nutri-Score B"))
    }

    @Test
    fun `missing nutrition data remains usable with a neutral score`() {
        val product = BackendProductDto(
            barcode = "5900000000002",
            name = "Product",
            nutritionGrade = "unknown",
        ).toDomain()

        assertEquals(50, product.score)
        assertEquals("Product", product.category)
        assertFalse(product.hasCompleteData)
    }
}
