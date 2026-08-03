package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.GetProductByBarcodeUseCase
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.repository.ProductRepository
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class GetProductByBarcodeUseCaseImpl(
    private val products: ProductRepository,
) : GetProductByBarcodeUseCase {
    override suspend fun invoke(barcode: String): Product? = products.byBarcode(barcode.trim())
}

@ContributesBinding(AppScope::class)
@Inject
class GetProductsUseCaseImpl(
    private val products: ProductRepository,
) : GetProductsUseCase {
    override suspend fun invoke(ids: List<ProductId>): List<Product> = products.byIds(ids)
}
