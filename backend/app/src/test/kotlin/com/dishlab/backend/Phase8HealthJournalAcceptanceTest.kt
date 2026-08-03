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

class Phase8HealthJournalAcceptanceTest {
    private fun bearer(uid: String) = "Bearer :$uid"

    @Test
    fun `nutrition log CRUD feeds day week month and year dashboard aggregates`() = testApplication {
        application { testModule() }
        val today = LocalDate.now()

        val created = client.post("/api/v1/me/nutrition-logs") {
            header(HttpHeaders.Authorization, bearer("health-user"))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "date":"$today",
                  "mealType":"DINNER",
                  "title":"Rice bowl",
                  "calories":520.0,
                  "proteinGrams":22.0,
                  "carbsGrams":72.0,
                  "fatGrams":14.0,
                  "incomplete":false
                }
                """.trimIndent(),
            )
        }
        val createdBody = created.body<String>()
        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(createdBody.contains("\"title\":\"Rice bowl\""), createdBody)
        val logId = extractJsonString(createdBody, "id")

        val updated = client.patch("/api/v1/me/nutrition-logs/$logId") {
            header(HttpHeaders.Authorization, bearer("health-user"))
            contentType(ContentType.Application.Json)
            setBody("""{"calories":600.0,"incomplete":true,"warning":"Estimated from photo"}""")
        }
        val updatedBody = updated.body<String>()
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updatedBody.contains("\"calories\":600.0"), updatedBody)
        assertTrue(updatedBody.contains("\"incomplete\":true"), updatedBody)
        assertTrue(updatedBody.contains("Estimated from photo"), updatedBody)

        val list = client.get("/api/v1/me/nutrition-logs?from=$today&to=$today") {
            header(HttpHeaders.Authorization, bearer("health-user"))
        }
        val listBody = list.body<String>()
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(listBody.contains("\"total\":1"), listBody)
        assertTrue(listBody.contains("Rice bowl"), listBody)

        val dashboard = client.get("/api/v1/me/nutrition-dashboard?date=$today") {
            header(HttpHeaders.Authorization, bearer("health-user"))
        }
        val dashboardBody = dashboard.body<String>()
        assertEquals(HttpStatusCode.OK, dashboard.status)
        assertTrue(dashboardBody.contains("\"day\""), dashboardBody)
        assertTrue(dashboardBody.contains("\"week\""), dashboardBody)
        assertTrue(dashboardBody.contains("\"month\""), dashboardBody)
        assertTrue(dashboardBody.contains("\"year\""), dashboardBody)
        assertTrue(dashboardBody.contains("\"calories\":600.0"), dashboardBody)
        assertTrue(dashboardBody.contains("\"incompleteCount\":1"), dashboardBody)

        val deleted = client.delete("/api/v1/me/nutrition-logs/$logId") {
            header(HttpHeaders.Authorization, bearer("health-user"))
        }
        assertEquals(HttpStatusCode.OK, deleted.status)
        assertTrue(deleted.body<String>().contains("\"deleted\":true"))
    }

    @Test
    fun `water quick add and weight logs are persisted for journal screens`() = testApplication {
        application { testModule() }
        val today = LocalDate.now()

        val water = client.post("/api/v1/me/water-logs") {
            header(HttpHeaders.Authorization, bearer("journal-user"))
            contentType(ContentType.Application.Json)
            setBody("""{"amountMl":350,"date":"$today"}""")
        }
        val waterBody = water.body<String>()
        assertEquals(HttpStatusCode.Created, water.status)
        assertTrue(waterBody.contains("\"amountMl\":350"), waterBody)

        val weight = client.post("/api/v1/me/weight-logs") {
            header(HttpHeaders.Authorization, bearer("journal-user"))
            contentType(ContentType.Application.Json)
            setBody("""{"weightKg":72.4,"date":"$today","note":"Morning"}""")
        }
        val weightBody = weight.body<String>()
        assertEquals(HttpStatusCode.Created, weight.status)
        assertTrue(weightBody.contains("\"weightKg\":72.4"), weightBody)

        val weights = client.get("/api/v1/me/weight-logs") {
            header(HttpHeaders.Authorization, bearer("journal-user"))
        }
        val weightsBody = weights.body<String>()
        assertEquals(HttpStatusCode.OK, weights.status)
        assertTrue(weightsBody.contains("\"total\":1"), weightsBody)
        assertTrue(weightsBody.contains("Morning"), weightsBody)

        val dashboard = client.get("/api/v1/me/nutrition-dashboard?date=$today") {
            header(HttpHeaders.Authorization, bearer("journal-user"))
        }
        val dashboardBody = dashboard.body<String>()
        assertEquals(HttpStatusCode.OK, dashboard.status)
        assertTrue(dashboardBody.contains("\"waterMl\":350"), dashboardBody)
        assertTrue(dashboardBody.contains("\"latestWeightKg\":72.4"), dashboardBody)
    }

    @Test
    fun `photo nutrition job requires explicit consent and exposes status`() = testApplication {
        application { testModule() }

        val rejected = client.post("/api/v1/me/photo-nutrition-jobs") {
            header(HttpHeaders.Authorization, bearer("photo-user"))
            contentType(ContentType.Application.Json)
            setBody("""{"imageUrl":"https://example.com/meal.jpg","consentAccepted":false}""")
        }
        assertEquals(HttpStatusCode.Forbidden, rejected.status)
        assertTrue(rejected.body<String>().contains("FORBIDDEN"))

        val created = client.post("/api/v1/me/photo-nutrition-jobs") {
            header(HttpHeaders.Authorization, bearer("photo-user"))
            contentType(ContentType.Application.Json)
            setBody("""{"imageUrl":"https://example.com/meal.jpg","consentAccepted":true}""")
        }
        val createdBody = created.body<String>()
        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(createdBody.contains("\"status\":\"PENDING\""), createdBody)
        assertTrue(createdBody.contains("\"requiresReview\":true"), createdBody)
        val jobId = extractJsonString(createdBody, "id")

        val fetched = client.get("/api/v1/me/photo-nutrition-jobs/$jobId") {
            header(HttpHeaders.Authorization, bearer("photo-user"))
        }
        val fetchedBody = fetched.body<String>()
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertTrue(fetchedBody.contains("https://example.com/meal.jpg"), fetchedBody)
        assertTrue(fetchedBody.contains("PENDING"), fetchedBody)
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\":\\\"([^\\\"]+)\\\"")
        return regex.find(json)?.groupValues?.get(1) ?: error("Missing '$key' in $json")
    }
}
