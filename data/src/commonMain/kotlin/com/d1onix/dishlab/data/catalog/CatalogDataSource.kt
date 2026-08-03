package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.data.catalog.dto.RecipeDto
import com.d1onix.dishlab.data.catalog.mapper.toDomain
import com.d1onix.dishlab.data.resources.Res
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
 * Temporary bundled recipes, read once from the packaged JSON file.
 *
 * Products deliberately do not belong here: every product in the app comes
 * from the backend and is cached only after a successful server response.
 */
@SingleIn(AppScope::class)
@Inject
class RecipeCatalogDataSource(
    private val dispatchers: DispatcherProvider,
    override val logger: Logger,
) : Loggable {

    override val logTag: String = "Catalog"

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: List<Recipe>? = null

    suspend fun recipes(): List<Recipe> = mutex.withLock {
        cache ?: load().also { cache = it }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun load(): List<Recipe> = logged("load") {
        withContext(dispatchers.io) {
            json
                .decodeFromString<List<RecipeDto>>(Res.readBytes("files/recipes.json").decodeToString())
                .map { it.toDomain() }
        }
    }
}
