package com.dishlab.application.service

import com.dishlab.domain.error.NotFoundError
import com.dishlab.domain.model.CatalogRecipe
import com.dishlab.domain.model.PantryMatchedRecipe

data class CatalogRecipePage(
    val items: List<CatalogRecipe>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

data class PantryMatchPage(
    val items: List<PantryMatchedRecipe>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

/**
 * Catalog keywords are one flat tag pool (dish type, cuisine, equipment, technique all mixed
 * together in the source dataset) — [CatalogFilters] is that pool split into the groups a filter
 * UI actually wants, so the client renders a "cuisine" chip row separately from an "equipment"
 * chip row instead of one undifferentiated list.
 */
data class CatalogFilters(
    val categories: List<String>,
    val cuisines: List<String>,
    val equipment: List<String>,
    val techniques: List<String>,
)

interface RecipeCatalogRepository {
    fun search(
        firebaseUid: String,
        query: String?,
        categories: List<String> = emptyList(),
        cuisines: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        techniques: List<String> = emptyList(),
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage

    fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        categories: List<String> = emptyList(),
        cuisines: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        techniques: List<String> = emptyList(),
        tags: List<String>,
        strictTags: Boolean,
        page: Int,
        pageSize: Int,
        partialMatchOnly: Boolean = false,
        exactMatch: Boolean = false,
        exactProductGroups: List<List<String>> = emptyList(),
    ): PantryMatchPage

    fun findById(firebaseUid: String, recipeId: Long): CatalogRecipe?
    fun getFilters(): CatalogFilters
}

class RecipeCatalogService(
    private val repository: RecipeCatalogRepository,
) {
    fun search(
        firebaseUid: String,
        query: String?,
        categories: List<String> = emptyList(),
        cuisines: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        techniques: List<String> = emptyList(),
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage = repository.search(
        firebaseUid = firebaseUid,
        query = query?.trim()?.takeIf(String::isNotBlank),
        categories = categories.cleaned(),
        cuisines = cuisines.cleaned(),
        equipment = equipment.cleaned(),
        techniques = techniques.cleaned(),
        ingredient = ingredient?.trim()?.takeIf(String::isNotBlank),
        page = page.coerceAtLeast(1),
        pageSize = pageSize.coerceIn(1, 100),
    )

    fun get(firebaseUid: String, recipeId: Long): CatalogRecipe =
        repository.findById(firebaseUid, recipeId) ?: throw NotFoundError("Рецепт не знайдено")

    fun getFilters(firebaseUid: String, userRecipeTags: List<String> = emptyList()): CatalogFilters {
        val filters = repository.getFilters()
        return filters.copy(
            categories = (filters.categories + userRecipeTags).filter(String::isNotBlank).distinct().sorted(),
        )
    }

    fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        categories: List<String> = emptyList(),
        cuisines: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        techniques: List<String> = emptyList(),
        tags: List<String>,
        strictTags: Boolean,
        page: Int,
        pageSize: Int,
        partialMatchOnly: Boolean = false,
        exactMatch: Boolean = false,
        exactProductGroups: List<List<String>> = emptyList(),
    ): PantryMatchPage = repository.findByPantryIngredients(
        firebaseUid = firebaseUid,
        ingredientNames = ingredientNames.map { it.trim() }.filter { it.isNotBlank() },
        categories = categories.cleaned(),
        cuisines = cuisines.cleaned(),
        equipment = equipment.cleaned(),
        techniques = techniques.cleaned(),
        tags = tags.map { it.trim() }.filter { it.isNotBlank() },
        strictTags = strictTags,
        page = page.coerceAtLeast(1),
        pageSize = pageSize.coerceIn(1, 100),
        partialMatchOnly = partialMatchOnly,
        exactMatch = exactMatch,
        exactProductGroups = exactProductGroups
            .map { group -> group.map { it.trim() }.filter { it.isNotBlank() } }
            .filter { it.isNotEmpty() },
    )
}

private fun List<String>.cleaned(): List<String> = map { it.trim() }.filter { it.isNotBlank() }.distinct()
