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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Phase7CookingSessionsAcceptanceTest {
    private fun bearer(uid: String) = "Bearer :$uid"

    @Test
    fun `starting and updating cooking session persists current step and status`() = testApplication {
        application { testModule() }
        val recipeId = createRecipe("cook-owner", "Step soup", "22222222-2222-2222-2222-222222222222", "Carrot", 2.0, "piece")

        val started = client.post("/api/v1/cooking-sessions") {
            header(HttpHeaders.Authorization, bearer("cook-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"recipeId":"$recipeId","servings":2}""")
        }
        val startedBody = started.body<String>()
        assertEquals(HttpStatusCode.Created, started.status)
        assertTrue(startedBody.contains("\"status\":\"IN_PROGRESS\""), startedBody)
        assertTrue(startedBody.contains("\"currentStepPosition\":1"), startedBody)
        val sessionId = extractJsonString(startedBody, "id")

        val updated = client.patch("/api/v1/cooking-sessions/$sessionId") {
            header(HttpHeaders.Authorization, bearer("cook-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"currentStepPosition":2,"status":"PAUSED"}""")
        }
        val updatedBody = updated.body<String>()
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updatedBody.contains("\"currentStepPosition\":2"), updatedBody)
        assertTrue(updatedBody.contains("\"status\":\"PAUSED\""), updatedBody)

        val fetched = client.get("/api/v1/cooking-sessions/$sessionId") {
            header(HttpHeaders.Authorization, bearer("cook-owner"))
        }
        val fetchedBody = fetched.body<String>()
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertTrue(fetchedBody.contains("Step soup"), fetchedBody)
        assertTrue(fetchedBody.contains("\"status\":\"PAUSED\""), fetchedBody)
    }

    @Test
    fun `timers can be created updated and deleted for a cooking session`() = testApplication {
        application { testModule() }
        val recipeId = createRecipe("timer-owner", "Timer rice", "33333333-3333-3333-3333-333333333333", "Rice", 100.0, "g")
        val sessionId = startCookingSession("timer-owner", recipeId, servings = 1)

        val created = client.post("/api/v1/timers") {
            header(HttpHeaders.Authorization, bearer("timer-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId":"$sessionId","label":"Boil rice","durationSeconds":600,"stepPosition":1}""")
        }
        val createdBody = created.body<String>()
        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(createdBody.contains("\"label\":\"Boil rice\""), createdBody)
        assertTrue(createdBody.contains("\"status\":\"RUNNING\""), createdBody)
        val timerId = extractJsonString(createdBody, "id")

        val updated = client.patch("/api/v1/timers/$timerId") {
            header(HttpHeaders.Authorization, bearer("timer-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"remainingSeconds":300,"status":"PAUSED"}""")
        }
        val updatedBody = updated.body<String>()
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updatedBody.contains("\"remainingSeconds\":300"), updatedBody)
        assertTrue(updatedBody.contains("\"status\":\"PAUSED\""), updatedBody)

        val deleted = client.delete("/api/v1/timers/$timerId") {
            header(HttpHeaders.Authorization, bearer("timer-owner"))
        }
        assertEquals(HttpStatusCode.OK, deleted.status)
        assertTrue(deleted.body<String>().contains("\"deleted\":true"))
    }

    @Test
    fun `complete cooking session auto deducts scaled inventory and creates nutrition log`() = testApplication {
        application { testModule() }
        val riceId = "44444444-4444-4444-4444-444444444444"
        val recipeId = createRecipe("complete-owner", "Family rice", riceId, "Rice", 100.0, "g", servings = 2)
        addInventoryItem("complete-owner", riceId, "Rice", 500.0, "g")
        val sessionId = startCookingSession("complete-owner", recipeId, servings = 3)

        val completed = client.post("/api/v1/cooking-sessions/$sessionId/complete") {
            header(HttpHeaders.Authorization, bearer("complete-owner"))
            contentType(ContentType.Application.Json)
            setBody("""{"deductInventory":true,"createNutritionLog":true,"notes":"Family dinner"}""")
        }
        val completedBody = completed.body<String>()
        assertEquals(HttpStatusCode.OK, completed.status)
        assertTrue(completedBody.contains("\"status\":\"COMPLETED\""), completedBody)
        assertTrue(completedBody.contains("\"inventoryTransactions\""), completedBody)
        assertTrue(completedBody.contains("\"quantityDelta\":-150.0"), completedBody)
        assertTrue(completedBody.contains("\"nutritionLog\""), completedBody)
        assertTrue(completedBody.contains("Family rice"), completedBody)

        val inventory = client.get("/api/v1/pantry/inventory?search=Rice") {
            header(HttpHeaders.Authorization, bearer("complete-owner"))
        }
        val inventoryBody = inventory.body<String>()
        assertEquals(HttpStatusCode.OK, inventory.status)
        assertTrue(inventoryBody.contains("\"quantity\":350.0"), inventoryBody)

        val transactions = client.get("/api/v1/pantry/inventory/transactions") {
            header(HttpHeaders.Authorization, bearer("complete-owner"))
        }
        val transactionsBody = transactions.body<String>()
        assertEquals(HttpStatusCode.OK, transactions.status)
        assertTrue(transactionsBody.contains("cooking_session_completed"), transactionsBody)
        assertTrue(transactionsBody.contains("CONSUME"), transactionsBody)
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createRecipe(
        uid: String,
        title: String,
        ingredientId: String,
        ingredientName: String,
        amount: Double,
        unit: String,
        servings: Int = 1,
    ): String {
        val response = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, bearer(uid))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "title":"$title",
                  "servings":$servings,
                  "visibility":"FAMILY",
                  "ingredients":[{"ingredientId":"$ingredientId","name":"$ingredientName","amount":$amount,"unit":"$unit"}],
                  "steps":[{"position":1,"text":"Prepare"},{"position":2,"text":"Cook"}],
                  "tags":["cooking"]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return extractJsonString(response.body(), "id")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.startCookingSession(uid: String, recipeId: String, servings: Int): String {
        val response = client.post("/api/v1/cooking-sessions") {
            header(HttpHeaders.Authorization, bearer(uid))
            contentType(ContentType.Application.Json)
            setBody("""{"recipeId":"$recipeId","servings":$servings}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return extractJsonString(response.body(), "id")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.addInventoryItem(
        uid: String,
        ingredientId: String,
        name: String,
        quantity: Double,
        unit: String,
    ): String {
        val response = client.post("/api/v1/pantry/inventory") {
            header(HttpHeaders.Authorization, bearer(uid))
            contentType(ContentType.Application.Json)
            setBody("""{"ingredientId":"$ingredientId","name":"$name","quantity":$quantity,"unit":"$unit","location":"pantry","category":"staples"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return extractJsonString(response.body(), "id")
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\":\\\"([^\\\"]+)\\\"")
        return regex.find(json)?.groupValues?.get(1) ?: error("Missing '$key' in $json")
    }
}
