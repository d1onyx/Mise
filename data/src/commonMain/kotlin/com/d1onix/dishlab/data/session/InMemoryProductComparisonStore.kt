package com.d1onix.dishlab.data.session

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ProductComparisonStore
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class InMemoryProductComparisonStore : ProductComparisonStore {
    private val mutableProducts = MutableStateFlow<List<ProductId>>(emptyList())
    override val products: StateFlow<List<ProductId>> = mutableProducts.asStateFlow()

    override suspend fun add(id: ProductId): Boolean {
        val current = mutableProducts.value
        if (id in current) return true
        if (current.size >= ProductComparisonStore.MAX_PRODUCTS) return false
        mutableProducts.value = current + id
        return true
    }

    override suspend fun remove(id: ProductId) {
        mutableProducts.update { products -> products.filterNot { it == id } }
    }

    override suspend fun clear() {
        mutableProducts.value = emptyList()
    }
}
