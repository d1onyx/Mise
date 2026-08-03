package com.dishlab.api.dto

import com.dishlab.application.service.RecipeCompareResult
import com.dishlab.application.service.RecipeFeedResult
import com.dishlab.application.service.RecipeInput
import com.dishlab.application.service.RecipeReviewsResult
import com.dishlab.application.service.RecipeValidationResult
import com.dishlab.application.service.RecipeValidationSeverity
import com.dishlab.domain.model.Recipe
import com.dishlab.domain.model.RecipeIngredient
import com.dishlab.domain.model.RecipeMediaType
import com.dishlab.domain.model.RecipeMediaUploadTicket
import com.dishlab.domain.model.RecipeReview
import com.dishlab.domain.model.RecipeReport
import com.dishlab.domain.model.RecipeStep
import com.dishlab.domain.model.RecipeVersion
import com.dishlab.domain.model.RecipeVisibility
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RecipeIngredientDto(
    val ingredientId: String? = null,
    val name: String,
    val amount: Double,
    val unit: String,
    val note: String? = null,
    val canonicalTags: List<String> = emptyList(),
)

@Serializable
data class RecipeStepDto(
    val position: Int,
    val text: String,
    val timerSeconds: Int? = null,
)

@Serializable
data class RecipeWriteRequest(
    val title: String? = null,
    val description: String? = null,
    val servings: Int? = null,
    val visibility: String? = null,
    val ingredients: List<RecipeIngredientDto>? = null,
    val steps: List<RecipeStepDto>? = null,
    val tags: List<String>? = null,
    val equipment: List<String>? = null,
    val imageUrl: String? = null,
)

@Serializable
data class CreateRecipeVersionRequest(
    val changelog: String,
    val title: String? = null,
    val description: String? = null,
    val servings: Int? = null,
    val ingredients: List<RecipeIngredientDto>? = null,
    val steps: List<RecipeStepDto>? = null,
    val tags: List<String>? = null,
    val equipment: List<String>? = null,
)

@Serializable
data class RecipeVersionResponse(
    val id: String,
    val recipeId: String,
    val versionNumber: Int,
    val changelog: String,
    val title: String,
    val description: String? = null,
    val servings: Int,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>,
    val tags: List<String>,
    val equipment: List<String>,
    val difficulty: String,
    val createdAt: String,
)

@Serializable
data class RecipeResponse(
    val id: String,
    val ownerUserId: String,
    val ownedByCurrentUser: Boolean,
    val familyId: String? = null,
    val forkedFromRecipeId: String? = null,
    val status: String,
    val visibility: String,
    val title: String,
    val description: String? = null,
    val servings: Int,
    val versionNumber: Int,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>,
    val tags: List<String>,
    val equipment: List<String>,
    val imageUrl: String? = null,
    val bookmarked: Boolean,
    val difficulty: String,
    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String? = null,
)

@Serializable
data class RecipeTabsResponse(
    val overview: RecipeResponse,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>,
    val versions: List<RecipeVersionResponse>,
    val reviews: List<RecipeReviewResponse>,
)

@Serializable
data class RecipeDetailsResponse(
    val recipe: RecipeResponse,
    val tabs: RecipeTabsResponse,
)

@Serializable
data class RecipeFeedResponse(
    val items: List<RecipeResponse>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

@Serializable
data class RecipeVersionsResponse(val versions: List<RecipeVersionResponse>)

@Serializable
data class CompareRecipeVersionRequest(val targetVersionNumber: Int)

@Serializable
data class RecipeCompareResponse(
    val fromVersionNumber: Int,
    val toVersionNumber: Int,
    val changedFields: List<String>,
)

@Serializable
data class BookmarkResponse(val recipeId: String, val bookmarked: Boolean)

@Serializable
data class CreateRecipeReviewRequest(val rating: Int, val comment: String? = null)

@Serializable
data class RecipeReviewResponse(
    val id: String,
    val recipeId: String,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String,
)

@Serializable
data class RecipeReviewsResponse(
    val items: List<RecipeReviewResponse>,
    val averageRating: Double,
    val total: Int,
)

@Serializable
data class CreateRecipeReportRequest(val reason: String, val comment: String? = null)

@Serializable
data class RecipeReportResponse(
    val id: String,
    val recipeId: String,
    val userId: String,
    val reason: String,
    val comment: String? = null,
    val status: String,
    val resolution: String? = null,
    val resolutionNote: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String,
)

@Serializable
data class RecipeValidationCheckDto(
    val field: String? = null,
    val severity: String,
    val message: String,
    val conflictsWith: String? = null,
)

@Serializable
data class RecipeValidationResponseDto(
    val valid: Boolean,
    val checks: List<RecipeValidationCheckDto>,
)

@Serializable
data class MediaUploadUrlRequest(
    val mediaType: String,
    val fileName: String,
    val contentType: String,
    val fileSizeBytes: Long,
)

@Serializable
data class MediaUploadUrlResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val method: String,
    val expiresInSeconds: Int,
    val maxFileSizeBytes: Long,
)

