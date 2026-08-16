package com.d1onix.dishlab.domain.repository

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.domain.model.ProfileSettings
import com.d1onix.dishlab.domain.model.CookingPreferences
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeCatalogFilters
import com.d1onix.dishlab.domain.model.RecipeCatalogFilterSelection
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipePage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Products resolved by the backend and cached locally after a successful scan. */
interface ProductRepository {
    suspend fun byBarcode(barcode: String): Product?
    suspend fun byId(id: ProductId): Product?
    suspend fun byIds(ids: List<ProductId>): List<Product>
    suspend fun all(): List<Product>
}

interface RecipeRepository {
    suspend fun all(): List<Recipe>
    suspend fun allPage(
        page: Int,
        pageSize: Int,
        filters: RecipeCatalogFilterSelection = RecipeCatalogFilterSelection(),
    ): RecipePage
    suspend fun catalogFilters(): RecipeCatalogFilters
    /** [productIds] preserves pantry-match "already on your graph" highlighting on the ingredient list. */
    suspend fun byId(id: RecipeId, productIds: List<ProductId> = emptyList()): Recipe?
    /** Recipes that use at least one of [productIds]. */
    suspend fun forProducts(productIds: List<ProductId>): List<Recipe>
    suspend fun forProductsPage(
        productIds: List<ProductId>,
        page: Int,
        pageSize: Int,
        filters: RecipeCatalogFilterSelection = RecipeCatalogFilterSelection(),
    ): RecipePage
}

interface SavedRecipesRepository {
    val saved: Flow<Set<RecipeId>>
    /** Full content of saved recipes, cached locally at save time so it resolves fully offline. */
    val savedRecipes: Flow<List<Recipe>>
    suspend fun toggle(recipe: Recipe)
}

interface ScanHistoryRepository {
    /** Most recently scanned first. */
    val history: Flow<List<ProductId>>
    suspend fun add(id: ProductId)
    suspend fun remove(id: ProductId)
    suspend fun clear()
}

/**
 * The products currently on the combination graph.
 *
 * Persisted locally so the graph can be restored independently of the product
 * catalogue implementation.
 */
interface ScanSessionStore {
    /** Null until the durable graph answers its first query; empty means a new graph. */
    val startupProducts: StateFlow<List<ProductId>?>
    val products: StateFlow<List<ProductId>>
    /** User-defined undirected edges that constrain which product sets may form recipes. */
    val connections: StateFlow<Set<ProductConnection>>
    val positions: StateFlow<Map<ProductId, ProductGraphPosition>>
    suspend fun add(id: ProductId)
    suspend fun remove(id: ProductId)
    suspend fun connect(first: ProductId, second: ProductId)
    suspend fun disconnect(first: ProductId, second: ProductId)
    suspend fun updatePosition(id: ProductId, position: ProductGraphPosition)
    suspend fun reset(ids: List<ProductId>)
}

interface ProfileSettingsRepository {
    val settings: Flow<ProfileSettings>
    suspend fun setDisplayName(value: String)
    suspend fun setAutoConnectNewProducts(enabled: Boolean)
    suspend fun setReduceGraphMotion(enabled: Boolean)
    suspend fun setShowProductScores(enabled: Boolean)
}

interface CookingPreferencesRepository {
    val preferences: Flow<CookingPreferences>
    suspend fun save(preferences: CookingPreferences)
}

interface ProductComparisonStore {
    val products: StateFlow<List<ProductId>>
    suspend fun add(id: ProductId): Boolean
    suspend fun remove(id: ProductId)
    suspend fun clear()

    companion object {
        const val MAX_PRODUCTS = 5
    }
}
