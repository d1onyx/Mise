package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.data.demo.resolveDemoBarcode
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.repository.ProductRepository
import com.d1onix.dishlab.domain.repository.RecipeRepository
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Catalogue-backed products — the stand-in for the OpenFoodFacts client. */
@ContributesBinding(AppScope::class)
@Inject
class CatalogProductRepository(
    private val catalog: CatalogDataSource,
) : ProductRepository {

    override suspend fun byBarcode(barcode: String): Product? {
        val products = catalog.catalog().products
        return products.firstOrNull { it.barcode == barcode }
            // Demo fallback: an unknown barcode still resolves, so the whole app
            // is walkable without a product API. See DemoMode.
            ?: products.resolveDemoBarcode(barcode)
    }

    override suspend fun byId(id: ProductId): Product? =
        catalog.catalog().products.firstOrNull { it.id == id }

    override suspend fun byIds(ids: List<ProductId>): List<Product> {
        val byId = catalog.catalog().products.associateBy { it.id }
        return ids.mapNotNull(byId::get)
    }

    override suspend fun all(): List<Product> = catalog.catalog().products
}

@ContributesBinding(AppScope::class)
@Inject
class CatalogRecipeRepository(
    private val catalog: CatalogDataSource,
) : RecipeRepository {

    override suspend fun all(): List<Recipe> = catalog.catalog().recipes

    override suspend fun byId(id: RecipeId): Recipe? =
        catalog.catalog().recipes.firstOrNull { it.id == id }

    override suspend fun forProducts(productIds: List<ProductId>): List<Recipe> {
        if (productIds.isEmpty()) return all()
        // Ranked by how much of the scanned set a recipe actually uses.
        return all()
            .filter { recipe -> recipe.productIds.any { it in productIds } }
            .sortedByDescending { recipe -> recipe.productIds.count { it in productIds } }
    }
}
