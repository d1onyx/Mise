package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.ClearScanHistoryUseCase
import com.d1onix.dishlab.domain.ObserveScanHistoryUseCase
import com.d1onix.dishlab.domain.RecordScanUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ProductRepository
import com.d1onix.dishlab.domain.repository.ScanHistoryRepository
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
@Inject
class ObserveScanHistoryUseCaseImpl(
    private val history: ScanHistoryRepository,
    private val products: ProductRepository,
) : ObserveScanHistoryUseCase {
    override fun invoke(): Flow<List<Product>> = history.history.map { products.byIds(it) }
}

@ContributesBinding(AppScope::class)
@Inject
class RecordScanUseCaseImpl(
    private val history: ScanHistoryRepository,
) : RecordScanUseCase {
    override suspend fun invoke(id: ProductId) = history.add(id)
}

@ContributesBinding(AppScope::class)
@Inject
class ClearScanHistoryUseCaseImpl(
    private val history: ScanHistoryRepository,
) : ClearScanHistoryUseCase {
    override suspend fun invoke() = history.clear()
}
