package com.dishlab.application.service

import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.FoodCarbonation
import com.dishlab.domain.model.FoodOrigin
import com.dishlab.domain.model.FoodPhysicalForm
import com.dishlab.domain.model.FoodPreparationState
import com.dishlab.domain.model.FoodPreservation
import com.dishlab.domain.model.FoodVariant
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

class ProductCanonicalizationService(
    private val normalizer: ProductNameNormalizer,
    private val taxonomy: FoodTaxonomyRepository,
) {
    suspend fun canonicalize(product: CatalogProduct): CatalogProduct {
        val normalized = normalize(
            listOf(ProductNormalizationInput(name = product.name, categories = product.categories)),
        ).firstOrNull() ?: return product
        val names = normalized.normalizedNames.map(::normalizeName).filter(String::isNotBlank).distinct()
        val canonicalTags = normalized.canonicalTags.ifEmpty { product.canonicalTags }
        val conceptName = selectConceptName(names) ?: return product.copy(canonicalTags = canonicalTags)
        val variantName = names.firstOrNull() ?: conceptName
        val evidence = product.evidenceText(names)
        val origin = inferOrigin(evidence)
        val concept = taxonomy.upsertConcept(conceptName, origin)
        product.aliases(names).forEach { (language, alias, source, confidence) ->
            taxonomy.saveAlias(
                FoodAlias(
                    conceptId = concept.id,
                    language = language,
                    value = alias,
                    normalizedValue = normalizeName(alias),
                    source = source,
                    confidence = confidence,
                ),
            )
        }
        val variant = taxonomy.upsertVariant(
            FoodVariant(
                id = UUID.randomUUID(),
                conceptId = concept.id,
                canonicalName = variantName,
                origin = origin,
                preparationState = inferPreparationState(evidence),
                physicalForm = inferPhysicalForm(evidence),
                carbonation = inferCarbonation(evidence, conceptName),
                preservation = inferPreservation(evidence),
                facets = buildMap {
                    product.language.takeIf(String::isNotBlank)?.let { put("source_language", it) }
                    product.novaGroup?.let { put("nova_group", it.toString()) }
                },
            ),
        )
        return product.copy(
            canonicalTags = canonicalTags,
            foodConceptId = concept.id,
            foodVariantId = variant.id,
        )
    }

    suspend fun normalize(products: List<ProductNormalizationInput>): List<NormalizedProductName> =
        normalizer.normalize(products)

    private fun selectConceptName(names: List<String>): String? {
        val specific = names.firstOrNull() ?: return null
        val stripped = specific
            .replace(LeadingVariantQualifiers, "")
            .replace(RepeatedSpaces, " ")
            .trim()
        return stripped.takeIf(String::isNotBlank) ?: names.lastOrNull()
    }

    private fun CatalogProduct.evidenceText(normalizedNames: List<String>): String = buildList {
        add(name)
        add(genericName)
        addAll(categories)
        addAll(labels)
        addAll(origins)
        addAll(ingredients.map { it.id })
        addAll(normalizedNames)
    }.joinToString(" ").lowercase()

    private fun CatalogProduct.aliases(
        normalizedNames: List<String>,
    ): List<AliasCandidate> = buildList {
        name.takeIf(String::isNotBlank)?.let { add(AliasCandidate(language.ifBlank { "und" }, it, "product_name", 1.0)) }
        genericName.takeIf(String::isNotBlank)?.let { add(AliasCandidate(language.ifBlank { "und" }, it, "generic_name", 1.0)) }
        normalizedNames.forEach { add(AliasCandidate("en", it, "canonicalizer", 0.9)) }
    }.distinctBy { "${it.language}:${normalizeName(it.value)}" }

    private fun inferOrigin(evidence: String): FoodOrigin = when {
        evidence.containsAny("plant-based-meat", "vegan-meat", "meat-alternative") -> FoodOrigin.PLANT
        evidence.containsAny("chicken", "beef", "pork", "animal-meat", "meats") -> FoodOrigin.ANIMAL
        evidence.containsAny("mushroom", "fungi") -> FoodOrigin.FUNGI
        evidence.containsAny("mineral-water", "waters", "water") -> FoodOrigin.MINERAL
        else -> FoodOrigin.UNKNOWN
    }

    private fun inferPreparationState(evidence: String): FoodPreparationState = when {
        evidence.containsAny("raw-", "raw ", "uncooked") -> FoodPreparationState.RAW
        evidence.containsAny("boiled", "cooked-in-water") -> FoodPreparationState.BOILED
        evidence.containsAny("baked") -> FoodPreparationState.BAKED
        evidence.containsAny("roasted") -> FoodPreparationState.ROASTED
        evidence.containsAny("fried") -> FoodPreparationState.FRIED
        evidence.containsAny("smoked") -> FoodPreparationState.SMOKED
        evidence.containsAny("fermented") -> FoodPreparationState.FERMENTED
        evidence.containsAny("dried", "dehydrated") -> FoodPreparationState.DRIED
        evidence.containsAny("cooked") -> FoodPreparationState.COOKED
        else -> FoodPreparationState.AS_SOLD
    }

    private fun inferPhysicalForm(evidence: String): FoodPhysicalForm = when {
        evidence.containsAny("minced", "ground-meat") -> FoodPhysicalForm.MINCED
        evidence.containsAny("ground", "powdered") -> FoodPhysicalForm.GROUND
        evidence.containsAny("powder") -> FoodPhysicalForm.POWDER
        evidence.containsAny("sliced", "slices") -> FoodPhysicalForm.SLICED
        evidence.containsAny("puree", "purée") -> FoodPhysicalForm.PUREE
        evidence.containsAny("beverages", "waters", "liquid") -> FoodPhysicalForm.LIQUID
        else -> FoodPhysicalForm.UNKNOWN
    }

    private fun inferCarbonation(evidence: String, conceptName: String): FoodCarbonation = when {
        evidence.containsAny("highly-sparkling", "strongly-carbonated") -> FoodCarbonation.HIGHLY_SPARKLING
        evidence.containsAny("lightly-sparkling", "lightly sparkling", "slightly-carbonated") -> FoodCarbonation.LIGHTLY_SPARKLING
        evidence.containsAny("sparkling-water", "sparkling water", "carbonated-water", "carbonated") -> FoodCarbonation.SPARKLING
        evidence.containsAny("still-water", "still water", "non-carbonated-water") -> FoodCarbonation.STILL
        conceptName.contains("water") -> FoodCarbonation.UNKNOWN
        else -> FoodCarbonation.NOT_APPLICABLE
    }

    private fun inferPreservation(evidence: String): FoodPreservation = when {
        evidence.containsAny("frozen") -> FoodPreservation.FROZEN
        evidence.containsAny("chilled", "refrigerated") -> FoodPreservation.CHILLED
        evidence.containsAny("canned") -> FoodPreservation.CANNED
        evidence.containsAny("pickled") -> FoodPreservation.PICKLED
        evidence.containsAny("shelf-stable") -> FoodPreservation.SHELF_STABLE
        evidence.containsAny("fresh") -> FoodPreservation.FRESH
        else -> FoodPreservation.UNKNOWN
    }

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)

    private fun normalizeName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(NonAlphaNumeric, " ")
        .replace(RepeatedSpaces, " ")
        .trim()

    private data class AliasCandidate(
        val language: String,
        val value: String,
        val source: String,
        val confidence: Double,
    )

    private companion object {
        val CombiningMarks = Regex("\\p{M}+")
        val NonAlphaNumeric = Regex("[^\\p{L}\\p{N}]+")
        val RepeatedSpaces = Regex("\\s+")
        val LeadingVariantQualifiers = Regex(
            "^(?:(?:raw|cooked|boiled|baked|roasted|fried|smoked|dried|frozen|chilled|" +
                "lightly sparkling|highly sparkling|sparkling|still)\\s+)+",
        )
    }
}