fun RecipeWriteRequest.toInput(): RecipeInput = RecipeInput(
    title = title,
    description = description,
    servings = servings,
    visibility = visibility?.let { RecipeVisibility.valueOf(it.trim().uppercase()) },
    ingredients = ingredients?.map { it.toDomain() },
    steps = steps?.map { it.toDomain() },
    tags = tags,
    equipment = equipment,
    imageUrl = imageUrl,
)

fun CreateRecipeVersionRequest.toInput(): RecipeInput = RecipeInput(
    title = title,
    description = description,
    servings = servings,
    ingredients = ingredients?.map { it.toDomain() },
    steps = steps?.map { it.toDomain() },
    tags = tags,
    equipment = equipment,
)

fun RecipeIngredientDto.toDomain(): RecipeIngredient = RecipeIngredient(
    ingredientId = ingredientId?.let { UUID.fromString(it) },
    name = name,
    amount = amount,
    unit = unit.normalizeUnit(),
    note = note,
)

internal fun String.normalizeUnit(): String {
    val key = trim().lowercase()
        .replace(Regex("[.\\s]+"), "")
    return UNIT_MAP[key] ?: trim().ifBlank { "pcs" }
}

private val UNIT_MAP: Map<String, String> = buildMap {
    // mass
    put("г", "g"); put("гр", "g"); put("грам", "g"); put("грами", "g"); put("грамів", "g")
    put("кг", "kg"); put("кілограм", "kg"); put("кілограми", "kg"); put("кілограмів", "kg")
    put("мг", "mg"); put("міліграм", "mg"); put("міліграми", "mg")
    put("г", "g"); put("кг", "kg") // cyrillic already above
    // Russian
    put("гр", "g"); put("грамм", "g"); put("граммов", "g"); put("кг", "kg")
    // English
    put("g", "g"); put("kg", "kg"); put("mg", "mg")
    put("oz", "oz"); put("lb", "lb"); put("lbs", "lb"); put("pound", "lb"); put("pounds", "lb")

    // volume
    put("мл", "ml"); put("міліліт", "ml"); put("мілілітр", "ml"); put("мілілітри", "ml")
    put("л", "l"); put("літр", "l"); put("літри", "l"); put("літрів", "l")
    put("мл", "ml"); put("л", "l")
    // Russian
    put("мл", "ml"); put("литр", "l"); put("литров", "l"); put("литра", "l")
    // English
    put("ml", "ml"); put("l", "l"); put("liter", "l"); put("liters", "l")
    put("fl", "fl oz"); put("floz", "fl oz"); put("fluidoz", "fl oz")
    put("cup", "cup"); put("cups", "cup")
    put("qt", "qt"); put("quart", "qt"); put("quarts", "qt")
    put("pt", "pt"); put("pint", "pt"); put("pints", "pt")
    put("gallon", "gal"); put("gallons", "gal"); put("gal", "gal")

    // spoon measures
    put("стл", "tbsp"); put("ст.л", "tbsp"); put("столоваложка", "tbsp")
    put("столовіложки", "tbsp"); put("столовихложок", "tbsp")
    put("стол", "tbsp"); put("ложка", "tbsp"); put("ложки", "tbsp")
    // Russian
    put("стл", "tbsp"); put("столоваяложка", "tbsp"); put("столовыхложек", "tbsp")
    // English
    put("tbsp", "tbsp"); put("tablespoon", "tbsp"); put("tablespoons", "tbsp"); put("tbs", "tbsp")

    put("чл", "tsp"); put("ч.л", "tsp"); put("чайналожка", "tsp")
    put("чайніложки", "tsp"); put("чайнихложок", "tsp")
    // Russian
    put("чл", "tsp"); put("чайнаяложка", "tsp")
    // English
    put("tsp", "tsp"); put("teaspoon", "tsp"); put("teaspoons", "tsp")

    // count
    put("шт", "pcs"); put("штук", "pcs"); put("штуки", "pcs"); put("штука", "pcs")
    put("одиниця", "pcs"); put("одиниці", "pcs")
    // Russian
    put("шт", "pcs"); put("штука", "pcs"); put("штуки", "pcs"); put("штук", "pcs")
    // English
    put("pcs", "pcs"); put("piece", "pcs"); put("pieces", "pcs"); put("unit", "pcs"); put("units", "pcs")
    put("item", "pcs"); put("items", "pcs"); put("ea", "pcs"); put("each", "pcs")

    // common cooking units
    put("пучок", "bunch"); put("пучки", "bunch"); put("пучків", "bunch")
    put("пучок", "bunch") // already there
    // Russian
    put("пучок", "bunch"); put("пучка", "bunch"); put("пучков", "bunch")
    // English
    put("bunch", "bunch"); put("bunches", "bunch")

    put("зубчик", "clove"); put("зубчики", "clove"); put("зубчиків", "clove")
    // Russian
    put("зубчик", "clove"); put("зубчика", "clove"); put("зубчиков", "clove")
    put("clove", "clove"); put("cloves", "clove")

    put("щіпка", "pinch"); put("щіпки", "pinch"); put("щіпок", "pinch")
    // Russian
    put("щепотка", "pinch"); put("щепотки", "pinch")
    put("pinch", "pinch"); put("pinches", "pinch")

    put("склянка", "cup"); put("склянки", "cup"); put("склянок", "cup")
    // Russian
    put("стакан", "cup"); put("стакана", "cup"); put("стаканов", "cup")

    put("пакет", "pack"); put("пакети", "pack"); put("пакетів", "pack"); put("пакетик", "pack")
    // Russian
    put("пакет", "pack"); put("пакетик", "pack"); put("пакетиков", "pack")
    put("pack", "pack"); put("packs", "pack"); put("packet", "pack"); put("packets", "pack")

    put("шматок", "piece"); put("шматки", "piece"); put("шматків", "piece")
    // Russian
    put("кусок", "piece"); put("куска", "piece"); put("кусков", "piece")

    put("головка", "head"); put("головки", "head"); put("головок", "head")
    // Russian
    put("головка", "head"); put("головок", "head")
    put("head", "head"); put("heads", "head")

    put("стебло", "stalk"); put("стебла", "stalk"); put("стебел", "stalk")
    // Russian
    put("стебель", "stalk"); put("стеблей", "stalk")
    put("stalk", "stalk"); put("stalks", "stalk"); put("stem", "stalk"); put("stems", "stalk")

    put("листок", "leaf"); put("листки", "leaf"); put("листків", "leaf"); put("лист", "leaf")
    // Russian
    put("лист", "leaf"); put("листьев", "leaf"); put("листа", "leaf")
    put("leaf", "leaf"); put("leaves", "leaf")

    put("краплі", "drops"); put("крапля", "drops"); put("крапель", "drops")
    // Russian
    put("капля", "drops"); put("капли", "drops"); put("капель", "drops")
    put("drop", "drops"); put("drops", "drops")

    put("скибочка", "slice"); put("скибочки", "slice"); put("скибочок", "slice")
    // Russian
    put("ломтик", "slice"); put("ломтика", "slice"); put("ломтиков", "slice")
    put("slice", "slice"); put("slices", "slice")

    put("порція", "serving"); put("порції", "serving"); put("порцій", "serving")
    // Russian
    put("порция", "serving"); put("порции", "serving"); put("порций", "serving")
    put("serving", "serving"); put("servings", "serving")
}

