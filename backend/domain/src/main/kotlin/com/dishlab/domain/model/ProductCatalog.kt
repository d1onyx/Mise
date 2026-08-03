package com.dishlab.domain.model

import java.util.UUID

data class CatalogProduct(
    val barcode: String,
    val name: String,
    val brand: String = "",
    val genericName: String = "",
    val language: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val origins: List<String> = emptyList(),
    val unit: String = "шт",
    val quantity: String = "",
    val servingSize: String = "",
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val imageUrl: String = "",
    val imageFrontUrl: String = "",
    val packageQuantity: Int = 0,
    /** Open Food Facts Nutri-Score grade (`a` through `e`) when the source provides one. */
    val nutritionGrade: String = "",
    val nutritionScore: Int? = null,
    val novaGroup: Int? = null,
    val environmentalScoreGrade: String = "",
    val environmentalScore: Int? = null,
    val ingredientsText: String = "",
    val ingredients: List<CatalogIngredient> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val nutrition: CatalogProductNutrition? = null,
    val packaging: List<CatalogPackagingComponent> = emptyList(),
    val source: CatalogProductSource? = null,
    val canonicalTags: List<String> = emptyList(),
    val foodConceptId: UUID? = null,
    val foodVariantId: UUID? = null,
)

data class CatalogIngredient(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<CatalogIngredient> = emptyList(),
)

data class CatalogProductNutrition(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, CatalogNutrientValue> = emptyMap(),
)

data class CatalogNutrientValue(
    val value: Double? = null,
    val computedValue: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

data class CatalogPackagingComponent(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

data class CatalogProductSource(
    val provider: String,
    val schemaVersion: Int? = null,
    val revision: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
    /** Client payloads are useful source snapshots, but not trusted safety attestations. */
    val clientProvided: Boolean = false,
)

data class CatalogProductPage(
    val items: List<CatalogProduct>,
    val page: Int,
    val hasMore: Boolean,
)
