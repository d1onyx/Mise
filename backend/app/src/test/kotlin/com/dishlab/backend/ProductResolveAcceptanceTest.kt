package com.dishlab.backend

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductResolveAcceptanceTest {
    @Test
    fun `OFF water category snapshot resolves to recipe water tag`() = testApplication {
        application { testModule() }

        val response = client.post("/api/v1/products/resolve") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"barcode":"8594001407842","name":"Dobrá voda neperlivá","language":"cs","categories":["en:beverages-and-beverages-preparations","en:beverages","en:carbonated-drinks","en:waters","en:carbonated-waters","en:drinking-water"]}""",
            )
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status, body)
        assertTrue(body.contains("\"canonical_tags\":["), body)
        assertTrue(body.contains("\"en:water\""), body)
    }
}
