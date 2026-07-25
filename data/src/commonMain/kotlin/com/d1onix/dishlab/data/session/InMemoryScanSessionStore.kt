package com.d1onix.dishlab.data.session

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The current combination, held in memory for the life of the process.
 *
 * Deliberately not persisted: reopening the app should start from the home
 * screen, not from yesterday's shopping trip.
 */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class InMemoryScanSessionStore : ScanSessionStore {

    private val _products = MutableStateFlow<List<ProductId>>(emptyList())
    override val products: StateFlow<List<ProductId>> = _products.asStateFlow()

    override fun add(id: ProductId) {
        _products.update { current -> if (id in current) current else current + id }
    }

    override fun remove(id: ProductId) {
        _products.update { current -> current - id }
    }

    override fun reset(ids: List<ProductId>) {
        _products.value = ids.distinct()
    }
}
