package com.d1onix.dishlab.feature.products.presentation.comparison

import com.d1onix.dishlab.domain.model.ProductId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComparisonBestPolicyTest {
    private val first = ProductId("first")
    private val second = ProductId("second")

    @Test
    fun `higher DishLab score wins`() {
        assertEquals(
            second,
            ComparisonBestPolicy.highestNumeric(listOf(first to "68", second to "82")),
        )
    }

    @Test
    fun `A grade and lower NOVA group win`() {
        assertEquals(
            second,
            ComparisonBestPolicy.bestGrade(listOf(first to "D", second to "A")),
        )
        assertEquals(
            second,
            ComparisonBestPolicy.bestNovaGroup(listOf(first to "4", second to "1")),
        )
    }

    @Test
    fun `health nutrients apply their correct direction`() {
        assertEquals(
            second,
            ComparisonBestPolicy.bestNutrient("Sugars", listOf(first to "12", second to "3")),
        )
        assertEquals(
            first,
            ComparisonBestPolicy.bestNutrient("Protein", listOf(first to "12", second to "3")),
        )
    }

    @Test
    fun `equal values do not falsely claim a winner`() {
        assertNull(ComparisonBestPolicy.bestNutrient("Salt", listOf(first to "0.2", second to "0.2")))
    }
}
