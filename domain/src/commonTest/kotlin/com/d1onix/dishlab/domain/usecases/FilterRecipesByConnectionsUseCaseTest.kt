package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterRecipesByConnectionsUseCaseTest {

    @Test
    fun `allows ABC AB and BC but rejects AC when only AB and BC are connected`() {
        val a = ProductId("a")
        val b = ProductId("b")
        val c = ProductId("c")
        val recipes = listOf(
            recipe("abc", listOf(a, b, c)),
            recipe("ab", listOf(a, b)),
            recipe("bc", listOf(b, c)),
            recipe("ac", listOf(a, c)),
        )
        val connections = setOf(
            ProductConnection.between(a, b),
            ProductConnection.between(b, c),
        )

        val result = FilterRecipesByConnectionsUseCaseImpl()(
            recipes = recipes,
            sessionProductIds = listOf(a, b, c),
            connections = connections,
        )

        assertEquals(listOf("abc", "ab", "bc"), result.map { it.id.value })
    }

    private fun recipe(id: String, productIds: List<ProductId>) = Recipe(
        id = RecipeId(id),
        name = id,
        minutes = 10,
        difficulty = RecipeDifficulty.Easy,
        categories = emptyList(),
        productIds = productIds,
        description = "",
        ingredients = emptyList(),
        steps = emptyList(),
    )
}
