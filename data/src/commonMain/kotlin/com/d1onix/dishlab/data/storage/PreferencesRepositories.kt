package com.d1onix.dishlab.data.storage

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.repository.SavedRecipesRepository
import com.d1onix.dishlab.domain.repository.ScanHistoryRepository
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.datastore.PreferenceKey
import com.d1onyx.core.datastore.get
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object DishLabKeys {
    val SavedRecipes = PreferenceKey.StringSetKey("saved_recipes")
    /** Ordered, most recent first — a set would lose that, so it is one string. */
    val ScanHistory = PreferenceKey.StringKey("scan_history")
}

private const val HISTORY_SEPARATOR = ","
private const val HISTORY_LIMIT = 30

@ContributesBinding(AppScope::class)
@Inject
class StoredSavedRecipesRepository(
    private val storage: KeyValueStorage,
) : SavedRecipesRepository {

    override val saved: Flow<Set<RecipeId>> =
        storage.observe(DishLabKeys.SavedRecipes).map { ids ->
            ids.orEmpty().map(::RecipeId).toSet()
        }

    override suspend fun toggle(id: RecipeId) {
        val current = storage.get(DishLabKeys.SavedRecipes).orEmpty()
        val updated = if (id.value in current) current - id.value else current + id.value
        storage.put(DishLabKeys.SavedRecipes, updated)
    }
}

@ContributesBinding(AppScope::class)
@Inject
class StoredScanHistoryRepository(
    private val storage: KeyValueStorage,
) : ScanHistoryRepository {

    override val history: Flow<List<ProductId>> =
        storage.observe(DishLabKeys.ScanHistory).map { raw -> raw.decodeHistory() }

    override suspend fun add(id: ProductId) {
        val current = storage.get(DishLabKeys.ScanHistory).decodeHistory()
        val updated = (listOf(id) + current.filterNot { it == id }).take(HISTORY_LIMIT)
        storage.put(DishLabKeys.ScanHistory, updated.joinToString(HISTORY_SEPARATOR) { it.value })
    }

    override suspend fun clear() {
        storage.remove(DishLabKeys.ScanHistory)
    }
}

private fun String?.decodeHistory(): List<ProductId> =
    this?.split(HISTORY_SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.map(::ProductId)
        .orEmpty()
