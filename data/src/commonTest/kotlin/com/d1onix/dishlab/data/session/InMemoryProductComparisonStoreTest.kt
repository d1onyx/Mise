package com.d1onix.dishlab.data.session

import com.d1onix.dishlab.domain.model.ProductId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryProductComparisonStoreTest {
    @Test
    fun `comparison accepts four unique products and rejects the fifth`() = runTest {
        val store = InMemoryProductComparisonStore()

        repeat(4) { index ->
            assertTrue(store.add(ProductId("product-$index")))
        }

        assertFalse(store.add(ProductId("product-4")))
        assertEquals(4, store.products.value.size)
    }

    @Test
    fun `adding the same product is idempotent`() = runTest {
        val store = InMemoryProductComparisonStore()
        val product = ProductId("same")

        assertTrue(store.add(product))
        assertTrue(store.add(product))

        assertEquals(listOf(product), store.products.value)
    }
}
