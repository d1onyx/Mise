package com.dishlab.api.dto

import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductPage
import com.dishlab.domain.model.CatalogIngredient
import com.dishlab.domain.model.CatalogNutrientValue
import com.dishlab.domain.model.CatalogPackagingComponent
import com.dishlab.domain.model.CatalogProductNutrition
import com.dishlab.domain.model.CatalogProductSource
import com.dishlab.application.service.NormalizedProductName
import com.dishlab.application.service.ProductNormalizationInput
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class CatalogProductResponse(
    val barcode: String,
    val name: String,
    val brand: String,
    val emoji: String,
    val category: String,
    val categories: List<String>,
    val unit: String,
    val calories: String,
    val protein: String,
    val fat: String,
    val carbs: String,
    val imageUrl: String,
    val packageQuantity: Int,
    val nutritionGrade: String,
    val canonicalTags: List<String>,
    val genericName: String = "",
    val language: String = "",
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val origins: List<String> = emptyList(),
    val quantity: String = "",
    val servingSize: String = "",
    val nutritionScore: Int? = null,
    val novaGroup: Int? = null,
    val environmentalScoreGrade: String = "",
    val environmentalScore: Int? = null,
    val ingredientsText: String = "",
    val ingredients: List<CatalogIngredientResponse> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val nutrition: CatalogProductNutritionResponse? = null,
    val packaging: List<CatalogPackagingComponentResponse> = emptyList(),
    val source: CatalogProductSourceResponse? = null,
    val foodConceptId: String? = null,
    val foodVariantId: String? = null,
)

@Serializable
data class CatalogIngredientResponse(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<CatalogIngredientResponse> = emptyList(),
)

@Serializable
data class CatalogProductNutritionResponse(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, CatalogNutrientValueResponse> = emptyMap(),
)

