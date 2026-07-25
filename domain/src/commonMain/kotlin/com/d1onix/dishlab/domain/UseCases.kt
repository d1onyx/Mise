package com.d1onix.dishlab.domain

import com.d1onix.dishlab.domain.model.Product
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

/** The whole catalogue — used by the demo scan to pick something to show. */
fun interface GetAllProductsUseCase {
    suspend operator fun invoke(): List<Product>
}

fun interface GetRecipesForProductsUseCase {
    suspend operator fun invoke(productIds: List<ProductId>): List<Recipe>
}

fun interface GetRecipeUseCase {
    suspend operator fun invoke(id: RecipeId): Recipe?
}

/** Pure — search and the three filter groups applied to a list of recipes. */
fun interface FilterRecipesUseCase {
    operator fun invoke(recipes: List<Recipe>, filters: RecipeFilters): List<Recipe>
}

/** The next catalogue product not yet on the graph, for the «tap to add» gesture. */
fun interface SuggestNextProductUseCase {
    suspend operator fun invoke(current: List<ProductId>): Product?
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
