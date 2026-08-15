package com.d1onix.dishlab.data.catalog.off

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class OpenFoodFactsProductResponseDto(
    val status: String = "",
    val product: OpenFoodFactsProductDto? = null,
)

@Serializable
internal data class OpenFoodFactsProductDto(
    val code: String = "",
    val productName: String = "",
    val genericName: String = "",
    val lang: String = "",
    val brands: String = "",
    val quantity: String = "",
    val productQuantity: Double? = null,
    val productQuantityUnit: String = "",
    val servingSize: String = "",
    val categoriesTags: List<String> = emptyList(),
    val labelsTags: List<String> = emptyList(),
    val countriesTags: List<String> = emptyList(),
    val originsTags: List<String> = emptyList(),
    val ingredientsText: String = "",
    val ingredients: List<OpenFoodFactsIngredientDto> = emptyList(),
    val allergensTags: List<String> = emptyList(),
    val tracesTags: List<String> = emptyList(),
    val additivesTags: List<String> = emptyList(),
    val nutrition: OpenFoodFactsNutritionDto? = null,
    val nutriscoreGrade: String = "",
    val nutriscoreScore: Int? = null,
    val novaGroup: Int? = null,
    val environmentalScoreGrade: String = "",
    val environmentalScoreScore: Int? = null,
    val imageFrontUrl: String = "",
    val imageFrontSmallUrl: String = "",
    val packagings: List<OpenFoodFactsPackagingDto> = emptyList(),
    val schemaVersion: Int? = null,
    val rev: Long? = null,
    val lastModifiedT: Long? = null,
)

@Serializable
internal data class OpenFoodFactsIngredientDto(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<OpenFoodFactsIngredientDto> = emptyList(),
)

@Serializable
internal data class OpenFoodFactsNutritionDto(
    val aggregatedSet: OpenFoodFactsNutrientSetDto? = null,
)

@Serializable
internal data class OpenFoodFactsNutrientSetDto(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, OpenFoodFactsNutrientValueDto> = emptyMap(),
)

@Serializable
internal data class OpenFoodFactsNutrientValueDto(
    val value: Double? = null,
    val valueComputed: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

@Serializable
internal data class OpenFoodFactsPackagingDto(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: JsonElement? = null,
    val shape: JsonElement? = null,
    val recycling: JsonElement? = null,
)

/** Bounded source payload sent to DishLab for canonicalization. */
@Serializable
data class ClientProductSnapshotDto(
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
    val ingredients: List<ClientIngredientDto> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val nutrition: ClientProductNutritionDto? = null,
    val packaging: List<ClientPackagingComponentDto> = emptyList(),
    val sourceProvider: String = OPEN_FOOD_FACTS_PROVIDER,
    val sourceSchemaVersion: Int? = null,
    val sourceRevision: Long? = null,
    val sourceUpdatedAtEpochSeconds: Long? = null,
    /**
     * The complete OFF `product` object, verbatim. Backstop for every field OFF
     * exposes that this DTO does not (yet) name explicitly — typed fields above
     * cover what the app currently uses; this is what makes "all data for one
     * barcode" true regardless of OFF schema growth.
     */
    val rawSourceJson: String = "",
)

@Serializable
data class ClientIngredientDto(
    val id: String = "",
    val text: String = "",
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
    val fromPalmOil: String = "",
    val ingredients: List<ClientIngredientDto> = emptyList(),
)

@Serializable
data class ClientProductNutritionDto(
    val per: String = "",
    val preparation: String = "",
    val nutrients: Map<String, ClientNutrientValueDto> = emptyMap(),
)

@Serializable
data class ClientNutrientValueDto(
    val value: Double? = null,
    val computedValue: Double? = null,
    val unit: String = "",
    val source: String = "",
    val sourcePer: String = "",
)

@Serializable
data class ClientPackagingComponentDto(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

internal fun OpenFoodFactsProductResponseDto.toSnapshot(
    requestedBarcode: String,
    rawProductJson: JsonElement? = null,
): ClientProductSnapshotDto? {
    val source = product ?: return null
    val barcode = source.code.filter(Char::isDigit).ifBlank { requestedBarcode.filter(Char::isDigit) }
    // Every other field is optional — OFF frequently has partial data (no name yet,
    // pending review, etc.), and a barcode with *some* data is strictly more useful
    // than dropping it outright. Only a barcode to key it by is not negotiable.
    val displayName = source.productName.ifBlank { source.genericName }.ifBlank { source.brands }.ifBlank { barcode }.trim()
    val nutrientSet = source.nutrition?.aggregatedSet
    return ClientProductSnapshotDto(
        barcode = barcode,
        name = displayName,
        brand = source.brands.trim(),
        genericName = source.genericName.trim(),
        language = source.lang,
        categories = source.categoriesTags,
        labels = source.labelsTags,
        countries = source.countriesTags,
        origins = source.originsTags,
        quantity = source.quantity,
        productQuantity = source.productQuantity,
        productQuantityUnit = source.productQuantityUnit,
        servingSize = source.servingSize,
        imageFrontUrl = source.imageFrontUrl,
        imageFrontSmallUrl = source.imageFrontSmallUrl,
        nutritionGrade = source.nutriscoreGrade,
        nutritionScore = source.nutriscoreScore,
        novaGroup = source.novaGroup,
        environmentalScoreGrade = source.environmentalScoreGrade,
        environmentalScore = source.environmentalScoreScore,
        ingredientsText = source.ingredientsText,
        ingredients = source.ingredients.map(OpenFoodFactsIngredientDto::toClient),
        allergens = source.allergensTags,
        traces = source.tracesTags,
        additives = source.additivesTags,
        nutrition = nutrientSet?.let { set ->
            ClientProductNutritionDto(
                per = set.per,
                preparation = set.preparation,
                nutrients = set.nutrients.mapValues { (_, nutrient) ->
                    ClientNutrientValueDto(
                        value = nutrient.value,
                        computedValue = nutrient.valueComputed,
                        unit = nutrient.unit,
                        source = nutrient.source,
                        sourcePer = nutrient.sourcePer,
                    )
                },
            )
        },
        packaging = source.packagings.map { packaging ->
            ClientPackagingComponentDto(
                numberOfUnits = packaging.numberOfUnits,
                quantityPerUnit = packaging.quantityPerUnit,
                material = packaging.material.taxonomyId(),
                shape = packaging.shape.taxonomyId(),
                recycling = packaging.recycling.taxonomyId(),
            )
        },
        sourceSchemaVersion = source.schemaVersion,
        sourceRevision = source.rev,
        sourceUpdatedAtEpochSeconds = source.lastModifiedT,
        rawSourceJson = rawProductJson?.toString().orEmpty(),
    )
}

private fun OpenFoodFactsIngredientDto.toClient(): ClientIngredientDto = ClientIngredientDto(
    id = id,
    text = text,
    percent = percent,
    percentEstimate = percentEstimate,
    vegan = vegan,
    vegetarian = vegetarian,
    fromPalmOil = fromPalmOil,
    ingredients = ingredients.map(OpenFoodFactsIngredientDto::toClient),
)

private fun JsonElement?.taxonomyId(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonObject -> get("id")?.jsonPrimitive?.contentOrNull.orEmpty()
    else -> ""
}

private const val OPEN_FOOD_FACTS_PROVIDER = "open_food_facts"
