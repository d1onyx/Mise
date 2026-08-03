package com.d1onix.dishlab.domain

import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductConnection
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeFilters
import com.d1onix.dishlab.domain.model.RecipeId
import kotlinx.coroutines.flow.Flow

/** Resolve a scanned barcode. `null` means «not in the catalogue» — the Not found screen. */
fun interface GetProductByBarcodeUseCase {
    suspend operator fun invoke(barcode: String): Product?
}

fun interface GetProductsUseCase {
    suspend operator fun invoke(ids: List<ProductId>): List<Product>
}

fun interface GetRecipesForProductsUseCase {
    suspend operator fun invoke(productIds: List<ProductId>): List<Recipe>
}

fun interface GetAllRecipesUseCase {
    suspend operator fun invoke(): List<Recipe>
}

fun interface FilterRecipesByConnectionsUseCase {
    operator fun invoke(
        recipes: List<Recipe>,
        sessionProductIds: List<ProductId>,
        connections: Set<ProductConnection>,
    ): List<Recipe>
}

fun interface GetRecipeUseCase {
    suspend operator fun invoke(id: RecipeId): Recipe?
}

/** Pure — search and the three filter groups applied to a list of recipes. */
fun interface FilterRecipesUseCase {
    operator fun invoke(recipes: List<Recipe>, filters: RecipeFilters): List<Recipe>
}

fun interface ObserveSavedRecipesUseCase {
    operator fun invoke(): Flow<List<Recipe>>
}

fun interface ObserveSavedRecipeIdsUseCase {
    operator fun invoke(): Flow<Set<RecipeId>>
}

fun interface ToggleSavedRecipeUseCase {
    suspend operator fun invoke(id: RecipeId)
}

fun interface ObserveScanHistoryUseCase {
    operator fun invoke(): Flow<List<Product>>
}

fun interface ClearScanHistoryUseCase {
    suspend operator fun invoke()
}

fun interface RecordScanUseCase {
    suspend operator fun invoke(id: ProductId)
}
