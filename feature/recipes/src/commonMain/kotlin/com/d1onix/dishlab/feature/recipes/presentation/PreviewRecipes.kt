package com.d1onix.dishlab.feature.recipes.presentation

import com.d1onix.dishlab.domain.model.Ingredient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep

/** Fixtures for previews only — one recipe per difficulty. */
internal fun previewRecipes(): List<Recipe> = listOf(previewBowl, previewMeatballs)

internal val previewProducts: List<Product> = listOf(
    Product(
        id = ProductId("oats"),
        barcode = "5010026551017",
        name = "Rolled Oats",
        category = "Grains",
        score = 82,
        accentColor = 0xFFC8FF4D,
        initial = "O",
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    ),
    Product(
        id = ProductId("banana"),
        barcode = "4011200296908",
        name = "Banana",
        category = "Fruit",
        score = 76,
        accentColor = 0xFFFFE24E,
        initial = "B",
        nutrients = emptyList(),
        summary = "",
        hasCompleteData = true,
        alternatives = emptyList(),
    ),
)

internal val previewBowl = Recipe(
    id = RecipeId("bowl"),
    name = "Banana Oat Breakfast Bowl",
    minutes = 10,
    difficulty = RecipeDifficulty.Easy,
    categories = listOf("Breakfast", "Vegetarian"),
    productIds = listOf(ProductId("oats"), ProductId("banana")),
    description = "Creamy stovetop oats topped with caramelized banana — a 10-minute " +
        "breakfast that keeps you full till lunch.",
    ingredients = listOf(
        Ingredient("50 g", "Rolled oats"),
        Ingredient("200 ml", "Milk or water"),
        Ingredient("1", "Ripe banana"),
    ),
    steps = listOf(
        RecipeStep("Simmer the oats", "Bring oats and milk to a gentle simmer."),
        RecipeStep("Slice the banana", "Slice the banana into thin rounds."),
        RecipeStep("Assemble & finish", "Top with banana, honey and cinnamon."),
    ),
)

internal val previewMeatballs = Recipe(
    id = RecipeId("meatballs"),
    name = "Chicken & Oat Meatballs",
    minutes = 35,
    difficulty = RecipeDifficulty.Hard,
    categories = listOf("Meat", "Dinner"),
    productIds = listOf(ProductId("oats")),
    description = "Oats replace breadcrumbs for tender, high-protein meatballs.",
    ingredients = listOf(
        Ingredient("400 g", "Ground chicken"),
        Ingredient("40 g", "Rolled oats"),
    ),
    steps = listOf(
        RecipeStep("Mix the mince", "Combine chicken, oats, egg and seasoning."),
        RecipeStep("Sear", "Brown in a hot pan on all sides."),
    ),
)
