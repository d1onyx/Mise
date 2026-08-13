package com.dishlab.application.service

import com.dishlab.domain.error.ForbiddenError
import com.dishlab.domain.error.MediaFileTooLargeError
import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.error.PublishValidationError
import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.Ingredient
import com.dishlab.domain.model.Recipe
import com.dishlab.domain.model.RecipeIngredient
import com.dishlab.domain.model.RecipeMediaType
import com.dishlab.domain.model.RecipeMediaUploadTicket
import com.dishlab.domain.model.RecipeReview
import com.dishlab.domain.model.RecipeReport
import com.dishlab.domain.model.RecipeStatus
import com.dishlab.domain.model.RecipeStep
import com.dishlab.domain.model.RecipeVersion
import com.dishlab.domain.model.RecipeVisibility
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface RecipeRepository {
    fun save(recipe: Recipe): Recipe
    fun findById(id: UUID): Recipe?
    fun list(): List<Recipe>
    fun delete(id: UUID)
    fun saveReview(review: RecipeReview): RecipeReview
    fun reviews(recipeId: UUID): List<RecipeReview>
    fun saveReport(report: RecipeReport): RecipeReport
    fun reports(): List<RecipeReport>
    fun findReportById(reportId: UUID): RecipeReport?
}

class InMemoryRecipeRepository : RecipeRepository {
    private val recipes = ConcurrentHashMap<UUID, Recipe>()
    private val reviews = ConcurrentHashMap<UUID, MutableList<RecipeReview>>()
    private val reports = ConcurrentHashMap<UUID, MutableList<RecipeReport>>()

    override fun save(recipe: Recipe): Recipe {
        recipes[recipe.id] = recipe
        return recipe
    }

    override fun findById(id: UUID): Recipe? = recipes[id]

    override fun list(): List<Recipe> = recipes.values.sortedByDescending { it.updatedAt }

    override fun delete(id: UUID) {
        recipes.remove(id)
    }

    override fun saveReview(review: RecipeReview): RecipeReview {
        reviews.computeIfAbsent(review.recipeId) { mutableListOf() }.add(review)
        return review
    }

    override fun reviews(recipeId: UUID): List<RecipeReview> = reviews[recipeId].orEmpty().sortedByDescending { it.createdAt }

    override fun saveReport(report: RecipeReport): RecipeReport {
        val recipeReports = reports.computeIfAbsent(report.recipeId) { mutableListOf() }
        recipeReports.removeIf { it.id == report.id }
        recipeReports.add(report)
        return report
    }

    override fun reports(): List<RecipeReport> = reports.values.flatten().sortedByDescending { it.createdAt }

    override fun findReportById(reportId: UUID): RecipeReport? = reports.values.flatten().firstOrNull { it.id == reportId }
}

/**
 * Maps an `Idempotency-Key` (scoped per user) to the recipe id created for it, so an
 * offline client that retries `POST /recipes` after a lost response gets the original
 * recipe back instead of creating a duplicate. Keyed per user to avoid cross-user clashes.
 */
interface RecipeIdempotencyStore {
    fun get(userId: UUID, key: String): UUID?
    fun put(userId: UUID, key: String, recipeId: UUID)
}

class InMemoryRecipeIdempotencyStore : RecipeIdempotencyStore {
    private val keys = ConcurrentHashMap<String, UUID>()
    private fun composite(userId: UUID, key: String) = "$userId:$key"
    override fun get(userId: UUID, key: String): UUID? = keys[composite(userId, key)]
    override fun put(userId: UUID, key: String, recipeId: UUID) {
        keys[composite(userId, key)] = recipeId
    }
}

data class RecipeInput(
    val title: String? = null,
    val description: String? = null,
    val servings: Int? = null,
    val visibility: RecipeVisibility? = null,
    val ingredients: List<RecipeIngredient>? = null,
    val steps: List<RecipeStep>? = null,
    val tags: List<String>? = null,
    val equipment: List<String>? = null,
    val imageUrl: String? = null,
)

