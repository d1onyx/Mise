package com.dishlab.application.service

import com.dishlab.domain.model.RecipeVersion

enum class RecipeValidationSeverity { ERROR, WARNING }

data class RecipeValidationCheck(
    val field: String?,
    val severity: RecipeValidationSeverity,
    val message: String,
    val conflictsWith: String? = null,
)

data class RecipeValidationResult(
    val valid: Boolean,
    val checks: List<RecipeValidationCheck>,
)

fun interface RecipeValidationProvider {
    suspend fun validate(recipe: RecipeVersion): RecipeValidationResult
}

class NoOpRecipeValidationProvider : RecipeValidationProvider {
    override suspend fun validate(recipe: RecipeVersion) =
        RecipeValidationResult(valid = true, checks = emptyList())
}
