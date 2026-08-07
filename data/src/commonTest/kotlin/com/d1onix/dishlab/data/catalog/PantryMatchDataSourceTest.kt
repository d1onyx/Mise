package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.network.NetworkConfig
import com.d1onyx.core.network.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PantryMatchDataSourceTest {
    @Test
    fun `pantry match returns catalog recipes and sends query groups`() = runTest {
        var path = ""
        var parameters = emptyMap<String, List<String>>()
        val client = createHttpClient(
            config = NetworkConfig(baseUrl = "https://api.example.com/", isDebug = true),
            logger = DefaultLogger(RecordingLogSink()),
            engine = MockEngine { request ->
            path = request.url.encodedPath
            parameters = request.url.parameters.entries().associate { it.key to it.value }
            respond(
                content = """{"items":[{"recipe":{"id":"catalog:42","title":"Oat Bowl","category":"Breakfast","ingredients":[{"name":"Oats","quantity":"1 cup"}],"steps":[{"position":1,"text":"Cook"}]},"matched_count":1,"total_ingredients":1,"match_percent":100}],"page":1,"page_size":20,"total":1}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        },
        )

        val result = PantryMatchDataSource(client).match(
            ingredients = listOf("en:oats"),
            exactGroups = listOf(listOf("en:oats", "en:rolled-oats")),
        )

        assertEquals("/api/v1/recipe-catalog/pantry-match", path)
        assertEquals(listOf("en:oats"), parameters["ingredient"])
        assertEquals(listOf("en:oats,en:rolled-oats"), parameters["exactGroup"])
        assertEquals("Oat Bowl", result.items.single().toDomain().name)
    }

    @Test
    fun `pantry match requests only the top results page`() = runTest {
        val requestedPages = mutableListOf<String?>()
        val client = createHttpClient(
            config = NetworkConfig(baseUrl = "https://api.example.com/", isDebug = true),
            logger = DefaultLogger(RecordingLogSink()),
            engine = MockEngine { request ->
                val page = request.url.parameters["page"]
                requestedPages += page
                respond(
                    content = """{"items":[{"recipe":{"id":"catalog:one","title":"catalog:one","ingredients":[],"steps":[]},"matched_count":1,"total_ingredients":1,"match_percent":100}],"page":$page,"page_size":1,"total":165896}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        )

        val recipes = PantryMatchDataSource(client).match(ingredients = listOf("en:oats"))

        assertEquals(listOf<String?>("1"), requestedPages)
        assertEquals(listOf("catalog:one"), recipes.items.map { it.recipe.id })
    }
}

class RecipeCatalogRemoteDataSourceTest {
    @Test
    fun `recipe detail requests catalog recipe id and preserves preparation steps`() = runTest {
        var path = ""
        val client = createHttpClient(
            config = NetworkConfig(baseUrl = "https://api.example.com/", isDebug = true),
            logger = DefaultLogger(RecordingLogSink()),
            engine = MockEngine { request ->
                path = request.url.encodedPath
                respond(
                    content = """{"id":"catalog:15324","title":"Crackers","ingredients":[],"steps":[{"position":2,"text":"Bake"},{"position":1,"text":"Mix"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            },
        )

        val recipe = RecipeCatalogRemoteDataSource(client).recipe(RecipeId("catalog:15324")).toDomain()

        assertEquals("/api/v1/recipe-catalog/catalog:15324", path)
        assertEquals(listOf("Mix", "Bake"), recipe.steps.map { it.description })
    }
}
