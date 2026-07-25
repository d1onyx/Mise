package com.d1onix.dishlab.data.catalog.mapper

import com.d1onix.dishlab.data.catalog.dto.AlternativeDto
import com.d1onix.dishlab.data.catalog.dto.IngredientDto
import com.d1onix.dishlab.data.catalog.dto.NutrientDto
import com.d1onix.dishlab.data.catalog.dto.ProductDto
import com.d1onix.dishlab.data.catalog.dto.RecipeDto
import com.d1onix.dishlab.data.catalog.dto.RecipeStepDto
import com.d1onix.dishlab.domain.model.Ingredient
import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductAlternative
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep

fun ProductDto.toDomain(): Product = Product(
    id = ProductId(id),
    barcode = barcode,
    name = name,
    category = category,
    score = score,
    accentColor = accentColor.toArgb(),
    initial = initial,
    nutrients = nutrients.map(NutrientDto::toDomain),
    summary = summary,
    hasCompleteData = hasCompleteData,
    alternatives = alternatives.map(AlternativeDto::toDomain),
)

fun NutrientDto.toDomain(): Nutrient = Nutrient(name = name, amount = amount, unit = unit)

fun AlternativeDto.toDomain(): ProductAlternative = ProductAlternative(name = name, score = score)

fun RecipeDto.toDomain(): Recipe = Recipe(
    id = RecipeId(id),
    name = name,
    minutes = minutes,
    difficulty = RecipeDifficulty.parse(difficulty) ?: RecipeDifficulty.Easy,
    categories = categories,
    productIds = productIds.map(::ProductId),
    description = description,
    ingredients = ingredients.map(IngredientDto::toDomain),
    steps = steps.map(RecipeStepDto::toDomain),
)

fun IngredientDto.toDomain(): Ingredient = Ingredient(quantity = quantity, name = name)

fun RecipeStepDto.toDomain(): RecipeStep = RecipeStep(
    title = title,
    description = description,
    timerSeconds = timerSeconds,
)

/** `"#C8FF4D"` → opaque ARGB. */
internal fun String.toArgb(): Long = removePrefix("#").toLong(radix = 16) or 0xFF000000L
