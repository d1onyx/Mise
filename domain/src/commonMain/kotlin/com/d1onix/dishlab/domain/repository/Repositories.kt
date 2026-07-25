package com.d1onix.dishlab.domain.repository

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
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
 * Process-scoped rather than persisted: a session is what the user is holding in
 * their hands right now. The scanner writes to it, the graph reads from it, and
 * neither feature needs to know about the other.
 */
interface ScanSessionStore {
    val products: StateFlow<List<ProductId>>
    fun add(id: ProductId)
    fun remove(id: ProductId)
    fun reset(ids: List<ProductId>)
}
