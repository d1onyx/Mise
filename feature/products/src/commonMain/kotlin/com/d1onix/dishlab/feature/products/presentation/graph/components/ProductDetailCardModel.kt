package com.d1onix.dishlab.feature.products.presentation.graph.components

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDataOrigin

/** UI-ready sections for the product sheet. Empty values never become a tile. */
internal data class ProductDetailCardModel(
    val facts: List<ProductDetailFact>,
    val ingredients: String?,
    val allergens: List<String>,
    val categories: List<String>,
    val labels: List<String>,
    val nutrients: List<Nutrient>,
    val notInDatabase: Boolean,
    val imageUrl: String?,
    val isDeviceFallback: Boolean,
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
    val validNutrients = nutrients.filter { nutrient ->
        nutrient.name.normalizedText() != null &&
            nutrient.unit.normalizedText() != null &&
            nutrient.amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    }
    val nutritionPresent = validNutrients.isNotEmpty()
    return ProductDetailCardModel(
        facts = facts,
        ingredients = details.ingredientsText.normalizedText(),
        allergens = details.allergens.mapNotNull(String::normalizedText),
        categories = details.categories.mapNotNull(String::normalizedText),
        labels = details.labels.mapNotNull(String::normalizedText),
        nutrients = validNutrients,
        notInDatabase = !nutritionPresent,
        imageUrl = details.imageUrl.trim().takeIf { it.startsWith("https://") },
        isDeviceFallback = dataOrigin == ProductDataOrigin.DeviceFallback,
    )
}

private fun String.normalizedText(): String? = trim().takeIf { it.isNotEmpty() && !it.equals("null", true) }

private fun String.normalizedGrade(): String? = trim().uppercase().takeIf { it in setOf("A", "B", "C", "D", "E") }
