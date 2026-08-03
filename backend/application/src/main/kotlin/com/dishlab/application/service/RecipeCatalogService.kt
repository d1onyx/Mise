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

interface RecipeCatalogRepository {
    fun search(
        firebaseUid: String,
        query: String?,
        category: String?,
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage

    fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        category: String?,
        tags: List<String>,
        strictTags: Boolean,
        page: Int,
        pageSize: Int,
        partialMatchOnly: Boolean = false,
        exactMatch: Boolean = false,
        exactProductGroups: List<List<String>> = emptyList(),
    ): PantryMatchPage

    fun findById(firebaseUid: String, recipeId: Long): CatalogRecipe?
    fun setBookmarked(firebaseUid: String, recipeId: Long, bookmarked: Boolean)
    fun getCategories(): List<String>
}

class RecipeCatalogService(
    private val repository: RecipeCatalogRepository,
) {
    fun search(
        firebaseUid: String,
        query: String?,
        category: String?,
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage = repository.search(
        firebaseUid = firebaseUid,
        query = query?.trim()?.takeIf(String::isNotBlank),
        category = category?.trim()?.takeIf(String::isNotBlank),
        ingredient = ingredient?.trim()?.takeIf(String::isNotBlank),
        page = page.coerceAtLeast(1),
        pageSize = pageSize.coerceIn(1, 100),
    )

    fun get(firebaseUid: String, recipeId: Long): CatalogRecipe =
        repository.findById(firebaseUid, recipeId) ?: throw NotFoundError("Рецепт не знайдено")

    fun setBookmarked(firebaseUid: String, recipeId: Long, bookmarked: Boolean): CatalogRecipe {
        get(firebaseUid, recipeId)
        repository.setBookmarked(firebaseUid, recipeId, bookmarked)
        return get(firebaseUid, recipeId)
    }

    fun getCategories(firebaseUid: String, userRecipeTags: List<String> = emptyList()): List<String> =
        (repository.getCategories() + userRecipeTags)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        category: String?,
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
        category = category?.trim()?.takeIf(String::isNotBlank),
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
