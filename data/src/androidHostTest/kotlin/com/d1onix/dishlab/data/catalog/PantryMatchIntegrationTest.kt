package com.d1onix.dishlab.data.catalog

import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.network.NetworkConfig
import com.d1onyx.core.network.createHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/** Runs against a Food.com-backed server when DISH_LAB_INTEGRATION_BASE_URL is set. */
class PantryMatchIntegrationTest {
    @Test
    fun `connected flour and water or egg includes egg-only catalog recipe`() = runTest {
        val baseUrl = System.getenv("DISH_LAB_INTEGRATION_BASE_URL")
        assumeTrue("Set DISH_LAB_INTEGRATION_BASE_URL to run backend integration tests", !baseUrl.isNullOrBlank())
        val client = createHttpClient(
            NetworkConfig(baseUrl = baseUrl!!, isDebug = true),
            DefaultLogger(RecordingLogSink()),
        )
        try {
            val result = PantryMatchDataSource(client).match(
                ingredients = listOf("en:egg"),
                exactGroups = listOf(
                    listOf("en:flour", "en:wheat-flour"),
                    listOf("en:water", "en:mineral-water"),
                ),
                pageSize = 100,
            )
            assertTrue(result.items.any { it.recipe.id == "catalog:822" })
        } finally {
            client.close()
        }
    }
}