fun RecipeStepDto.toDomain(): RecipeStep = RecipeStep(position = position, text = text, timerSeconds = timerSeconds)

fun RecipeIngredient.toDto(): RecipeIngredientDto = RecipeIngredientDto(
    ingredientId = ingredientId?.toString(),
    name = name,
    amount = amount,
    unit = unit,
    note = note,
    canonicalTags = canonicalTags.ifEmpty { name.ingredientCanonicalTag()?.let { listOf(it) } ?: emptyList() },
)

internal fun String.ingredientCanonicalTag(): String? {
    // Only generate canonical tag when name is Latin-based (English/French/etc.).
    // Cyrillic, Arabic, Hebrew, CJK names can't be auto-slugged into a meaningful "en:" tag
    // without actual translation — returning wrong "en:яблуко" is worse than returning nothing.
    val hasCyrillic = any { it.code in 0x0400..0x04FF }
    val hasArabic = any { it.code in 0x0600..0x06FF }
    val hasCjk = any { it.code in 0x4E00..0x9FFF }
    if (hasCyrillic || hasArabic || hasCjk) return null
    return toEnglishIngredientTaxonomyTag().takeIf(String::isNotBlank)
}

fun RecipeStep.toDto(): RecipeStepDto = RecipeStepDto(position = position, text = text, timerSeconds = timerSeconds)

