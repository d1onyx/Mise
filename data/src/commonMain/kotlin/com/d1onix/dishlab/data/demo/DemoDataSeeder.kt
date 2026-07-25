package com.d1onix.dishlab.data.demo

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.repository.SavedRecipesRepository
import com.d1onix.dishlab.domain.repository.ScanHistoryRepository
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.datastore.PreferenceKey
import com.d1onyx.core.datastore.getOrDefault
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logged
import dev.zacsweers.metro.Inject

/**
 * Fills Saved and History once, on first launch, so no screen opens empty
 * during a walkthrough.
 *
 * Guarded by its own flag rather than by «is the list empty», so clearing the
 * history or unsaving everything stays cleared — the seed happens once per
 * installation, not once per empty list.
 */
@Inject
class DemoDataSeeder(
    private val storage: KeyValueStorage,
    private val saved: SavedRecipesRepository,
    private val history: ScanHistoryRepository,
    override val logger: Logger,
) : Loggable {

    override val logTag: String = "Demo"

    suspend fun seedIfNeeded(): Unit = logged("seed") {
        if (storage.getOrDefault(SeededKey, false)) return@logged

        DemoMode.savedRecipeIds.forEach { id -> saved.toggle(RecipeId(id)) }
        // `add` prepends, so seeding oldest-first leaves the newest on top.
        DemoMode.historyProductIds.forEach { id -> history.add(ProductId(id)) }

        storage.put(SeededKey, true)
    }

    private companion object {
        val SeededKey = PreferenceKey.BooleanKey("demo_data_seeded")
    }
}
