package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.data.catalog.off.ClientProductSnapshotDto
import com.d1onix.dishlab.data.catalog.off.OpenFoodFactsProductDataSource
import com.d1onix.dishlab.domain.model.ProductDataOrigin
import com.d1onyx.core.datastore.InMemoryKeyValueStorage
import com.d1onyx.core.essentials.exceptions.ConnectionException
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.network.NetworkConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The cache is what lets the graph and scan history render offline, so it has to
 * keep device-fallback products. What it must not do is answer a fresh scan with
 * one: that would pin a product to Open Food Facts data for the life of the
 * install because a single lookup happened during an outage.
 */
class BackendProductRepositoryTest {

    @Test
    fun `a canonical product is served from the cache without a lookup`() = runTest {
        val cache = cache()
        cache.put(canonicalDto.toDomain())
        val openFoodFacts = FakeOpenFoodFacts { error("the network must not be touched") }

        val product = repository(cache, openFoodFacts).byBarcode(BARCODE)

        assertEquals(ProductDataOrigin.Canonical, product?.dataOrigin)
        assertEquals(0, openFoodFacts.calls)
    }

    @Test
    fun `a cached device fallback is retried and replaced once the service returns`() = runTest {
        val cache = cache()
        cache.put(fallbackProduct)
        val openFoodFacts = FakeOpenFoodFacts { snapshot }
        val repository = repository(cache, openFoodFacts, backend = { canonicalDto })

        val product = repository.byBarcode(BARCODE)

        assertEquals(1, openFoodFacts.calls)
        assertEquals(ProductDataOrigin.Canonical, product?.dataOrigin)
        assertEquals("Rolled Oats", product?.name)
        // The stand-in is gone from the cache, so the banner stops appearing.
        assertEquals(ProductDataOrigin.Canonical, cache.byBarcode(BARCODE)?.dataOrigin)
    }

    @Test
    fun `a cached device fallback is reused while the service is still down`() = runTest {
        val cache = cache()
        cache.put(fallbackProduct)
        val repository = repository(
            cache = cache,
            openFoodFacts = FakeOpenFoodFacts { snapshot },
            backend = { throw ConnectionException() },
        )

        val product = repository.byBarcode(BARCODE)

        assertEquals(ProductDataOrigin.DeviceFallback, product?.dataOrigin)
    }

    @Test
    fun `a cached product answers when Open Food Facts itself is unreachable`() = runTest {
        val cache = cache()
        cache.put(fallbackProduct)
        val repository = repository(
            cache = cache,
            openFoodFacts = FakeOpenFoodFacts { throw ConnectionException() },
        )

        val product = repository.byBarcode(BARCODE)

        assertEquals(ProductDataOrigin.DeviceFallback, product?.dataOrigin)
    }

    @Test
    fun `an unknown barcode with nothing cached stays unknown`() = runTest {
        val repository = repository(cache(), FakeOpenFoodFacts { null })

        assertNull(repository.byBarcode(BARCODE))
    }

    @Test
    fun `the graph still renders fallback products by id`() = runTest {
        val cache = cache()
        cache.put(fallbackProduct)

        // byId serves the graph and history, which persist identifiers only —
        // filtering fallbacks out here would empty the graph during an outage.
        val product = repository(cache, FakeOpenFoodFacts { null })
            .byId(fallbackProduct.id)

        assertEquals(ProductDataOrigin.DeviceFallback, product?.dataOrigin)
        assertTrue(cache.all().isNotEmpty())
    }

    private fun cache() = RemoteProductCache(InMemoryKeyValueStorage(), Json)

    private fun repository(
        cache: RemoteProductCache,
        openFoodFacts: OpenFoodFactsProductDataSource,
        backend: suspend () -> BackendProductDto = { canonicalDto },
    ) = BackendProductRepository(
        backend = FakeBackend(backend),
        openFoodFacts = openFoodFacts,
        remoteCache = cache,
    )

    private val canonicalDto = BackendProductDto(
        barcode = BARCODE,
        name = "Rolled Oats",
        category = "Grains",
        calories = "370",
        protein = "13",
        fat = "7",
        carbs = "60",
        nutritionGrade = "a",
    )

    private val fallbackProduct = BackendProductDto(barcode = BARCODE, name = "Oats (device)")
        .toDomain()
        .copy(dataOrigin = ProductDataOrigin.DeviceFallback)

    private val snapshot = ClientProductSnapshotDto(barcode = BARCODE, name = "Rolled Oats")

    private class FakeBackend(
        private val respond: suspend () -> BackendProductDto,
    ) : BackendProductDataSource(HttpClient(MockEngine { respondOk() })) {
        override suspend fun resolveClientProduct(snapshot: ClientProductSnapshotDto) = respond()
    }

    private class FakeOpenFoodFacts(
        private val respond: suspend () -> ClientProductSnapshotDto?,
    ) : OpenFoodFactsProductDataSource(
        config = NetworkConfig(baseUrl = "http://localhost/", isDebug = true),
        logger = logger,
        json = Json,
    ) {
        var calls = 0
            private set

        override suspend fun findByBarcode(barcode: String): ClientProductSnapshotDto? {
            calls++
            return respond()
        }

        private companion object {
            val logger: Logger = DefaultLogger(RecordingLogSink())
        }
    }

    private companion object {
        const val BARCODE = "4011200296908"
    }
}
