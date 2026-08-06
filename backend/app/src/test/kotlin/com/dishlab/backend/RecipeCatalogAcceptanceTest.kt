package com.dishlab.backend

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeCatalogAcceptanceTest {
    private val token = "Bearer :catalog-test-user"

    @Test
    fun `core catalog routes accept anonymous and bearer requests`() = testApplication {
        application { testModule() }

        suspend fun resolve(headers: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}) =
            client.post("/api/v1/products/resolve") {
                contentType(ContentType.Application.Json)
                setBody("""{"barcode":"8594001234567","name":"Test tea"}""")
                headers()
            }
        suspend fun pantryMatch(headers: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}) =
            client.get("/api/v1/recipe-catalog/pantry-match?ingredient=en:flour", headers)
        suspend fun details(headers: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}) =
            client.get("/api/v1/recipe-catalog/catalog:38", headers)

        listOf(
            resolve(), pantryMatch(), details(),
            resolve { header(HttpHeaders.Authorization, token) },
            pantryMatch { header(HttpHeaders.Authorization, token) },
            details { header(HttpHeaders.Authorization, token) },
        ).forEach { response ->
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
            assertTrue(response.bodyAsText().isNotBlank())
        }
    }

    @Test
    fun `public catalog routes are rate limited by IP`() = testApplication {
        application { testModule() }

        repeat(20) {
            val response = client.get("/api/v1/recipe-catalog/pantry-match?ingredient=en:flour") {
                header("X-Forwarded-For", "198.51.100.42")
            }
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        }

        val limited = client.get("/api/v1/recipe-catalog/catalog:38") {
            header("X-Forwarded-For", "198.51.100.42")
        }
        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertEquals("20", limited.headers["X-RateLimit-Limit"])
        assertTrue(limited.bodyAsText().contains("RATE_LIMIT_EXCEEDED"))
    }

    @Test
    fun `public catalog details never expose another users bookmark`() = testApplication {
        application { testModule() }

        val search = client.get("/api/v1/recipe-catalog?q=berry&pageSize=2") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, search.status)
        val searchBody = search.bodyAsText()
        assertTrue(searchBody.contains("\"total\":"), searchBody)
        assertTrue(searchBody.contains("\"id\":\"catalog:"), searchBody)

        val details = client.get("/api/v1/recipe-catalog/catalog:38") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, details.status)
        val detailsBody = details.bodyAsText()
        assertTrue(detailsBody.contains("Low-Fat Berry Blue Frozen Dessert"), detailsBody)
        assertTrue(detailsBody.contains("\"ingredients\":"), detailsBody)
        assertTrue(detailsBody.contains("\"canonical_tags\":[\"en:"), detailsBody)
        assertTrue(detailsBody.contains("\"steps\":"), detailsBody)

        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/v1/recipe-catalog/catalog:38/bookmark") {
                header(HttpHeaders.Authorization, token)
            }.status,
        )
        val publicDetails = client.get("/api/v1/recipe-catalog/catalog:38").bodyAsText()
        assertTrue(publicDetails.contains("\"bookmarked\":false"), publicDetails)

        assertEquals(
            HttpStatusCode.OK,
            client.delete("/api/v1/recipe-catalog/catalog:38/bookmark") {
                header(HttpHeaders.Authorization, token)
            }.status,
        )
    }

    @Test
    fun `exact pantry match requires all selected products and partial requires at least one`() =
        testApplication {
            application { testModule() }

            // butter/flour/egg/water — Zebra Cake (catalog:513667) lists egg and water twice, so the
            // existence-based matching must tolerate duplicate ingredient rows.
            val p4 = "ingredient=en:butter&ingredient=en:flour&ingredient=en:egg&ingredient=en:water"
            val p5 = "$p4&ingredient=en:salt"

            suspend fun pantryMatchTotal(query: String): Int {
                val body = client.get("/api/v1/recipe-catalog/pantry-match?$query&pageSize=100") {
                    header(HttpHeaders.Authorization, token)
                }.bodyAsText()
                return Regex("\"total\":(\\d+)").find(body)!!.groupValues[1].toInt()
            }

            // Exact = recipe must contain EVERY selected product → adding another required product
            // can only shrink the result set.
            val exact4 = pantryMatchTotal("$p4&exactMatch=true")
            val exact5 = pantryMatchTotal("$p5&exactMatch=true")
            assertTrue(exact4 > 0, "exact match should find recipes containing all 4 products")
            assertTrue(exact5 <= exact4, "requiring an extra product must not grow the exact result set")

            // Non-exact = at least one selected product → adding a product can only grow the set,
            // and it is always broader than the exact set for the same products.
            val partial4 = pantryMatchTotal("$p4&partialMatchOnly=true")
            val partial5 = pantryMatchTotal("$p5&partialMatchOnly=true")
            assertTrue(partial5 >= partial4, "adding a product must not shrink the non-exact result set")
            assertTrue(partial4 >= exact4, "non-exact must be at least as broad as exact for the same products")

            // Both modes rank coverage 100%→0%; the top of the non-exact page is fully-covered.
            val partialBody = client.get("/api/v1/recipe-catalog/pantry-match?$p4&partialMatchOnly=true&pageSize=100") {
                header(HttpHeaders.Authorization, token)
            }.bodyAsText()
            val partialPercents = Regex("\"match_percent\":(\\d+)")
                .findAll(partialBody).map { it.groupValues[1].toInt() }.toList()
            assertTrue(partialPercents.isNotEmpty() && partialPercents.all { it in 1..100 }, partialBody)
            assertTrue(partialPercents.contains(100), "non-exact results must include fully covered recipes")
        }

    @Test
    fun `exact match groups synonym tags so one product with several tags still matches`() =
        testApplication {
            application { testModule() }

            // A pantry where each product carries two synonym tags (flour+wheat-flour, water+mineral-
            // water, milk+2-milk). The flat union is sent for the % computation.
            val flat = "ingredient=en:flour&ingredient=en:wheat-flour&ingredient=en:water" +
                "&ingredient=en:mineral-water&ingredient=en:milk&ingredient=en:2-milk" +
                "&ingredient=en:egg&ingredient=en:butter"

            suspend fun pantryMatchTotal(query: String): Int {
                val body = client.get("/api/v1/recipe-catalog/pantry-match?$query&pageSize=100") {
                    header(HttpHeaders.Authorization, token)
                }.bodyAsText()
                return Regex("\"total\":(\\d+)").find(body)!!.groupValues[1].toInt()
            }

            // Without grouping, exact requires EVERY tag present (incl. wheat-flour AND mineral-water
            // AND 2-milk) — no recipe lists all of those → 0. This is the bug grouping fixes.
            assertEquals(0, pantryMatchTotal("$flat&exactMatch=true"))

            // Grouped: each product is an OR-group of its synonym tags, so a recipe with any flour +
            // any water + any milk + egg + butter qualifies.
            val groups = "exactGroup=en:flour,en:wheat-flour&exactGroup=en:water,en:mineral-water" +
                "&exactGroup=en:milk,en:2-milk&exactGroup=en:egg&exactGroup=en:butter"
            assertTrue(
                pantryMatchTotal("$flat&$groups&exactMatch=true") > 0,
                "grouped exact match must find recipes that contain one tag from every product group",
            )
        }

    @Test
    fun `exact groups are AND clauses while plain ingredients are OR clauses`() = testApplication {
        application { testModule() }

        val body = client.get(
            "/api/v1/recipe-catalog/pantry-match?" +
                "exactGroup=en:flour,en:wheat-flour&exactGroup=en:water,en:mineral-water" +
                "&ingredient=en:egg&pageSize=100",
        ).bodyAsText()

        assertTrue(body.contains("Low-Fat Berry Blue Frozen Dessert"), body)
        assertTrue(body.contains("Egg-only omelette"), body)
        assertTrue(!body.contains("Flour-only flatbread"), body)
    }
}
