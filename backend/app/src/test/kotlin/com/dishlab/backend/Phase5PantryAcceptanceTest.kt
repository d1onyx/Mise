package com.dishlab.backend

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Phase5PantryAcceptanceTest {
    private fun bearer(uid: String) = "Bearer :$uid"

    @Test
    fun `user can add list filter update and tombstone inventory items`() = testApplication {
        application { testModule() }

        val add = client.post("/api/v1/pantry/inventory") {
            header(HttpHeaders.Authorization, bearer("pantry-owner"))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "ingredientId":"11111111-1111-1111-1111-111111111111",
                  "name":"Tomato",
                  "quantity":6.0,
                  "unit":"piece",
                  "location":"fridge",
                  "category":"vegetables",
                  "expiryDate":"${LocalDate.now().plusDays(2)}"
                }
                """.trimIndent(),
            )
        }
        val addBody = add.body<String>()
        assertEquals(HttpStatusCode.Created, add.status)
        assertTrue(addBody.contains("\"name\":\"Tomato\""), addBody)
        assertTrue(addBody.contains("\"quantity\":6.0"), addBody)
        val itemId = extractJsonString(addBody, "id")

        val filtered = client.get("/api/v1/pantry/inventory?location=fridge&category=vegetables&search=tom&expiry=week") {
            header(HttpHeaders.Authorization, bearer("pantry-owner"))
        }
        val filteredBody = filtered.body<String>()
        assertEquals(HttpStatusCode.OK, filtered.status)
        assertTrue(filteredBody.contains("\"total\":1"), filteredBody)
        assertTrue(filteredBody.contains("\"expiryStatus\":\"SOON\""), filteredBody)

        val updated = client.patch("/api/v1/pantry/inventory/$itemId") {
            header(HttpHeaders.Authorization, bearer("pantry-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":8.0,"location":"freezer","expiryDate":"${LocalDate.now().plusDays(30)}"}""")
        }
        val updatedBody = updated.body<String>()
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updatedBody.contains("\"quantity\":8.0"), updatedBody)
        assertTrue(updatedBody.contains("\"location\":\"freezer\""), updatedBody)

        val deleted = client.delete("/api/v1/pantry/inventory/$itemId") {
            header(HttpHeaders.Authorization, bearer("pantry-owner"))
        }
        assertEquals(HttpStatusCode.OK, deleted.status)
        assertTrue(deleted.body<String>().contains("\"deleted\":true"))

        val listAfterDelete = client.get("/api/v1/pantry/inventory?includeDeleted=false") {
            header(HttpHeaders.Authorization, bearer("pantry-owner"))
        }
        assertEquals(HttpStatusCode.OK, listAfterDelete.status)
        assertTrue(listAfterDelete.body<String>().contains("\"total\":0"))
    }

    @Test
    fun `consume and waste actions create transactions and reduce quantity to zero`() = testApplication {
        application { testModule() }
        val itemId = addInventoryItem("stock-owner", name = "Rice", quantity = 500.0, unit = "g")

        val consumed = client.post("/api/v1/pantry/inventory/$itemId/consume") {
            header(HttpHeaders.Authorization, bearer("stock-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":200.0,"reason":"cooked_recipe","recipeId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"}""")
        }
        val consumedBody = consumed.body<String>()
        assertEquals(HttpStatusCode.OK, consumed.status)
        assertTrue(consumedBody.contains("\"quantity\":300.0"), consumedBody)
        assertTrue(consumedBody.contains("CONSUME"), consumedBody)

        val wasted = client.post("/api/v1/pantry/inventory/$itemId/waste") {
            header(HttpHeaders.Authorization, bearer("stock-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":300.0,"reason":"expired"}""")
        }
        val wastedBody = wasted.body<String>()
        assertEquals(HttpStatusCode.OK, wasted.status)
        assertTrue(wastedBody.contains("\"quantity\":0.0"), wastedBody)
        assertTrue(wastedBody.contains("\"depleted\":true"), wastedBody)
        assertTrue(wastedBody.contains("WASTE"), wastedBody)

        val transactions = client.get("/api/v1/pantry/inventory/transactions") {
            header(HttpHeaders.Authorization, bearer("stock-owner"))
        }
        val transactionsBody = transactions.body<String>()
        assertEquals(HttpStatusCode.OK, transactions.status)
        assertTrue(transactionsBody.contains("\"total\":3"), transactionsBody)
        assertTrue(transactionsBody.contains("ADD"), transactionsBody)
        assertTrue(transactionsBody.contains("cooked_recipe"), transactionsBody)
        assertTrue(transactionsBody.contains("expired"), transactionsBody)
    }

    @Test
    fun `freeze updates location and suggested expiry while expiring query groups statuses`() = testApplication {
        application { testModule() }
        addInventoryItem("expiry-owner", name = "Old milk", quantity = 1.0, unit = "l", expiryDate = LocalDate.now().minusDays(1))
        addInventoryItem("expiry-owner", name = "Today yogurt", quantity = 1.0, unit = "piece", expiryDate = LocalDate.now())
        val soonId = addInventoryItem("expiry-owner", name = "Fresh herbs", quantity = 1.0, unit = "bunch", expiryDate = LocalDate.now().plusDays(2))
        addInventoryItem("expiry-owner", name = "Potatoes", quantity = 2.0, unit = "kg", expiryDate = LocalDate.now().plusDays(6))

        val frozen = client.post("/api/v1/pantry/inventory/$soonId/freeze") {
            header(HttpHeaders.Authorization, bearer("expiry-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"extraDays":30}""")
        }
        val frozenBody = frozen.body<String>()
        assertEquals(HttpStatusCode.OK, frozen.status)
        assertTrue(frozenBody.contains("\"location\":\"freezer\""), frozenBody)
        assertTrue(frozenBody.contains("FREEZE"), frozenBody)

        val expiring = client.get("/api/v1/pantry/inventory/expiring") {
            header(HttpHeaders.Authorization, bearer("expiry-owner"))
        }
        val expiringBody = expiring.body<String>()
        assertEquals(HttpStatusCode.OK, expiring.status)
        assertTrue(expiringBody.contains("\"expired\""), expiringBody)
        assertTrue(expiringBody.contains("Old milk"), expiringBody)
        assertTrue(expiringBody.contains("\"today\""), expiringBody)
        assertTrue(expiringBody.contains("Today yogurt"), expiringBody)
        assertTrue(expiringBody.contains("\"week\""), expiringBody)
        assertTrue(expiringBody.contains("Potatoes"), expiringBody)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.addInventoryItem(
        uid: String,
        name: String,
        quantity: Double,
        unit: String,
        expiryDate: LocalDate = LocalDate.now().plusDays(5),
    ): String {
        val response = client.post("/api/v1/pantry/inventory") {
            header(HttpHeaders.Authorization, bearer(uid))
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","quantity":$quantity,"unit":"$unit","location":"fridge","category":"test","expiryDate":"$expiryDate"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return extractJsonString(response.body(), "id")
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\":\\\"([^\\\"]+)\\\"")
        return regex.find(json)?.groupValues?.get(1) ?: error("Missing '$key' in $json")
    }
}
