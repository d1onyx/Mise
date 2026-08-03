package com.dishlab.application.service

import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.FoodConcept
import com.dishlab.domain.model.FoodOrigin
import com.dishlab.domain.model.FoodVariant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface ProductCatalogRepository {
    fun findByBarcode(barcode: String): CatalogProduct?
    fun save(product: CatalogProduct): CatalogProduct
}

class InMemoryProductCatalogRepository : ProductCatalogRepository {
    private val products = ConcurrentHashMap<String, CatalogProduct>()

    override fun findByBarcode(barcode: String): CatalogProduct? = products[barcode.filter(Char::isDigit)]

    override fun save(product: CatalogProduct): CatalogProduct {
        products[product.barcode.filter(Char::isDigit)] = product
        return product
    }
}

data class FoodAlias(
    val conceptId: UUID,
    val language: String,
    val value: String,
    val normalizedValue: String,
    val source: String,
    val confidence: Double,
)

interface FoodTaxonomyRepository {
    fun upsertConcept(canonicalName: String, origin: FoodOrigin): FoodConcept
    fun saveAlias(alias: FoodAlias)
    fun upsertVariant(variant: FoodVariant): FoodVariant
}

class InMemoryFoodTaxonomyRepository : FoodTaxonomyRepository {
    private val concepts = ConcurrentHashMap<String, FoodConcept>()
    private val variants = ConcurrentHashMap<String, FoodVariant>()
    private val aliases = ConcurrentHashMap<String, FoodAlias>()

    override fun upsertConcept(
        canonicalName: String,
        origin: FoodOrigin,
    ): FoodConcept = concepts.compute(canonicalName.lowercase()) { _, existing ->
        existing?.copy(origin = existing.origin.takeUnless { it == FoodOrigin.UNKNOWN } ?: origin)
            ?: FoodConcept(UUID.randomUUID(), canonicalName, origin = origin)
    }!!

    override fun saveAlias(alias: FoodAlias) {
        aliases["${alias.conceptId}:${alias.language}:${alias.normalizedValue}"] = alias
    }

    override fun upsertVariant(variant: FoodVariant): FoodVariant {
        val key = listOf(
            variant.conceptId,
            variant.canonicalName.lowercase(),
            variant.origin,
            variant.preparationState,
            variant.physicalForm,
            variant.carbonation,
            variant.preservation,
            variant.facets.toSortedMap(),
        ).joinToString("|")
        return variants.putIfAbsent(key, variant) ?: variant
    }
}
