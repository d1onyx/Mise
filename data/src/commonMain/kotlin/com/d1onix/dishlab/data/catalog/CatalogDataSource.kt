package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.data.catalog.dto.ProductDto
import com.d1onix.dishlab.data.catalog.dto.RecipeDto
import com.d1onix.dishlab.data.catalog.mapper.toDomain
import com.d1onix.dishlab.data.resources.Res
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onyx.core.essentials.coroutines.DispatcherProvider
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logged
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The demo catalogue, read once from the bundled JSON files.
 *
 * The dataset lives in `composeResources/files/` rather than in Kotlin so that
 * swapping it for the OpenFoodFacts client is a change to this module only —
 * the domain interfaces and every screen stay as they are.
 */
@SingleIn(AppScope::class)
@Inject
class CatalogDataSource(
    private val dispatchers: DispatcherProvider,
    override val logger: Logger,
) : Loggable {

    override val logTag: String = "Catalog"

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: Catalog? = null

    suspend fun catalog(): Catalog = mutex.withLock {
        cache ?: load().also { cache = it }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun load(): Catalog = logged("load") {
        withContext(dispatchers.io) {
            val products = json
                .decodeFromString<List<ProductDto>>(Res.readBytes("files/products.json").decodeToString())
                .map { it.toDomain() }
            val recipes = json
                .decodeFromString<List<RecipeDto>>(Res.readBytes("files/recipes.json").decodeToString())
                .map { it.toDomain() }
            Catalog(products = products, recipes = recipes)
        }
    }
}

data class Catalog(
    val products: List<Product>,
    val recipes: List<Recipe>,
)
