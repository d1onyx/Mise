package com.d1onix.dishlab.feature.products.presentation

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductAlternative
import com.d1onix.dishlab.domain.model.ProductId

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
)
