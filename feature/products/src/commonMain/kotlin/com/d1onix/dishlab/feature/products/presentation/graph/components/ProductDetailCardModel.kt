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
        details.brand.takeIf(String::isNotBlank)?.let { add(ProductDetailFact(ProductDetailFactKind.Brand, it)) }
        details.quantity.takeIf(String::isNotBlank)?.let { add(ProductDetailFact(ProductDetailFactKind.Quantity, it)) }
        details.servingSize.takeIf(String::isNotBlank)?.let { add(ProductDetailFact(ProductDetailFactKind.ServingSize, it)) }
        details.nutriScore.takeIf(String::isNotBlank)?.let { add(ProductDetailFact(ProductDetailFactKind.NutriScore, it.uppercase())) }
        details.novaGroup?.let { add(ProductDetailFact(ProductDetailFactKind.Nova, it.toString())) }
        details.ecoScore.takeIf(String::isNotBlank)?.let { add(ProductDetailFact(ProductDetailFactKind.EcoScore, it.uppercase())) }
    }
    val nutritionPresent = nutrients.any { nutrient -> nutrient.amount.toDoubleOrNull()?.let { it != 0.0 } == true }
    return ProductDetailCardModel(
        facts = facts,
        ingredients = details.ingredientsText.takeIf(String::isNotBlank),
        allergens = details.allergens.filter(String::isNotBlank),
        categories = details.categories.filter(String::isNotBlank),
        labels = details.labels.filter(String::isNotBlank),
        nutrients = nutrients.takeIf { nutritionPresent }.orEmpty(),
        notInDatabase = !nutritionPresent,
        imageUrl = details.imageUrl.takeIf(String::isNotBlank),
        isDeviceFallback = dataOrigin == ProductDataOrigin.DeviceFallback,
    )
}
