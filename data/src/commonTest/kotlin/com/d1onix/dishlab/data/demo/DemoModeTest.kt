package com.d1onix.dishlab.data.demo

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DemoModeTest {

    private val catalogue = listOf(product("oats"), product("banana"), product("honey"))

    @Test
    fun `an unknown barcode still resolves to something`() {
        assertNotNull(catalogue.resolveDemoBarcode("4006381333931"))
        assertNotNull(catalogue.resolveDemoBarcode("not-a-barcode"))
    }

    @Test
    fun `the same barcode always resolves to the same product`() {
        val first = catalogue.resolveDemoBarcode("4006381333931")
        val second = catalogue.resolveDemoBarcode("4006381333931")
        assertEquals(first, second)
    }

    @Test
    fun `the reserved barcode keeps the not-found screen reachable`() {
        assertNull(catalogue.resolveDemoBarcode(DemoMode.NOT_FOUND_BARCODE))
    }

    @Test
    fun `an empty catalogue resolves to nothing`() {
        assertNull(emptyList<Product>().resolveDemoBarcode("111"))
    }

    private fun product(id: String) = Product(
        id = ProductId(id),
        barcode = id,
        name = id,
        category = "Test",
        score = 50,
        accentColor = 0xFFC8FF4D,
        initial = id.first().uppercase(),
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    )
}
