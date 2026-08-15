package com.d1onix.dishlab.feature.products.presentation.graph.components

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductPackagingComponent

/** UI-ready sections for the product sheet. Empty values never become a tile. */
internal data class ProductDetailCardModel(
    val facts: List<ProductDetailFact>,
    val ingredients: String?,
    val ingredientsBreakdown: String?,
    val allergens: List<String>,
    val traces: List<String>,
    val additives: List<String>,
    val categories: List<String>,
    val labels: List<String>,
    val countries: List<String>,
    val origins: List<String>,
    val packaging: List<ProductPackagingComponent>,
    val nutrients: List<Nutrient>,
    val nutrientLevels: List<Pair<String, String>>,
    val foodGroups: List<String>,
    val manufacturingPlaces: List<String>,
    val purchasePlaces: List<String>,
    val stores: List<String>,
    val imageUrl: String?,
    val ingredientsImageUrl: String?,
    val nutritionImageUrl: String?,
    val packagingImageUrl: String?,
)

internal data class ProductDetailFact(val kind: ProductDetailFactKind, val value: String)

internal enum class ProductDetailFactKind { Brand, Quantity, ServingSize, NutriScore, Nova, EcoScore }

internal fun Product.toDetailCardModel(): ProductDetailCardModel {
    val details = details
    val facts = buildList {
        details.brand.normalizedText()?.let { add(ProductDetailFact(ProductDetailFactKind.Brand, it)) }
        details.quantity.normalizedText()?.let { add(ProductDetailFact(ProductDetailFactKind.Quantity, it)) }
        details.servingSize.normalizedText()?.let { add(ProductDetailFact(ProductDetailFactKind.ServingSize, it)) }
        details.nutriScore.normalizedGrade()?.let { add(ProductDetailFact(ProductDetailFactKind.NutriScore, it)) }
        details.novaGroup?.takeIf { it in 1..4 }?.let { add(ProductDetailFact(ProductDetailFactKind.Nova, it.toString())) }
        details.ecoScore.normalizedGrade()?.let { add(ProductDetailFact(ProductDetailFactKind.EcoScore, it)) }
    }
    // The details snapshot carries every nutrient the source reported; the
    // curated headline four on Product.nutrients is only a fallback for
    // products that predate that (e.g. cached/preview fixtures).
    val nutrientSource = details.nutrients.ifEmpty { nutrients }
    val validNutrients = nutrientSource.filter { nutrient ->
        nutrient.name.normalizedText() != null &&
            nutrient.unit.normalizedText() != null &&
            nutrient.amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    }
    val breakdown = details.ingredients
        .filter { it.name.normalizedText() != null }
        .joinToString { ingredient ->
            val percent = ingredient.percent ?: ingredient.percentEstimate
            buildString {
                append(ingredient.name.trim())
                percent?.let { append(" (").append(it.toInt()).append("%)") }
            }
        }
        .takeIf(String::isNotBlank)
    return ProductDetailCardModel(
        facts = facts,
        ingredients = details.ingredientsText.normalizedText(),
        ingredientsBreakdown = breakdown,
        allergens = details.allergens.mapNotNull(String::normalizedTaxonomyTag),
        traces = details.traces.mapNotNull(String::normalizedTaxonomyTag),
        additives = details.additives.mapNotNull(String::normalizedTaxonomyTag),
        categories = details.categories.mapNotNull(String::normalizedTaxonomyTag),
        labels = details.labels.mapNotNull(String::normalizedTaxonomyTag),
        countries = details.countries.mapNotNull(String::normalizedTaxonomyTag),
        origins = details.origins.mapNotNull(String::normalizedTaxonomyTag),
        packaging = details.packaging,
        nutrients = validNutrients,
        nutrientLevels = details.nutrientLevels.entries
            .mapNotNull { (name, level) -> name.normalizedTaxonomyTag()?.let { it to level } },
        foodGroups = details.foodGroups.mapNotNull(String::normalizedTaxonomyTag),
        manufacturingPlaces = details.manufacturingPlaces.mapNotNull(String::normalizedTaxonomyTag),
        purchasePlaces = details.purchasePlaces.mapNotNull(String::normalizedTaxonomyTag),
        stores = details.stores.mapNotNull(String::normalizedTaxonomyTag),
        imageUrl = details.imageUrl.trim().takeIf { it.startsWith("https://") },
        ingredientsImageUrl = details.ingredientsImageUrl.trim().takeIf { it.startsWith("https://") },
        nutritionImageUrl = details.nutritionImageUrl.trim().takeIf { it.startsWith("https://") },
        packagingImageUrl = details.packagingImageUrl.trim().takeIf { it.startsWith("https://") },
    )
}

private fun String.normalizedText(): String? = trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }

private fun String.normalizedGrade(): String? = trim().uppercase().takeIf { it in setOf("A", "B", "C", "D", "E") }

private fun String.normalizedTaxonomyTag(): String? = normalizedText()
    ?.substringAfter(':')
    ?.replace('-', ' ')
    ?.split(' ')
    ?.filter(String::isNotBlank)
    ?.joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
    ?.takeIf(String::isNotBlank)
