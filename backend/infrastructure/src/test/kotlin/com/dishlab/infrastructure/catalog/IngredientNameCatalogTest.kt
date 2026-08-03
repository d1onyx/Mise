package com.dishlab.infrastructure.catalog

import com.dishlab.application.service.ProductNormalizationInput
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IngredientNameCatalogTest {
    private val catalog = IngredientNameCatalog.load(
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .flatMap { directory ->
                sequenceOf(
                    File(directory, "data/ingredients_by_category.json"),
                    File(directory, "ingredients_by_category.json"),
                )
            }
            .first(File::isFile)
            .toPath(),
    )

    @Test
    fun `catalog contains the canonical salt names used by recipes`() {
        assertTrue(catalog.contains("salt"))
        assertTrue(catalog.contains("sea salt"))
        assertEquals("salt", catalog.resolve("salts"))
    }

    @Test
    fun `local normalizer maps scanned category to recipe ingredient`() = runBlocking {
        val result = CatalogProductNameNormalizer(catalog).normalize(
            listOf(
                ProductNormalizationInput(
                    name = "Сіль кухонна — Торчин",
                    categories = listOf("Foods", "Condiments", "Salts"),
                ),
            ),
        ).single()

        assertEquals(listOf("salt"), result.normalizedNames)
        assertEquals(listOf("en:salt"), result.canonicalTags)
    }

    @Test
    fun `specific salt expands to compatible generic recipe names`() {
        assertEquals(
            listOf("table salt", "salt"),
            catalog.relatedNames("table salt"),
        )
        assertEquals(
            listOf("fine sea salt", "sea salt", "fine salt", "salt"),
            catalog.relatedNames("fine sea salt"),
        )
    }
}
