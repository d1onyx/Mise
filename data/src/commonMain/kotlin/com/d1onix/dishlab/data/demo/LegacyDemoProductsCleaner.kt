package com.d1onix.dishlab.data.demo

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ScanHistoryRepository
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.datastore.PreferenceKey
import com.d1onyx.core.datastore.getOrDefault
import dev.zacsweers.metro.Inject

/** Removes products persisted by the old bundled catalogue exactly once. */
@Inject
class LegacyDemoProductsCleaner(
    private val storage: KeyValueStorage,
    private val session: ScanSessionStore,
    private val history: ScanHistoryRepository,
) {
    suspend fun removeIfNeeded() {
        if (storage.getOrDefault(CleanedKey, false)) return

        LEGACY_PRODUCT_IDS.forEach { id ->
            session.remove(ProductId(id))
            history.remove(ProductId(id))
        }
        storage.put(CleanedKey, true)
    }

    private companion object {
        val CleanedKey = PreferenceKey.BooleanKey("legacy_demo_products_removed")
        val LEGACY_PRODUCT_IDS = setOf("oats", "banana", "honey", "yogurt")
    }
}
