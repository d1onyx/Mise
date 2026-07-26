package com.d1onix.dishlab.domain.repository

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProductGraphPosition
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The product catalogue. Backed by a bundled dataset today; the OpenFoodFacts
 * client will implement the same interface without any feature changing.
 */
interface ProductRepository {
    suspend fun byBarcode(barcode: String): Product?
    suspend fun byId(id: ProductId): Product?
    suspend fun byIds(ids: List<ProductId>): List<Product>
    suspend fun all(): List<Product>
}

interface RecipeRepository {
    suspend fun all(): List<Recipe>
    suspend fun byId(id: RecipeId): Recipe?
    /** Recipes that use at least one of [productIds]. */
    suspend fun forProducts(productIds: List<ProductId>): List<Recipe>
}

interface SavedRecipesRepository {
    val saved: Flow<Set<RecipeId>>
    suspend fun toggle(id: RecipeId)
}

interface ScanHistoryRepository {
    /** Most recently scanned first. */
    val history: Flow<List<ProductId>>
    suspend fun add(id: ProductId)
    suspend fun clear()
}

/**
 * The products currently on the combination graph.
 *
 * Persisted locally so the graph can be restored independently of the product
 * catalogue implementation.
 */
interface ScanSessionStore {
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
