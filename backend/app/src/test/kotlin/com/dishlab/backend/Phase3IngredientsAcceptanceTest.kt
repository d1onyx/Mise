package com.dishlab.backend

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Phase3IngredientsAcceptanceTest {
    private val token = "Bearer :phase3-user"

    @Test
    fun `ingredient search returns seeded catalog with pagination and nutrition completeness`() = testApplication {
        application { testModule() }

        val response = client.get("/api/v1/ingredients/search?q=tom&page=1&pageSize=5") {
            header(HttpHeaders.Authorization, token)
        }
        val body = response.body<String>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("\"page\":1"), body)
        assertTrue(body.contains("\"page_size\":5"), body)
        assertTrue(body.contains("\"name\":\"Tomato\""), body)
        assertTrue(body.contains("\"nutrition_complete\":true"), body)
    }

    @Test
    fun `custom ingredient creation validates title and exposes created ingredient details`() = testApplication {
        application { testModule() }

        val invalid = client.post("/api/v1/ingredients/custom") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"A","default_unit":"g"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue(invalid.body<String>().contains("VALIDATION_ERROR"))

        val created = client.post("/api/v1/ingredients/custom") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name":"Домашній соус",
                  "default_unit":"g",
                  "allergens":["milk"],
                  "nutrition_per100g":{"calories":120.0,"protein":2.0,"fat":5.0,"carbs":12.0}
                }
                """.trimIndent(),
            )
        }
        val createdBody = created.body<String>()

        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(createdBody.contains("\"name\":\"Домашній соус\""), createdBody)
        assertTrue(createdBody.contains("\"nutrition_complete\":true"), createdBody)
        val id = extractJsonString(createdBody, "id")

        val details = client.get("/api/v1/ingredients/$id") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, details.status)
        assertTrue(details.body<String>().contains("\"milk\""))
    }

    @Test
    fun `unit conversion supports compatible units and reports density required or impossible`() = testApplication {
        application { testModule() }

        val grams = client.post("/api/v1/tools/unit-convert") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"amount":1.5,"from_unit":"kg","to_unit":"g"}""")
        }
        val gramsBody = grams.body<String>()
        assertEquals(HttpStatusCode.OK, grams.status)
        assertTrue(gramsBody.contains("\"amount\":1500.0"), gramsBody)
        assertTrue(gramsBody.contains("\"unit\":\"g\""), gramsBody)

        val densityRequired = client.post("/api/v1/tools/unit-convert") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"amount":250.0,"from_unit":"ml","to_unit":"g"}""")
        }
        val densityBody = densityRequired.body<String>()
        assertEquals(HttpStatusCode.BadRequest, densityRequired.status)
        assertTrue(densityBody.contains("DENSITY_REQUIRED"), densityBody)

        val impossible = client.post("/api/v1/tools/unit-convert") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"amount":2.0,"from_unit":"piece","to_unit":"g"}""")
        }
        val impossibleBody = impossible.body<String>()
        assertEquals(HttpStatusCode.BadRequest, impossible.status)
        assertTrue(impossibleBody.contains("CONVERSION_IMPOSSIBLE"), impossibleBody)
    }

    @Test
    fun `substitute lookup returns known alternatives for ingredient`() = testApplication {
        application { testModule() }

        val search = client.get("/api/v1/ingredients/search?q=milk&page=1&pageSize=5") {
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        val milkId = extractJsonString(search, "id")

        val substitutes = client.get("/api/v1/ingredients/$milkId/substitutes") {
            header(HttpHeaders.Authorization, token)
        }
        val body = substitutes.body<String>()

        assertEquals(HttpStatusCode.OK, substitutes.status)
        assertTrue(body.contains("Oat milk"), body)
        assertTrue(body.contains("\"reason\":\"dairy_free\""), body)
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\":\\\"([^\\\"]+)\\\"")
        return regex.find(json)?.groupValues?.get(1) ?: error("Missing '$key' in $json")
    }
}
