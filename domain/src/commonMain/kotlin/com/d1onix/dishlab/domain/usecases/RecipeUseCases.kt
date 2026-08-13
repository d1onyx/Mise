package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.FilterRecipesUseCase
import com.d1onix.dishlab.domain.FilterRecipesByConnectionsUseCase
import com.d1onix.dishlab.domain.GetRecipeUseCase
import com.d1onix.dishlab.domain.GetAllRecipesUseCase
import com.d1onix.dishlab.domain.GetRecipeCatalogFiltersUseCase
import com.d1onix.dishlab.domain.GetRecipesForProductsUseCase
import com.d1onix.dishlab.domain.ObserveSavedRecipeIdsUseCase
import com.d1onix.dishlab.domain.ObserveSavedRecipesUseCase
import com.d1onix.dishlab.domain.ToggleSavedRecipeUseCase
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeCatalogFilters
import com.d1onix.dishlab.domain.model.RecipeFilters
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipePage
import com.d1onix.dishlab.domain.repository.RecipeRepository
import com.d1onix.dishlab.domain.repository.SavedRecipesRepository
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
@Inject
class GetRecipesForProductsUseCaseImpl(
    private val recipes: RecipeRepository,
) : GetRecipesForProductsUseCase {
    override suspend fun invoke(productIds: List<ProductId>, page: Int, pageSize: Int): RecipePage =
        recipes.forProductsPage(productIds, page, pageSize)
}

@ContributesBinding(AppScope::class)
@Inject
class GetAllRecipesUseCaseImpl(
    private val recipes: RecipeRepository,
) : GetAllRecipesUseCase {
    override suspend fun invoke(page: Int, pageSize: Int, category: String?): RecipePage =
        recipes.allPage(page, pageSize, category)
}

@ContributesBinding(AppScope::class)
@Inject
class GetRecipeCatalogFiltersUseCaseImpl(
    private val recipes: RecipeRepository,
) : GetRecipeCatalogFiltersUseCase {
    override suspend fun invoke(): RecipeCatalogFilters = recipes.catalogFilters()
}

@ContributesBinding(AppScope::class)
@Inject
class FilterRecipesByConnectionsUseCaseImpl : FilterRecipesByConnectionsUseCase {
    override fun invoke(
        recipes: List<Recipe>,
        sessionProductIds: List<ProductId>,
        connections: Set<ProductConnection>,
    ): List<Recipe> {
        val sessionProducts = sessionProductIds.toSet()
        return recipes.filter { recipe ->
            val recipeProducts = recipe.productIds.distinct().filterTo(mutableSetOf()) {
                it in sessionProducts
            }
            recipeProducts.size <= 1 || recipeProducts.isConnectedBy(connections)
        }
    }
}

private fun Set<ProductId>.isConnectedBy(
    connections: Set<ProductConnection>,
): Boolean {
    val visited = mutableSetOf<ProductId>()
    val queue = ArrayDeque<ProductId>()
    queue.add(first())

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current)) continue
        connections.forEach { connection ->
            val next = connection.other(current)
            if (next != null && next in this && next !in visited) queue.add(next)
        }
    }
    return visited.containsAll(this)
}

@ContributesBinding(AppScope::class)
@Inject
class GetRecipeUseCaseImpl(
    private val recipes: RecipeRepository,
) : GetRecipeUseCase {
    override suspend fun invoke(id: RecipeId): Recipe? = recipes.byId(id)
}

/**
 * Search matches the name; each filter group narrows independently, and an
 * empty group means «anything». Same rules as the prototype's `filterFn`.
 */
@ContributesBinding(AppScope::class)
@Inject
class FilterRecipesUseCaseImpl : FilterRecipesUseCase {
    override fun invoke(recipes: List<Recipe>, filters: RecipeFilters): List<Recipe> {
        val query = filters.query.trim()
        return recipes.filter { recipe ->
            val matchesQuery = query.isEmpty() || recipe.name.contains(query, ignoreCase = true)
            val matchesDifficulty =
                filters.difficulties.isEmpty() || recipe.difficulty in filters.difficulties
            val matchesCategory =
                filters.categories.isEmpty() || recipe.categories.any { it in filters.categories }
            val matchesTime =
                filters.times.isEmpty() || filters.times.any { it.matches(recipe.minutes) }
            matchesQuery && matchesDifficulty && matchesCategory && matchesTime
        }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class ObserveSavedRecipeIdsUseCaseImpl(
    private val saved: SavedRecipesRepository,
) : ObserveSavedRecipeIdsUseCase {
    override fun invoke(): Flow<Set<RecipeId>> = saved.saved
}

@ContributesBinding(AppScope::class)
@Inject
class ObserveSavedRecipesUseCaseImpl(
    private val saved: SavedRecipesRepository,
    private val recipes: RecipeRepository,
) : ObserveSavedRecipesUseCase {
    override fun invoke(): Flow<List<Recipe>> = saved.saved.map { ids ->
        recipes.all().filter { it.id in ids }
    }
}

@ContributesBinding(AppScope::class)
@Inject
class ToggleSavedRecipeUseCaseImpl(
    private val saved: SavedRecipesRepository,
) : ToggleSavedRecipeUseCase {
    override suspend fun invoke(id: RecipeId) = saved.toggle(id)
}