@Serializable
data class CatalogNutrientValueResponse(
    val value: Double? = null,
    val computedValue: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

@Serializable
data class CatalogPackagingComponentResponse(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

@Serializable
data class CatalogProductSourceResponse(
    val provider: String,
    val schemaVersion: Int? = null,
    val revision: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
    val clientProvided: Boolean = false,
)

@Serializable
data class CatalogProductPageResponse(
    val products: List<CatalogProductResponse>,
    val page: Int,
    val hasMore: Boolean,
)

@Serializable
data class ProductCategoriesResponse(val categories: List<String>)

@Serializable
data class CatalogDumpResponse(
    val version: String,
    val items: List<CatalogEntryResponse>,
)

@Serializable
data class CatalogEntryResponse(
    val name: String,
    val category: String,
)

@Serializable
data class NormalizeProductsRequest(val products: List<NormalizeProductRequest>)

@Serializable
data class NormalizeProductRequest(
    val name: String,
    val categories: List<String> = emptyList(),
)

@Serializable
data class NormalizeProductsResponse(val products: List<NormalizedProductResponse>)

@Serializable
data class NormalizedProductResponse(
    val originalName: String,
    val normalizedNames: List<String>,
    val canonicalTags: List<String>,
)

@Serializable
data class ValidateTagRequest(
    val tag: String,
    val productName: String = "",
)

@Serializable
data class ValidateTagResponse(
    val valid: Boolean,
    val tag: String,
    val reason: String? = null,
)

/** A bounded Open Food Facts snapshot fetched by the mobile device. */
@Serializable
data class ResolveClientProductRequest(
    val barcode: String,
    val name: String,
    val brand: String = "",
    val genericName: String = "",
    val language: String = "",
    val categories: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val origins: List<String> = emptyList(),
    val quantity: String = "",
    val productQuantity: Double? = null,
    val productQuantityUnit: String = "",
    val servingSize: String = "",
    val imageFrontUrl: String = "",
    val imageFrontSmallUrl: String = "",
    val nutritionGrade: String = "",
    val nutritionScore: Int? = null,
    val novaGroup: Int? = null,
    val environmentalScoreGrade: String = "",
    val environmentalScore: Int? = null,
    val ingredientsText: String = "",
    val ingredients: List<ClientIngredientRequest> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val nutrition: ClientProductNutritionRequest? = null,
    val packaging: List<ClientPackagingComponentRequest> = emptyList(),
    val sourceProvider: String = "open_food_facts",
    val sourceSchemaVersion: Int? = null,
    val sourceRevision: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
)

@Serializable
data class ClientIngredientRequest(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<ClientIngredientRequest> = emptyList(),
)

@Serializable
data class ClientProductNutritionRequest(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, ClientNutrientValueRequest> = emptyMap(),
)

@Serializable
data class ClientNutrientValueRequest(
    val value: Double? = null,
    val computedValue: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

@Serializable
data class ClientPackagingComponentRequest(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

@Serializable
data class SaveCatalogProductRequest(
    val barcode: String,
    val name: String,
    val emoji: String = "🥫",
    val category: String = "",
    val unit: String = "шт",
    val calories: String = "",
    val keywords: List<String> = emptyList(),
)

fun SaveCatalogProductRequest.toDomain(): CatalogProduct = CatalogProduct(
    barcode = barcode,
    name = name,
    category = category,
    categories = keywords,
    unit = unit,
    calories = calories.toDoubleOrNull(),
)

fun ResolveClientProductRequest.toDomain(): CatalogProduct {
    val categoryNames = categories
        .map { it.removePrefix("en:").replace('-', ' ').replaceFirstChar(Char::uppercaseChar) }
    val nutrients = nutrition?.nutrients.orEmpty()
    return CatalogProduct(
        barcode = barcode.filter(Char::isDigit),
        name = name.trim(),
        brand = brand.trim(),
        genericName = genericName.trim(),
        language = language.trim(),
        category = categoryNames.lastOrNull().orEmpty(),
        categories = categoryNames,
        labels = labels,
        countries = countries,
        origins = origins,
        unit = productQuantityUnit.lowercase().ifBlank { parseQuantityUnit(quantity) },
        quantity = quantity,
        servingSize = servingSize,
        calories = nutrients["energy-kcal"]?.value,
        protein = nutrients["proteins"]?.value,
        fat = nutrients["fat"]?.value,
        carbs = nutrients["carbohydrates"]?.value,
        imageUrl = imageFrontSmallUrl.ifBlank { imageFrontUrl },
        imageFrontUrl = imageFrontUrl,
        packageQuantity = productQuantity?.toInt() ?: 0,
        nutritionGrade = nutritionGrade.lowercase(),
        nutritionScore = nutritionScore,
        novaGroup = novaGroup,
        environmentalScoreGrade = environmentalScoreGrade.lowercase(),
        environmentalScore = environmentalScore,
        ingredientsText = ingredientsText,
        ingredients = ingredients.map(ClientIngredientRequest::toDomain),
        allergens = allergens,
        traces = traces,
        additives = additives,
        nutrition = nutrition?.toDomain(),
        packaging = packaging.map(ClientPackagingComponentRequest::toDomain),
        source = CatalogProductSource(
            provider = sourceProvider,
            schemaVersion = sourceSchemaVersion,
            revision = sourceRevision,
            sourceUpdatedAtEpochSeconds = sourceUpdatedAtEpochSeconds,
            clientProvided = true,
        ),
    )
}

fun CatalogProduct.toResponse(): CatalogProductResponse = CatalogProductResponse(
    barcode = barcode,
    name = listOf(name, brand).filter(String::isNotBlank).joinToString(" — "),
    brand = brand,
    emoji = category.toEmoji(),
    category = category,
    categories = categories,
    unit = unit,
    calories = calories.display(),
    protein = protein.display(),
    fat = fat.display(),
    carbs = carbs.display(),
    imageUrl = imageUrl,
    packageQuantity = packageQuantity,
    nutritionGrade = nutritionGrade,
    canonicalTags = canonicalTags.filter { it.startsWith("en:") }.distinct(),
    genericName = genericName,
    language = language,
    labels = labels,
    countries = countries,
    origins = origins,
    quantity = quantity,
    servingSize = servingSize,
    nutritionScore = nutritionScore,
    novaGroup = novaGroup,
    environmentalScoreGrade = environmentalScoreGrade,
    environmentalScore = environmentalScore,
    ingredientsText = ingredientsText,
    ingredients = ingredients.map(CatalogIngredient::toResponse),
    allergens = allergens,
    traces = traces,
    additives = additives,
    nutrition = nutrition?.toResponse(),
    packaging = packaging.map(CatalogPackagingComponent::toResponse),
    source = source?.toResponse(),
    foodConceptId = foodConceptId?.toString(),
    foodVariantId = foodVariantId?.toString(),
)

fun CatalogProductPage.toResponse(): CatalogProductPageResponse =
    CatalogProductPageResponse(items.map(CatalogProduct::toResponse), page, hasMore)

fun NormalizeProductRequest.toDomain(): ProductNormalizationInput =
    ProductNormalizationInput(name, categories)

fun NormalizedProductName.toResponse(): NormalizedProductResponse =
    NormalizedProductResponse(originalName, normalizedNames, canonicalTags)

private fun Double?.display(): String = this?.roundToInt()?.toString().orEmpty()

private fun ClientIngredientRequest.toDomain(): CatalogIngredient = CatalogIngredient(
    id = id,
    text = text,
    percent = percent,
    percentEstimate = percentEstimate,
    vegan = vegan,
    vegetarian = vegetarian,
    fromPalmOil = fromPalmOil,
    ingredients = ingredients.map(ClientIngredientRequest::toDomain),
)

private fun ClientProductNutritionRequest.toDomain(): CatalogProductNutrition = CatalogProductNutrition(
    per = per,
    preparation = preparation,
    nutrients = nutrients.mapValues { (_, nutrient) ->
        CatalogNutrientValue(
            value = nutrient.value,
            computedValue = nutrient.computedValue,
            unit = nutrient.unit,
            source = nutrient.source,
            sourcePer = nutrient.sourcePer,
        )
    },
)

private fun ClientPackagingComponentRequest.toDomain(): CatalogPackagingComponent = CatalogPackagingComponent(
    numberOfUnits = numberOfUnits,
    quantityPerUnit = quantityPerUnit,
    material = material,
    shape = shape,
    recycling = recycling,
)

private fun CatalogIngredient.toResponse(): CatalogIngredientResponse = CatalogIngredientResponse(
    id = id,
    text = text,
    percent = percent,
    percentEstimate = percentEstimate,
    vegan = vegan,
    vegetarian = vegetarian,
    fromPalmOil = fromPalmOil,
    ingredients = ingredients.map(CatalogIngredient::toResponse),
)

private fun CatalogProductNutrition.toResponse(): CatalogProductNutritionResponse = CatalogProductNutritionResponse(
    per = per,
    preparation = preparation,
    nutrients = nutrients.mapValues { (_, nutrient) ->
        CatalogNutrientValueResponse(
            value = nutrient.value,
            computedValue = nutrient.computedValue,
            unit = nutrient.unit,
            source = nutrient.source,
            sourcePer = nutrient.sourcePer,
        )
    },
)

private fun CatalogPackagingComponent.toResponse(): CatalogPackagingComponentResponse =
    CatalogPackagingComponentResponse(numberOfUnits, quantityPerUnit, material, shape, recycling)

private fun CatalogProductSource.toResponse(): CatalogProductSourceResponse = CatalogProductSourceResponse(
    provider = provider,
    schemaVersion = schemaVersion,
    revision = revision,
    sourceUpdatedAtEpochSeconds = sourceUpdatedAtEpochSeconds,
    clientProvided = clientProvided,
)

private fun parseQuantityUnit(quantity: String): String =
    Regex("""[\d.,]+\s*([A-Za-zА-Яа-я]+)""").find(quantity)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()
        ?: "шт"

private fun String.toEmoji(): String {
    val category = lowercase()
    return when {
        category.contains("dairy") || category.contains("milk") || category.contains("cheese") -> "🥛"
        category.contains("meat") || category.contains("chicken") || category.contains("pork") -> "🥩"
        category.contains("fish") || category.contains("seafood") -> "🐟"
        category.contains("fruit") -> "🍎"
        category.contains("vegetable") -> "🥦"
        category.contains("beverage") || category.contains("drink") -> "🥤"
        category.contains("bread") || category.contains("cereal") || category.contains("pasta") -> "🍞"
        category.contains("egg") -> "🥚"
        category.contains("chocolate") || category.contains("sweet") -> "🍫"
        category.contains("oil") || category.contains("butter") -> "🧈"
        category.contains("coffee") || category.contains("tea") -> "☕"
        category.contains("water") -> "💧"
        else -> "🥫"
    }
}