data class RecipeFeedResult(
    val items: List<Recipe>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

data class RecipeCompareResult(
    val fromVersionNumber: Int,
    val toVersionNumber: Int,
    val changedFields: List<String>,
)

data class RecipeReviewsResult(
    val items: List<RecipeReview>,
    val averageRating: Double,
    val total: Int,
)

class RecipeService(
    private val currentUserResolver: CurrentUserResolver,
    private val recipes: RecipeRepository,
    private val idempotencyKeys: RecipeIdempotencyStore = InMemoryRecipeIdempotencyStore(),
    private val ingredientValidationProvider: RecipeIngredientValidationProvider = NoOpRecipeIngredientValidationProvider(),
    private val ingredientRepository: IngredientRepository? = null,
    private val validationProvider: RecipeValidationProvider = NoOpRecipeValidationProvider(),
    private val ingredientCatalog: IngredientTagCatalog? = null,
    private val tagValidator: TagValidationProvider? = null,
    private val tagCategorizationProvider: TagCategorizationProvider? = null,
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    fun delete(firebaseUid: String, recipeId: UUID) {
        val user = currentUserResolver.resolve(firebaseUid)
        recipeForWrite(recipeId, user.id)
        recipes.delete(recipeId)
    }

    suspend fun createDraft(firebaseUid: String, input: RecipeInput, idempotencyKey: String? = null): Recipe {
        val user = currentUserResolver.resolve(firebaseUid)
        if (idempotencyKey != null) {
            idempotencyKeys.get(user.id, idempotencyKey)?.let { existingId ->
                recipes.findById(existingId)?.let { return it }
            }
        }
        val createErrors = mutableMapOf<String, String>()
        val rawTitle = input.title?.trim().orEmpty()
        if (rawTitle.length !in 2..120) createErrors["title"] = "Title must be between 2 and 120 characters"
        val rawServings = input.servings
        if (rawServings == null || rawServings !in 1..100) createErrors["servings"] = "Servings must be between 1 and 100"
        if (createErrors.isNotEmpty()) throw ValidationError(details = createErrors)
        val title = rawTitle
        val servings = rawServings!!
        val rawIngredients = input.ingredients.orEmpty().also { validateIngredients(it, requireNonEmpty = false) }
        val ingredients = resolveCanonicalTags(validateAndEnrichIngredients(rawIngredients, user.id))
        val version = RecipeVersion(
            id = UUID.randomUUID(),
            recipeId = UUID.randomUUID(),
            versionNumber = 1,
            changelog = "Initial draft",
            title = title,
            description = input.description?.trim()?.takeIf { it.isNotBlank() },
            servings = servings,
            ingredients = ingredients,
            steps = input.steps.orEmpty().also { validateSteps(it, requireNonEmpty = false) },
            tags = input.tags.normalizedDistinct(),
            equipment = input.equipment.normalizedDistinct(),
            imageUrl = input.imageUrl?.trim()?.takeIf { it.isNotBlank() },
        )
        val recipeId = version.recipeId
        val saved = recipes.save(
            Recipe(
                id = recipeId,
                ownerUserId = user.id,
                status = RecipeStatus.DRAFT,
                visibility = input.visibility ?: RecipeVisibility.PRIVATE,
                currentVersion = version,
                versions = listOf(version),
            ),
        )
        if (idempotencyKey != null) idempotencyKeys.put(user.id, idempotencyKey, saved.id)
        scheduleAiCanonicalTagEnrichment(saved.id, saved.currentVersion.id)
        return saved
    }

    suspend fun updateDraft(firebaseUid: String, recipeId: UUID, input: RecipeInput): Recipe {
        val user = currentUserResolver.resolve(firebaseUid)
        val recipe = recipeForWrite(recipeId, user.id)
        if (recipe.status != RecipeStatus.DRAFT) throw ValidationError(details = mapOf("status" to "Only drafts can be updated directly; create a new version instead"))
        val current = recipe.currentVersion
        val rawIngredients = input.ingredients?.also { validateIngredients(it, requireNonEmpty = false) }
        val enrichedIngredients = rawIngredients?.let { resolveCanonicalTags(validateAndEnrichIngredients(it, user.id)) }
        val updatedVersion = current.copy(
            title = input.title?.let { validateTitle(it) } ?: current.title,
            description = input.description?.trim()?.takeIf { it.isNotBlank() } ?: current.description,
            servings = input.servings?.let { validateServings(it) } ?: current.servings,
            ingredients = enrichedIngredients ?: current.ingredients,
            steps = input.steps?.also { validateSteps(it, requireNonEmpty = false) } ?: current.steps,
            tags = input.tags?.normalizedDistinct() ?: current.tags,
            equipment = input.equipment?.normalizedDistinct() ?: current.equipment,
        )
        val saved = recipes.save(
            recipe.copy(
                visibility = input.visibility ?: recipe.visibility,
                currentVersion = updatedVersion,
                versions = recipe.versions.dropLast(1) + updatedVersion,
                updatedAt = Instant.now(),
            ),
        )
        if (rawIngredients != null) scheduleAiCanonicalTagEnrichment(saved.id, saved.currentVersion.id)
        return saved
    }

    suspend fun publish(firebaseUid: String, recipeId: UUID): Recipe {
        val user = currentUserResolver.resolve(firebaseUid)
        val recipe = recipeForWrite(recipeId, user.id)
        val version = recipe.currentVersion
        val details = mutableMapOf<String, String>()
        if (version.ingredients.isEmpty()) details["ingredients"] = "At least one ingredient is required"
        if (version.steps.isEmpty()) details["steps"] = "At least one step is required"
        if (version.ingredients.size > MAX_PUBLISH_INGREDIENTS) {
            details["ingredients"] = "A recipe can contain at most $MAX_PUBLISH_INGREDIENTS ingredients"
        }
        if (version.steps.size > MAX_PUBLISH_STEPS) {
            details["steps"] = "A recipe can contain at most $MAX_PUBLISH_STEPS steps"
        }
        if (version.description.orEmpty().length > MAX_PUBLISH_DESCRIPTION_LENGTH) {
            details["description"] = "Description is too long"
        }
        version.ingredients.indexOfFirst { it.name.length > MAX_PUBLISH_INGREDIENT_NAME_LENGTH }.takeIf { it >= 0 }?.let {
            details["ingredients[$it].name"] = "Ingredient name is too long"
        }
        version.steps.indexOfFirst { it.text.length > MAX_PUBLISH_STEP_LENGTH }.takeIf { it >= 0 }?.let {
            details["steps[$it].text"] = "Step text is too long"
        }
        if (details.isNotEmpty()) throw PublishValidationError(details = details)
        val now = Instant.now()
        return recipes.save(
            recipe.copy(
                status = RecipeStatus.PUBLISHED,
                publishedAt = now,
                updatedAt = now,
            ),
        )
    }

    fun get(firebaseUid: String, recipeId: UUID): Recipe {
        currentUserResolver.resolve(firebaseUid)
        return recipes.findById(recipeId) ?: throw NotFoundError("Рецепт не знайдено")
    }

    fun list(
        firebaseUid: String,
        tag: String?,
        query: String?,
        sort: String?,
        page: Int,
        pageSize: Int,
        ownedOnly: Boolean = false,
    ): RecipeFeedResult {
        val user = currentUserResolver.resolve(firebaseUid)
        val normalizedTag = tag?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val normalizedQuery = query?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val filtered = recipes.list()
            .filter { it.status == RecipeStatus.PUBLISHED }
            .filter { !ownedOnly || it.ownerUserId == user.id }
            .filter { normalizedTag == null || it.currentVersion.tags.any { tagValue -> tagValue.lowercase() == normalizedTag } }
            .filter { normalizedQuery == null || it.currentVersion.title.lowercase().contains(normalizedQuery) }
            .let { list -> if (sort == "oldest") list.sortedBy { it.publishedAt } else list.sortedByDescending { it.publishedAt } }
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, 100)
        val start = (safePage - 1) * safePageSize
        return RecipeFeedResult(
            items = if (start >= filtered.size) emptyList() else filtered.drop(start).take(safePageSize),
            page = safePage,
            pageSize = safePageSize,
            total = filtered.size,
        )
    }

    suspend fun createVersion(firebaseUid: String, recipeId: UUID, changelog: String, input: RecipeInput): RecipeVersion {
        val user = currentUserResolver.resolve(firebaseUid)
        val recipe = recipeForWrite(recipeId, user.id)
        val current = recipe.currentVersion
        val cleanChangelog = changelog.trim().ifBlank { throw ValidationError(details = mapOf("changelog" to "Changelog is required")) }
        val rawIngredients = input.ingredients?.also { validateIngredients(it, requireNonEmpty = true) }
        val enrichedIngredients = rawIngredients?.let { resolveCanonicalTags(validateAndEnrichIngredients(it, user.id)) }
        val version = RecipeVersion(
            id = UUID.randomUUID(),
            recipeId = recipe.id,
            versionNumber = recipe.versions.maxOf { it.versionNumber } + 1,
            changelog = cleanChangelog,
            title = input.title?.let { validateTitle(it) } ?: current.title,
            description = input.description?.trim()?.takeIf { it.isNotBlank() } ?: current.description,
            servings = input.servings?.let { validateServings(it) } ?: current.servings,
            ingredients = enrichedIngredients ?: current.ingredients,
            steps = input.steps?.also { validateSteps(it, requireNonEmpty = true) } ?: current.steps,
            tags = input.tags?.normalizedDistinct() ?: current.tags,
            equipment = input.equipment?.normalizedDistinct() ?: current.equipment,
        )
        recipes.save(recipe.copy(currentVersion = version, versions = recipe.versions + version, updatedAt = Instant.now()))
        if (rawIngredients != null) scheduleAiCanonicalTagEnrichment(recipe.id, version.id)
        return version
    }

    fun versions(firebaseUid: String, recipeId: UUID): List<RecipeVersion> = get(firebaseUid, recipeId).versions.sortedByDescending { it.versionNumber }

    fun version(firebaseUid: String, recipeId: UUID, versionId: UUID): RecipeVersion =
        versions(firebaseUid, recipeId).firstOrNull { it.id == versionId } ?: throw NotFoundError("Версію рецепта не знайдено")

    fun compare(firebaseUid: String, recipeId: UUID, versionId: UUID, targetVersionNumber: Int): RecipeCompareResult {
        val from = version(firebaseUid, recipeId, versionId)
        val to = versions(firebaseUid, recipeId).firstOrNull { it.versionNumber == targetVersionNumber }
            ?: throw NotFoundError("Версію для порівняння не знайдено")
        val changed = buildList {
            if (from.title != to.title) add("title")
            if (from.description != to.description) add("description")
            if (from.servings != to.servings) add("servings")
            if (from.ingredients != to.ingredients) add("ingredients")
            if (from.steps != to.steps) add("steps")
            if (from.tags != to.tags) add("tags")
            if (from.equipment != to.equipment) add("equipment")
        }
        return RecipeCompareResult(from.versionNumber, to.versionNumber, changed)
    }

    fun fork(firebaseUid: String, recipeId: UUID): Recipe {
        val user = currentUserResolver.resolve(firebaseUid)
        val source = get(firebaseUid, recipeId)
        if (source.status != RecipeStatus.PUBLISHED) throw ValidationError(details = mapOf("status" to "Only published recipes can be forked"))
        val newRecipeId = UUID.randomUUID()
        val version = source.currentVersion.copy(
            id = UUID.randomUUID(),
            recipeId = newRecipeId,
            versionNumber = 1,
            changelog = "Forked from ${source.id}",
            createdAt = Instant.now(),
        )
        return recipes.save(
            Recipe(
                id = newRecipeId,
                ownerUserId = user.id,
                forkedFromRecipeId = source.id,
                status = RecipeStatus.DRAFT,
                visibility = RecipeVisibility.PRIVATE,
                currentVersion = version,
                versions = listOf(version),
            ),
        )
    }

    fun addReview(firebaseUid: String, recipeId: UUID, rating: Int, comment: String?): RecipeReview {
        val user = currentUserResolver.resolve(firebaseUid)
        get(firebaseUid, recipeId)
        if (rating !in 1..5) throw ValidationError(details = mapOf("rating" to "Rating must be between 1 and 5"))
        return recipes.saveReview(RecipeReview(UUID.randomUUID(), recipeId, user.id, rating, comment?.trim()?.takeIf { it.isNotBlank() }))
    }

    fun reviews(firebaseUid: String, recipeId: UUID): RecipeReviewsResult {
        get(firebaseUid, recipeId)
        val items = recipes.reviews(recipeId)
        val average = if (items.isEmpty()) 0.0 else items.map { it.rating }.average()
        return RecipeReviewsResult(items, average, items.size)
    }

    fun report(firebaseUid: String, recipeId: UUID, reason: String, comment: String?): RecipeReport {
        val user = currentUserResolver.resolve(firebaseUid)
        get(firebaseUid, recipeId)
        val cleanReason = reason.trim().ifBlank { throw ValidationError(details = mapOf("reason" to "Reason is required")) }
        return recipes.saveReport(RecipeReport(UUID.randomUUID(), recipeId, user.id, cleanReason, comment?.trim()?.takeIf { it.isNotBlank() }))
    }

    fun mediaUploadUrl(firebaseUid: String, recipeId: UUID, mediaType: RecipeMediaType, fileName: String, fileSizeBytes: Long): RecipeMediaUploadTicket {
        currentUserResolver.resolve(firebaseUid)
        get(firebaseUid, recipeId)
        val max = when (mediaType) {
            RecipeMediaType.PHOTO -> 10L * 1024 * 1024
            RecipeMediaType.VIDEO -> 50L * 1024 * 1024
        }
        if (fileSizeBytes <= 0) throw ValidationError(details = mapOf("fileSizeBytes" to "File size must be positive"))
        if (fileSizeBytes > max) throw MediaFileTooLargeError(details = mapOf("maxFileSizeBytes" to max.toString()))
        val safeName = fileName.trim().ifBlank { "media.bin" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val key = "recipes/$recipeId/${UUID.randomUUID()}-$safeName"
        return RecipeMediaUploadTicket(
            uploadUrl = "https://storage.dishlab.dev/upload/$key?signature=dev-signed",
            publicUrl = "https://cdn.dishlab.dev/$key",
            method = "PUT",
            expiresInSeconds = 900,
            maxFileSizeBytes = max,
        )
    }

    suspend fun validateRecipe(firebaseUid: String, input: RecipeInput): RecipeValidationResult {
        currentUserResolver.resolve(firebaseUid)
        val version = RecipeVersion(
            id = UUID.randomUUID(),
            recipeId = UUID.randomUUID(),
            versionNumber = 1,
            changelog = "validation",
            title = input.title.orEmpty(),
            description = input.description,
            servings = input.servings ?: 1,
            ingredients = input.ingredients.orEmpty(),
            steps = input.steps.orEmpty(),
            tags = input.tags.orEmpty(),
            equipment = input.equipment.orEmpty(),
        )
        return validationProvider.validate(version)
    }

    fun allRecipes(): List<Recipe> = recipes.list()

    fun saveModeratedRecipe(recipe: Recipe): Recipe = recipes.save(recipe)

    fun reportById(reportId: UUID): RecipeReport = recipes.findReportById(reportId) ?: throw NotFoundError("Скаргу не знайдено")

    fun reports(status: String?): List<RecipeReport> = recipes.reports().filter { status.isNullOrBlank() || it.status.equals(status, ignoreCase = true) }

    fun saveReport(report: RecipeReport): RecipeReport = recipes.saveReport(report)

    private suspend fun validateAndEnrichIngredients(
        ingredients: List<RecipeIngredient>,
        userId: UUID,
    ): List<RecipeIngredient> {
        if (ingredients.isEmpty()) return ingredients
        val repo = ingredientRepository ?: return ingredients
        val resolved = arrayOfNulls<RecipeIngredient>(ingredients.size)
        val unknown = mutableListOf<Pair<Int, RecipeIngredient>>()

        ingredients.forEachIndexed { index, ingredient ->
            val existing = ingredient.ingredientId?.let(repo::findById)
                ?: repo.findByName(ingredient.name)
            if (existing != null) {
                resolved[index] = ingredient.copy(
                    ingredientId = existing.id,
                    name = existing.name,
                )
            } else {
                unknown += index to ingredient
            }
        }
        if (unknown.isEmpty()) return resolved.map { requireNotNull(it) }

        val validation = ingredientValidationProvider.validate(
            ingredients = unknown.map(Pair<Int, RecipeIngredient>::second),
            knownIngredientNames = repo.findAll().map(Ingredient::name),
        )
        if (!validation.available) {
            unknown.forEach { (originalIndex, ingredient) ->
                resolved[originalIndex] = ingredient.copy(
                    ingredientId = null,
                    name = ingredient.name.trim(),
                )
            }
            return resolved.map { requireNotNull(it) }
        }

        val validationErrors = mutableMapOf<String, String>()
        unknown.forEachIndexed { validationIndex, (originalIndex, ingredient) ->
            val item = validation.items.getOrNull(validationIndex)
            when {
                item == null -> validationErrors["ingredients[$originalIndex]"] = "Ingredient validation result is missing"
                !item.isFood -> validationErrors["ingredients[$originalIndex].name"] =
                    "\"${ingredient.name}\" is not a food ingredient.${item.reason?.let { " $it" }.orEmpty()}"
                !item.isEdible -> validationErrors["ingredients[$originalIndex].name"] =
                    "\"${ingredient.name}\" is not edible.${item.reason?.let { " $it" }.orEmpty()}"
                !item.isAmountReasonable -> validationErrors["ingredients[$originalIndex].amount"] =
                    item.reason ?: "Unreasonable amount or unit for \"${ingredient.name}\""
            }
        }
        if (validationErrors.isNotEmpty()) throw ValidationError(details = validationErrors)

        unknown.forEachIndexed { validationIndex, (originalIndex, ingredient) ->
            val item = validation.items[validationIndex]
            val matchedName = item.matchedIngredientName?.trim()?.takeIf(String::isNotBlank)
            val existingMatch = matchedName?.let(repo::findByName)
            val stored = existingMatch ?: repo.save(
                Ingredient(
                    id = UUID.randomUUID(),
                    name = matchedName ?: ingredient.name.trim(),
                    defaultUnit = ingredient.unit.trim().lowercase(),
                    customCreatedByUserId = userId,
                ),
            )
            if (existingMatch == null) {
                ingredientCatalog?.addNew(stored.name, "other")
            }
            resolved[originalIndex] = ingredient.copy(
                ingredientId = stored.id,
                name = stored.name,
                canonicalTags = item.canonicalTags,
            )
        }
        return resolved.map { requireNotNull(it) }
    }

    private suspend fun resolveCanonicalTags(ingredients: List<RecipeIngredient>): List<RecipeIngredient> {
        val catalog = ingredientCatalog ?: return ingredients
        return ingredients.map { ingredient ->
            val alreadyHasTags = ingredient.canonicalTags.isNotEmpty()
            val nameIsLatin = ingredient.name.none { it.code in 0x0400..0x04FF || it.code in 0x0600..0x06FF || it.code in 0x4E00..0x9FFF }
            if (alreadyHasTags && nameIsLatin) return@map ingredient

            val canonicalTag = catalog.resolve(ingredient.name) ?: return@map ingredient
            ingredient.copy(canonicalTags = listOf(canonicalTag.toEnglishIngredientTaxonomyTag()))
        }
    }

    private fun scheduleAiCanonicalTagEnrichment(recipeId: UUID, versionId: UUID) {
        val catalog = ingredientCatalog ?: return
        val validator = tagValidator ?: return
        backgroundScope.launch {
            val recipe = recipes.findById(recipeId) ?: return@launch
            val current = recipe.currentVersion
            if (current.id != versionId) return@launch

            val enrichedIngredients = current.ingredients.map { ingredient ->
                val alreadyHasTags = ingredient.canonicalTags.isNotEmpty()
                val nameIsLatin = ingredient.name.none { it.code in 0x0400..0x04FF || it.code in 0x0600..0x06FF || it.code in 0x4E00..0x9FFF }
                if (alreadyHasTags && nameIsLatin) return@map ingredient

                val result = runCatching { validator.validate(ingredient.name, "") }.getOrNull()
                if (result == null || !result.valid) return@map ingredient

                val englishTag = result.tag.trim()
                val tagIsNonLatin = englishTag.any { it.code in 0x0400..0x04FF || it.code in 0x0600..0x06FF || it.code in 0x4E00..0x9FFF }
                if (englishTag.isBlank() || tagIsNonLatin) return@map ingredient

                val canonicalTag = catalog.resolve(englishTag) ?: run {
                    val category = tagCategorizationProvider?.categorize(englishTag, catalog.categories()) ?: "other"
                    catalog.addNew(englishTag, category)
                    englishTag
                }
                ingredient.copy(canonicalTags = listOf(canonicalTag.toEnglishIngredientTaxonomyTag()))
            }
            if (enrichedIngredients == current.ingredients) return@launch

            val latest = recipes.findById(recipeId) ?: return@launch
            if (latest.currentVersion.id != versionId) return@launch
            val enrichedVersion = latest.currentVersion.copy(ingredients = enrichedIngredients)
            recipes.save(
                latest.copy(
                    currentVersion = enrichedVersion,
                    versions = latest.versions.map { version ->
                        if (version.id == versionId) enrichedVersion else version
                    },
                    updatedAt = Instant.now(),
                ),
            )
        }
    }

    private fun recipeForWrite(recipeId: UUID, userId: UUID): Recipe {
        val recipe = recipes.findById(recipeId) ?: throw NotFoundError("Рецепт не знайдено")
        if (recipe.ownerUserId != userId) {
            println("[FORBIDDEN] recipeId=$recipeId ownerUserId=${recipe.ownerUserId} requestUserId=$userId")
            throw ForbiddenError()
        }
        return recipe
    }

    private fun validateTitle(value: String?): String {
        val title = value?.trim().orEmpty()
        if (title.length !in 2..120) throw ValidationError(details = mapOf("title" to "Title must be between 2 and 120 characters"))
        return title
    }

    private fun validateServings(value: Int?): Int {
        val servings = value ?: throw ValidationError(details = mapOf("servings" to "Servings is required"))
        if (servings !in 1..100) throw ValidationError(details = mapOf("servings" to "Servings must be between 1 and 100"))
        return servings
    }

    private fun validateIngredients(values: List<RecipeIngredient>, requireNonEmpty: Boolean) {
        if (requireNonEmpty && values.isEmpty()) throw ValidationError(details = mapOf("ingredients" to "At least one ingredient is required"))
        values.forEachIndexed { index, ingredient ->
            if (ingredient.name.trim().isBlank()) throw ValidationError(details = mapOf("ingredients[$index].name" to "Ingredient name is required"))
            if (ingredient.amount <= 0.0) throw ValidationError(details = mapOf("ingredients[$index].amount" to "Ingredient amount must be positive"))
            if (ingredient.unit.trim().isBlank()) throw ValidationError(details = mapOf("ingredients[$index].unit" to "Ingredient unit is required"))
        }
    }

    private fun validateSteps(values: List<RecipeStep>, requireNonEmpty: Boolean) {
        if (requireNonEmpty && values.isEmpty()) throw ValidationError(details = mapOf("steps" to "At least one step is required"))
        values.forEachIndexed { index, step ->
            if (step.text.trim().isBlank()) throw ValidationError(details = mapOf("steps[$index].text" to "Step text is required"))
            if (step.position <= 0) throw ValidationError(details = mapOf("steps[$index].position" to "Step position must be positive"))
        }
    }

    private companion object {
        const val MAX_PUBLISH_INGREDIENTS = 100
        const val MAX_PUBLISH_STEPS = 100
        const val MAX_PUBLISH_DESCRIPTION_LENGTH = 5_000
        const val MAX_PUBLISH_INGREDIENT_NAME_LENGTH = 200
        const val MAX_PUBLISH_STEP_LENGTH = 2_000
    }
}

private fun List<String>?.normalizedDistinct(): List<String> =
    orEmpty().map { it.trim() }.filter { it.isNotBlank() }.distinct()
