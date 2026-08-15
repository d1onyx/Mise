package com.d1onix.dishlab.feature.products.presentation

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductAlternative
import com.d1onix.dishlab.domain.model.ProductDetails
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductIngredient
import com.d1onix.dishlab.domain.model.ProductPackagingComponent

/**
 * Fixtures for previews only.
 *
 * Deliberately covers the awkward cases too — a low score, incomplete data and
 * alternatives — because those are the states hardest to reach in the running app.
 */
internal fun previewProducts(): List<Product> = listOf(previewOats, previewHoney)

internal val previewOats = Product(
    id = ProductId("oats"),
    barcode = "5010026551017",
    name = "Rolled Oats",
    category = "Grains",
    score = 82,
    accentColor = 0xFFC8FF4D,
    initial = "O",
    nutrients = listOf(
        Nutrient("Energy", "389", "kcal"),
        Nutrient("Carbs", "66", "g"),
        Nutrient("Fiber", "10", "g"),
        Nutrient("Protein", "13", "g"),
    ),
    summary = "High in fiber and plant protein, almost no sugar. A great slow-energy base.",
    hasCompleteData = true,
    alternatives = emptyList(),
    details = ProductDetails(
        brand = "Morning Mill",
        quantity = "500 g",
        servingSize = "50 g",
        ingredientsText = "Whole grain oats",
        ingredients = listOf(ProductIngredient(name = "Whole grain oats", percent = 100.0, vegan = "yes", vegetarian = "yes")),
        allergens = listOf("Gluten"),
        traces = listOf("Nuts", "Milk"),
        additives = emptyList(),
        categories = listOf("Cereals", "Oats"),
        labels = listOf("Whole grain", "Vegan"),
        countries = listOf("United Kingdom", "Ireland"),
        origins = listOf("Scotland"),
        nutriScore = "a",
        novaGroup = 1,
        ecoScore = "a",
        imageUrl = "https://images.openfoodfacts.org/images/products/501/002/655/1017/front_en.3.200.jpg",
        packaging = listOf(
            ProductPackagingComponent(numberOfUnits = 1, quantityPerUnit = "500 g", material = "Cardboard", shape = "Box", recycling = "Recycle"),
        ),
        nutrients = listOf(
            Nutrient("Energy", "389", "kcal"),
            Nutrient("Carbohydrates", "66", "g"),
            Nutrient("Sugars", "1", "g"),
            Nutrient("Fiber", "10", "g"),
            Nutrient("Proteins", "13", "g"),
            Nutrient("Salt", "0", "g"),
        ),
    ),
)

internal val previewHoney = Product(
    id = ProductId("honey"),
    barcode = "3175681800014",
    name = "Honey",
    category = "Sweetener",
    score = 44,
    accentColor = 0xFFFFC24E,
    initial = "H",
    nutrients = listOf(
        Nutrient("Energy", "304", "kcal"),
        Nutrient("Carbs", "82", "g"),
        Nutrient("Sugar", "82", "g"),
    ),
    summary = "Almost pure sugar. Fine as a drizzle, but not a health food.",
    hasCompleteData = false,
    alternatives = listOf(
        ProductAlternative("Date syrup", 60),
        ProductAlternative("Mashed banana", 76),
    ),
    details = ProductDetails(brand = "Golden Hive", quantity = "340 g"),
)

internal val previewUnknownProduct = previewHoney.copy(
    id = ProductId("unknown"),
    name = "Newly scanned product",
    nutrients = listOf(
        Nutrient("Energy", "0", "kcal"),
        Nutrient("Carbs", "0", "g"),
    ),
    summary = "",
    alternatives = emptyList(),
    details = ProductDetails(brand = "Local producer"),
)
