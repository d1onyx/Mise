package com.dishlab.application.service

import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.FoodCarbonation
import com.dishlab.domain.model.FoodConcept
import com.dishlab.domain.model.FoodOrigin
import com.dishlab.domain.model.FoodPreparationState
import com.dishlab.domain.model.FoodVariant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ProductCanonicalizationServiceTest {

    @Test
    fun `sparkling water keeps water concept and stores carbonation on variant`() = runBlocking {
        val taxonomy = RecordingTaxonomyRepository()
        val canonicalizer = canonicalizer(taxonomy) { listOf("lightly sparkling water", "water") }

        val product = canonicalizer.canonicalize(
            CatalogProduct(
                barcode = "4009900552387",
                name = "Artesie jemne perliva",
                categories = listOf("en:waters", "en:lightly-sparkling-water"),
                language = "cs",
            ),
        )

        assertEquals("water", taxonomy.concepts.single().canonicalName)
        assertEquals("lightly sparkling water", taxonomy.variants.single().canonicalName)
        assertEquals(FoodCarbonation.LIGHTLY_SPARKLING, taxonomy.variants.single().carbonation)
        assertEquals(FoodOrigin.MINERAL, taxonomy.variants.single().origin)
        assertEquals(taxonomy.concepts.single().id, product.foodConceptId)
        assertEquals(taxonomy.variants.single().id, product.foodVariantId)
    }

    @Test
    fun `animal meat and plant based meat do not collapse into one concept`() = runBlocking {
        val taxonomy = RecordingTaxonomyRepository()
        val canonicalizer = canonicalizer(taxonomy) { product ->
            if (product.name.startsWith("Plant")) listOf("plant based meat", "meat")
            else listOf("raw chicken meat", "chicken meat", "meat")
        }

        val animal = canonicalizer.canonicalize(
            CatalogProduct("11111111", "Raw chicken breast", categories = listOf("en:meats", "en:chicken")),
        )
        val plant = canonicalizer.canonicalize(
            CatalogProduct("22222222", "Plant burger", categories = listOf("en:plant-based-meats")),
        )

        val animalVariant = taxonomy.variants.first { it.id == animal.foodVariantId }
        val plantVariant = taxonomy.variants.first { it.id == plant.foodVariantId }
        assertNotEquals(animal.foodConceptId, plant.foodConceptId)
        assertEquals("chicken meat", taxonomy.concepts.first { it.id == animal.foodConceptId }.canonicalName)
        assertEquals("plant based meat", taxonomy.concepts.first { it.id == plant.foodConceptId }.canonicalName)
        assertEquals(FoodOrigin.ANIMAL, animalVariant.origin)
        assertEquals(FoodOrigin.PLANT, plantVariant.origin)
        assertEquals(FoodPreparationState.RAW, animalVariant.preparationState)
    }

    private fun canonicalizer(
        taxonomy: RecordingTaxonomyRepository,
        names: (ProductNormalizationInput) -> List<String>,
    ): ProductCanonicalizationService = ProductCanonicalizationService(
        normalizer = object : ProductNameNormalizer {
            override suspend fun normalize(
                products: List<ProductNormalizationInput>,
            ): List<NormalizedProductName> = products.map { NormalizedProductName(it.name, names(it)) }
        },
        taxonomy = taxonomy,
    )

    private class RecordingTaxonomyRepository : FoodTaxonomyRepository {
        val concepts = mutableListOf<FoodConcept>()
        val variants = mutableListOf<FoodVariant>()
        val aliases = mutableListOf<FoodAlias>()

        override fun upsertConcept(canonicalName: String, origin: FoodOrigin): FoodConcept =
            concepts.firstOrNull { it.canonicalName == canonicalName }
                ?: FoodConcept(UUID.randomUUID(), canonicalName, origin = origin).also(concepts::add)

        override fun saveAlias(alias: FoodAlias) {
            aliases += alias
        }

        override fun upsertVariant(variant: FoodVariant): FoodVariant =
            variants.firstOrNull {
                it.conceptId == variant.conceptId &&
                    it.canonicalName == variant.canonicalName &&
                    it.origin == variant.origin &&
                    it.preparationState == variant.preparationState &&
                    it.carbonation == variant.carbonation
            } ?: variant.also(variants::add)
    }
}
