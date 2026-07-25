package com.d1onix.dishlab.data.catalog.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val barcode: String,
    val name: String,
    val category: String,
    val score: Int,
    val accentColor: String,
    val initial: String,
    val summary: String,
    val hasCompleteData: Boolean,
    val nutrients: List<NutrientDto>,
    val alternatives: List<AlternativeDto> = emptyList(),
)

@Serializable
data class NutrientDto(
    val name: String,
    val amount: String,
    val unit: String,
)

@Serializable
data class AlternativeDto(
    val name: String,
    val score: Int,
)

@Serializable
data class RecipeDto(
    val id: String,
    val name: String,
    val minutes: Int,
    val difficulty: String,
    val categories: List<String>,
    val productIds: List<String>,
    val description: String,
    val ingredients: List<IngredientDto>,
    val steps: List<RecipeStepDto>,
)

@Serializable
data class IngredientDto(
    val quantity: String,
    val name: String,
)

@Serializable
data class RecipeStepDto(
    val title: String,
    val description: String,
    val timerSeconds: Int? = null,
)
