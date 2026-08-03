package com.dishlab.infrastructure.db

import com.dishlab.domain.model.CatalogIngredient
import com.dishlab.domain.model.CatalogNutrientValue
import com.dishlab.domain.model.CatalogPackagingComponent
import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductNutrition
import com.dishlab.domain.model.CatalogProductSource
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class StoredCatalogProduct(
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
    val nutritionGrade: String = "",
    val nutritionScore: Int? = null,
    val novaGroup: Int? = null,
    val environmentalScoreGrade: String = "",
    val environmentalScore: Int? = null,
    val ingredientsText: String = "",
    val ingredients: List<StoredIngredient> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val nutrition: StoredProductNutrition? = null,
    val packaging: List<StoredPackagingComponent> = emptyList(),
    val source: StoredProductSource? = null,
    val canonicalTags: List<String> = emptyList(),
    val foodConceptId: String? = null,
    val foodVariantId: String? = null,
)

@Serializable
internal data class StoredIngredient(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<StoredIngredient> = emptyList(),
)

@Serializable
internal data class StoredProductNutrition(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, StoredNutrientValue> = emptyMap(),
)

@Serializable
internal data class StoredNutrientValue(
    val value: Double? = null,
    val computedValue: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

@Serializable
internal data class StoredPackagingComponent(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

@Serializable
internal data class StoredProductSource(
    val provider: String,
    val schemaVersion: Int? = null,
    val revision: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
    val clientProvided: Boolean = false,
)

internal fun CatalogProduct.toStored(): StoredCatalogProduct = StoredCatalogProduct(
    barcode = barcode,
    name = name,
    brand = brand,
    genericName = genericName,
    language = language,
    category = category,
    categories = categories,
    labels = labels,
    countries = countries,
    origins = origins,
    unit = unit,
    quantity = quantity,
    servingSize = servingSize,
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
    imageUrl = imageUrl,
    imageFrontUrl = imageFrontUrl,
    packageQuantity = packageQuantity,
    nutritionGrade = nutritionGrade,
    nutritionScore = nutritionScore,
    novaGroup = novaGroup,
    environmentalScoreGrade = environmentalScoreGrade,
    environmentalScore = environmentalScore,
    ingredientsText = ingredientsText,
    ingredients = ingredients.map(CatalogIngredient::toStored),
    allergens = allergens,
    traces = traces,
    additives = additives,
    nutrition = nutrition?.toStored(),
    packaging = packaging.map(CatalogPackagingComponent::toStored),
    source = source?.toStored(),
    canonicalTags = canonicalTags,
    foodConceptId = foodConceptId?.toString(),
    foodVariantId = foodVariantId?.toString(),
)

internal fun StoredCatalogProduct.toDomain(): CatalogProduct = CatalogProduct(
    barcode = barcode,
    name = name,
    brand = brand,
    genericName = genericName,
    language = language,
    category = category,
    categories = categories,
    labels = labels,
    countries = countries,
    origins = origins,
    unit = unit,
    quantity = quantity,
    servingSize = servingSize,
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
    imageUrl = imageUrl,
    imageFrontUrl = imageFrontUrl,
    packageQuantity = packageQuantity,
    nutritionGrade = nutritionGrade,
    nutritionScore = nutritionScore,
    novaGroup = novaGroup,
    environmentalScoreGrade = environmentalScoreGrade,
    environmentalScore = environmentalScore,
    ingredientsText = ingredientsText,
    ingredients = ingredients.map(StoredIngredient::toDomain),
    allergens = allergens,
    traces = traces,
    additives = additives,
    nutrition = nutrition?.toDomain(),
    packaging = packaging.map(StoredPackagingComponent::toDomain),
    source = source?.toDomain(),
    canonicalTags = canonicalTags,
    foodConceptId = foodConceptId?.let(UUID::fromString),
    foodVariantId = foodVariantId?.let(UUID::fromString),
)

private fun CatalogIngredient.toStored(): StoredIngredient = StoredIngredient(
    id, text, percent, percentEstimate, vegan, vegetarian, fromPalmOil, ingredients.map(CatalogIngredient::toStored),
)

private fun StoredIngredient.toDomain(): CatalogIngredient = CatalogIngredient(
    id, text, percent, percentEstimate, vegan, vegetarian, fromPalmOil, ingredients.map(StoredIngredient::toDomain),
)

private fun CatalogProductNutrition.toStored(): StoredProductNutrition = StoredProductNutrition(
    per,
    preparation,
    nutrients.mapValues { (_, value) -> value.toStored() },
)

private fun StoredProductNutrition.toDomain(): CatalogProductNutrition = CatalogProductNutrition(
    per,
    preparation,
    nutrients.mapValues { (_, value) -> value.toDomain() },
)

private fun CatalogNutrientValue.toStored(): StoredNutrientValue =
    StoredNutrientValue(value, computedValue, unit, source, sourcePer)

private fun StoredNutrientValue.toDomain(): CatalogNutrientValue =
    CatalogNutrientValue(value, computedValue, unit, source, sourcePer)

private fun CatalogPackagingComponent.toStored(): StoredPackagingComponent =
    StoredPackagingComponent(numberOfUnits, quantityPerUnit, material, shape, recycling)

private fun StoredPackagingComponent.toDomain(): CatalogPackagingComponent =
    CatalogPackagingComponent(numberOfUnits, quantityPerUnit, material, shape, recycling)

private fun CatalogProductSource.toStored(): StoredProductSource =
    StoredProductSource(provider, schemaVersion, revision, sourceUpdatedAtEpochSeconds, clientProvided)

private fun StoredProductSource.toDomain(): CatalogProductSource =
    CatalogProductSource(provider, schemaVersion, revision, sourceUpdatedAtEpochSeconds, clientProvided)
