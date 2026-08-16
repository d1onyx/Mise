package com.d1onix.dishlab.feature.products.presentation.graph.components

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductPackagingComponent

/**
 * UI-ready fields for the product sheet's row lists. Unlike the previous
 * tile-based model, a blank string or empty list here is a real value — it
 * means the sheet still shows the row, just with a placeholder, so a field OFF
 * hasn't been filled in for this product still has a visible slot.
 */
internal data class ProductDetailCardModel(
    val brand: String,
    val quantity: String,
    val servingSize: String,
    val expirationDate: String,
    val nutriScoreGrade: String,
    val ecoScoreGrade: String,
    val nutriScoreVersion: String,
    val comparedToCategory: String,
    val novaGroup: Int?,
    val ingredients: String,
    val ingredientsBreakdown: String,
    val allergens: List<String>,
    val traces: List<String>,
    val additives: List<String>,
    val categories: List<String>,
    val labels: List<String>,
    val countries: List<String>,
    val origins: List<String>,
    val originNote: String,
    val pnnsGroup: String,
    val pnnsSubgroup: String,
    val foodGroups: List<String>,
    val manufacturingPlaces: List<String>,
    val purchasePlaces: List<String>,
    val stores: List<String>,
    val packaging: List<ProductPackagingComponent>,
    val nutrients: List<Nutrient>,
    val nutrientsPer: String,
    val nutrientLevels: List<Pair<String, String>>,
    val imageUrl: String?,
    val ingredientsImageUrl: String?,
    val nutritionImageUrl: String?,
    val packagingImageUrl: String?,
)

internal fun Product.toDetailCardModel(): ProductDetailCardModel {
    val details = details
    // The details snapshot carries every nutrient the source reported; the
    // curated headline four on Product.nutrients is only a fallback for
    // products that predate that (e.g. cached/preview fixtures).
    val nutrientSource = details.nutrients.ifEmpty { nutrients }
    val validNutrients = nutrientSource.filter { nutrient ->
        nutrient.name.normalizedText().isNotEmpty() &&
            nutrient.unit.normalizedText().isNotEmpty() &&
            nutrient.amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    }
    val breakdown = details.ingredients
        .filter { it.name.normalizedText().isNotEmpty() }
        .joinToString { ingredient ->
            val percent = ingredient.percent ?: ingredient.percentEstimate
            buildString {
                append(ingredient.name.trim())
                percent?.let { append(" (").append(it.toInt()).append("%)") }
            }
        }
    return ProductDetailCardModel(
        brand = details.brand.normalizedText(),
        quantity = details.quantity.normalizedText(),
        servingSize = details.servingSize.normalizedText(),
        expirationDate = details.expirationDate.normalizedText(),
        nutriScoreGrade = details.nutriScore.normalizedGrade(),
        ecoScoreGrade = details.ecoScore.normalizedGrade(),
        nutriScoreVersion = details.nutriScoreVersion.normalizedText(),
        comparedToCategory = details.comparedToCategory.normalizedTaxonomyTag(),
        novaGroup = details.novaGroup?.takeIf { it in 1..4 },
        ingredients = details.ingredientsText.normalizedText(),
        ingredientsBreakdown = breakdown,
        allergens = details.allergens.mapNotNull(String::normalizedTaxonomyTagOrNull),
        traces = details.traces.mapNotNull(String::normalizedTaxonomyTagOrNull),
        additives = details.additives.mapNotNull(String::normalizedTaxonomyTagOrNull),
        categories = details.categories.mapNotNull(String::normalizedTaxonomyTagOrNull),
        labels = details.labels.mapNotNull(String::normalizedTaxonomyTagOrNull),
        countries = details.countries.mapNotNull(String::normalizedTaxonomyTagOrNull),
        origins = details.origins.mapNotNull(String::normalizedTaxonomyTagOrNull),
        originNote = details.originNote.normalizedText(),
        pnnsGroup = details.pnnsGroup.normalizedText(),
        pnnsSubgroup = details.pnnsSubgroup.normalizedText(),
        foodGroups = details.foodGroups.mapNotNull(String::normalizedTaxonomyTagOrNull),
        manufacturingPlaces = details.manufacturingPlaces.mapNotNull(String::normalizedTaxonomyTagOrNull),
        purchasePlaces = details.purchasePlaces.mapNotNull(String::normalizedTaxonomyTagOrNull),
        stores = details.stores.mapNotNull(String::normalizedTaxonomyTagOrNull),
        packaging = details.packaging,
        nutrients = validNutrients,
        nutrientsPer = details.nutrientsPer.normalizedText(),
        nutrientLevels = details.nutrientLevels.entries
            .mapNotNull { (name, level) -> name.normalizedTaxonomyTagOrNull()?.let { it to level } },
        imageUrl = details.imageUrl.trim().takeIf { it.startsWith("https://") },
        ingredientsImageUrl = details.ingredientsImageUrl.trim().takeIf { it.startsWith("https://") },
        nutritionImageUrl = details.nutritionImageUrl.trim().takeIf { it.startsWith("https://") },
        packagingImageUrl = details.packagingImageUrl.trim().takeIf { it.startsWith("https://") },
    )
}

private fun String.normalizedText(): String = trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }.orEmpty()

private fun String.normalizedGrade(): String = trim().uppercase().takeIf { it in setOf("A", "B", "C", "D", "E") }.orEmpty()

private fun String.normalizedTaxonomyTag(): String = normalizedTaxonomyTagOrNull().orEmpty()

private fun String.normalizedTaxonomyTagOrNull(): String? = normalizedText()
    .ifBlank { null }
    ?.substringAfter(':')
    ?.replace('-', ' ')
    ?.split(' ')
    ?.filter(String::isNotBlank)
    ?.joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
    ?.takeIf(String::isNotBlank)
