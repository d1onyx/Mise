package com.dishlab.domain.model

import java.time.Instant
import java.util.UUID

enum class RecipeStatus { DRAFT, PUBLISHED, ARCHIVED }
enum class RecipeVisibility { PRIVATE, FAMILY, PUBLIC }
enum class RecipeMediaType { PHOTO, VIDEO }

data class RecipeIngredient(
    val ingredientId: UUID? = null,
    val name: String,
    val amount: Double,
    val unit: String,
    val note: String? = null,
    val canonicalTags: List<String> = emptyList(),
)

data class RecipeStep(
    val position: Int,
    val text: String,
)

data class RecipeVersion(
    val id: UUID,
    val recipeId: UUID,
    val versionNumber: Int,
    val changelog: String,
    val title: String,
    val description: String? = null,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<RecipeStep>,
    val tags: List<String>,
    val equipment: List<String>,
    val imageUrl: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class Recipe(
    val id: UUID,
    val ownerUserId: UUID,
    val familyId: UUID? = null,
    val forkedFromRecipeId: UUID? = null,
    val status: RecipeStatus,
    val visibility: RecipeVisibility,
    val currentVersion: RecipeVersion,
    val versions: List<RecipeVersion>,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val publishedAt: Instant? = null,
)

data class RecipeReview(
    val id: UUID,
    val recipeId: UUID,
    val userId: UUID,
    val rating: Int,
    val comment: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class RecipeReport(
    val id: UUID,
    val recipeId: UUID,
    val userId: UUID,
    val reason: String,
    val comment: String? = null,
    val status: String = "OPEN",
    val resolution: String? = null,
    val resolutionNote: String? = null,
    val resolvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
)

data class RecipeMediaUploadPolicy(
    val mediaType: RecipeMediaType,
    val maxFileSizeBytes: Long,
)

data class RecipeMediaUploadTicket(
    val uploadUrl: String,
    val publicUrl: String,
    val method: String,
    val expiresInSeconds: Int,
    val maxFileSizeBytes: Long,
)
