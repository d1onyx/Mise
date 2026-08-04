package com.d1onix.dishlab.domain

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphPantryQueryTest {
    private fun product(id: String, vararg tags: String) = Product(ProductId(id), id, id, "", 0, 0, id, emptyList(), "", true, emptyList(), tags.toList())
    @Test fun `one node becomes ingredient`() = assertEquals(PantryMatchQuery(emptyList(), listOf("en:a")), translateGraphToPantryQuery(listOf(product("a", "en:a")), emptySet()))
    @Test fun `connected pair becomes exact group`() = assertEquals(PantryMatchQuery(listOf(listOf("en:a", "en:b")), emptyList()), translateGraphToPantryQuery(listOf(product("a", "en:a"), product("b", "en:b")), setOf(ProductConnection.between(ProductId("a"), ProductId("b")))))
    @Test fun `connected pair plus single keeps and or semantics`() = assertEquals(PantryMatchQuery(listOf(listOf("en:a", "en:b")), listOf("en:c")), translateGraphToPantryQuery(listOf(product("a", "en:a"), product("b", "en:b"), product("c", "en:c")), setOf(ProductConnection.between(ProductId("a"), ProductId("b")))))
    @Test fun `component merges synonym tags`() = assertEquals(PantryMatchQuery(listOf(listOf("en:a", "en:aa", "en:b")), emptyList()), translateGraphToPantryQuery(listOf(product("a", "en:a", "en:aa"), product("b", "en:b")), setOf(ProductConnection.between(ProductId("a"), ProductId("b")))))
}