private fun Int.toDifficulty(): String = when {
    this < 5 -> "easy"
    this < 10 -> "medium"
    else -> "hard"
}

fun RecipeVersion.toResponse(): RecipeVersionResponse = RecipeVersionResponse(
    id = id.toString(),
    recipeId = recipeId.toString(),
    versionNumber = versionNumber,
    changelog = changelog,
    title = title,
    description = description,
    servings = servings,
    ingredients = ingredients.map { it.toDto() },
    steps = steps.map { it.toDto() },
    tags = tags,
    equipment = equipment,
    difficulty = steps.size.toDifficulty(),
    createdAt = createdAt.toString(),
)

fun Recipe.toResponse(viewerUserId: UUID? = null): RecipeResponse = RecipeResponse(
    id = id.toString(),
    ownerUserId = ownerUserId.toString(),
    ownedByCurrentUser = viewerUserId != null && ownerUserId == viewerUserId,
    familyId = familyId?.toString(),
    forkedFromRecipeId = forkedFromRecipeId?.toString(),
    status = status.name,
    visibility = visibility.name,
    title = currentVersion.title,
    description = currentVersion.description,
    servings = currentVersion.servings,
    versionNumber = currentVersion.versionNumber,
    ingredients = currentVersion.ingredients.map { it.toDto() },
    steps = currentVersion.steps.map { it.toDto() },
    tags = currentVersion.tags,
    equipment = currentVersion.equipment,
    imageUrl = currentVersion.imageUrl,
    bookmarked = viewerUserId != null && bookmarkedByUserIds.contains(viewerUserId),
    difficulty = currentVersion.steps.size.toDifficulty(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    publishedAt = publishedAt?.toString(),
)

fun Recipe.toDetailsResponse(reviews: List<RecipeReviewResponse> = emptyList(), viewerUserId: UUID? = null): RecipeDetailsResponse {
    val recipe = toResponse(viewerUserId)
    return RecipeDetailsResponse(
        recipe = recipe,
        tabs = RecipeTabsResponse(
            overview = recipe,
            ingredients = currentVersion.ingredients.map { it.toDto() },
            steps = currentVersion.steps.map { it.toDto() },
            versions = versions.sortedByDescending { it.versionNumber }.map { it.toResponse() },
            reviews = reviews,
        ),
    )
}

fun RecipeFeedResult.toResponse(viewerUserId: UUID? = null): RecipeFeedResponse = RecipeFeedResponse(
    items = items.map { it.toResponse(viewerUserId) },
    page = page,
    pageSize = pageSize,
    total = total,
)

fun RecipeCompareResult.toResponse(): RecipeCompareResponse = RecipeCompareResponse(fromVersionNumber, toVersionNumber, changedFields)

fun RecipeReview.toResponse(): RecipeReviewResponse = RecipeReviewResponse(
    id = id.toString(),
    recipeId = recipeId.toString(),
    userId = userId.toString(),
    rating = rating,
    comment = comment,
    createdAt = createdAt.toString(),
)

fun RecipeReviewsResult.toResponse(): RecipeReviewsResponse = RecipeReviewsResponse(
    items = items.map { it.toResponse() },
    averageRating = averageRating,
    total = total,
)

fun RecipeReport.toResponse(): RecipeReportResponse = RecipeReportResponse(
    id = id.toString(),
    recipeId = recipeId.toString(),
    userId = userId.toString(),
    reason = reason,
    comment = comment,
    status = status,
    resolution = resolution,
    resolutionNote = resolutionNote,
    resolvedAt = resolvedAt?.toString(),
    createdAt = createdAt.toString(),
)

fun RecipeMediaUploadTicket.toResponse(): MediaUploadUrlResponse = MediaUploadUrlResponse(
    uploadUrl = uploadUrl,
    publicUrl = publicUrl,
    method = method,
    expiresInSeconds = expiresInSeconds,
    maxFileSizeBytes = maxFileSizeBytes,
)

fun MediaUploadUrlRequest.toMediaType(): RecipeMediaType = RecipeMediaType.valueOf(mediaType.trim().uppercase())

fun RecipeValidationResult.toResponse(): RecipeValidationResponseDto = RecipeValidationResponseDto(
    valid = valid,
    checks = checks.map { check ->
        RecipeValidationCheckDto(
            field = check.field,
            severity = check.severity.name,
            message = check.message,
            conflictsWith = check.conflictsWith,
        )
    },
)
