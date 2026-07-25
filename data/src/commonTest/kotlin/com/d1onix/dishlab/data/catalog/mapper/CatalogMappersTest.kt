package com.d1onix.dishlab.data.catalog.mapper

import com.d1onix.dishlab.data.catalog.dto.ProductDto
import com.d1onix.dishlab.data.catalog.dto.RecipeDto
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.ScoreVerdict
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Guards the shape of the bundled catalogue against the domain model. */
class CatalogMappersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a product decodes into the domain model`() {
        val dto = json.decodeFromString<ProductDto>(
            """
            {
              "id": "honey",
              "barcode": "3175681800014",
              "name": "Honey",
              "category": "Sweetener",
              "score": 44,
              "accentColor": "#FFC24E",
              "initial": "H",
              "summary": "Almost pure sugar.",
              "hasCompleteData": false,
              "nutrients": [{ "name": "Sugar", "amount": "82", "unit": "g" }],
              "alternatives": [{ "name": "Date syrup", "score": 60 }]
            }
            """.trimIndent()
        )

        val product = dto.toDomain()

        assertEquals("honey", product.id.value)
        assertEquals(ScoreVerdict.Skip, product.verdict)
        assertEquals(false, product.hasCompleteData)
        assertEquals(1, product.nutrients.size)
        assertEquals(ScoreVerdict.Maybe, product.alternatives.single().verdict)
    }

    @Test
    fun `hex colours become opaque ARGB`() {
        assertEquals(0xFFC8FF4DL, "#C8FF4D".toArgb())
        assertEquals(0xFF000000L, "#000000".toArgb())
    }

    @Test
    fun `a recipe decodes with its steps and timers`() {
        val dto = json.decodeFromString<RecipeDto>(
            """
            {
              "id": "bowl",
              "name": "Banana Oat Bowl",
              "minutes": 10,
              "difficulty": "Easy",
              "categories": ["Breakfast"],
              "productIds": ["oats", "banana"],
              "description": "Creamy oats.",
              "ingredients": [{ "quantity": "50 g", "name": "Rolled oats" }],
              "steps": [
                { "title": "Simmer", "description": "Simmer the oats", "timerSeconds": 120 },
                { "title": "Serve", "description": "Plate it" }
              ]
            }
            """.trimIndent()
        )

        val recipe = dto.toDomain()

        assertEquals(RecipeDifficulty.Easy, recipe.difficulty)
        assertEquals(listOf("oats", "banana"), recipe.productIds.map { it.value })
        assertEquals(120, recipe.steps.first().timerSeconds)
        assertNull(recipe.steps.last().timerSeconds)
    }

    @Test
    fun `an unknown difficulty falls back instead of crashing`() {
        val dto = RecipeDto(
            id = "x",
            name = "X",
            minutes = 1,
            difficulty = "Impossible",
            categories = emptyList(),
            productIds = emptyList(),
            description = "",
            ingredients = emptyList(),
            steps = emptyList(),
        )

        assertEquals(RecipeDifficulty.Easy, dto.toDomain().difficulty)
    }
}
