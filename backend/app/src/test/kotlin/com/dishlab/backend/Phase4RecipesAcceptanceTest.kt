package com.dishlab.backend

import io.ktor.client.call.body
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

class Phase4RecipesAcceptanceTest {
    private val token = "Bearer :phase4-user"

    @Test
    fun `recipe draft creation validates fields and publish requires ingredient and step`() = testApplication {
        application { testModule() }

        val invalid = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"A","servings":0,"ingredients":[],"steps":[]}""")
        }
        val invalidBody = invalid.body<String>()
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue(invalidBody.contains("VALIDATION_ERROR"), invalidBody)
        assertTrue(invalidBody.contains("title"), invalidBody)
        assertTrue(invalidBody.contains("servings"), invalidBody)

        val draft = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "title":"Томатний суп",
                  "description":"Легка домашня страва",
                  "servings":4,
                  "visibility":"PRIVATE",
                  "tags":["soup","vegan"],
                  "equipment":["pot"]
                }
                """.trimIndent(),
            )
        }
        val draftBody = draft.body<String>()
        assertEquals(HttpStatusCode.Created, draft.status)
        assertTrue(draftBody.contains("\"status\":\"DRAFT\""), draftBody)
        assertTrue(draftBody.contains("\"versionNumber\":1"), draftBody)
        val recipeId = extractJsonString(draftBody, "id")

        val publishWithoutContent = client.post("/api/v1/recipes/$recipeId/publish") {
            header(HttpHeaders.Authorization, token)
        }
        val publishBody = publishWithoutContent.body<String>()
        assertEquals(HttpStatusCode.BadRequest, publishWithoutContent.status)
        assertTrue(publishBody.contains("PUBLISH_VALIDATION_ERROR"), publishBody)
    }

    @Test
    fun `recipe draft can be updated published and returned with tabs data`() = testApplication {
        application { testModule() }

        val recipeId = createDraftRecipe(title = "Паста з томатами")

        val updated = client.patch("/api/v1/recipes/$recipeId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "title":"Паста з томатами та базиліком",
                  "servings":2,
                  "ingredients":[{"ingredientId":"11111111-1111-1111-1111-111111111111","name":"Tomato","amount":250.0,"unit":"g","note":"fresh"}],
                  "steps":[{"position":1,"text":"Наріжте томати."},{"position":2,"text":"Змішайте з пастою."}],
                  "tags":["dinner","pasta"],
                  "equipment":["pot","knife"]
                }
                """.trimIndent(),
            )
        }
        val updatedBody = updated.body<String>()
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updatedBody.contains("Паста з томатами та базиліком"), updatedBody)
        assertTrue(updatedBody.contains("\"ingredients\""), updatedBody)

        val published = client.post("/api/v1/recipes/$recipeId/publish") {
            header(HttpHeaders.Authorization, token)
        }
        val publishedBody = published.body<String>()
        assertEquals(HttpStatusCode.OK, published.status)
        assertTrue(publishedBody.contains("\"status\":\"PUBLISHED\""), publishedBody)
        assertTrue(publishedBody.contains("\"publishedAt\":"), publishedBody)

        val details = client.get("/api/v1/recipes/$recipeId") {
            header(HttpHeaders.Authorization, token)
        }
        val detailsBody = details.body<String>()
        assertEquals(HttpStatusCode.OK, details.status)
        assertTrue(detailsBody.contains("\"tabs\""), detailsBody)
        assertTrue(detailsBody.contains("\"overview\""), detailsBody)
        assertTrue(detailsBody.contains("\"ingredients\""), detailsBody)
        assertTrue(detailsBody.contains("\"steps\""), detailsBody)
        assertTrue(detailsBody.contains("\"versions\""), detailsBody)
    }

    @Test
    fun `recipe version creation compare and feed filters work`() = testApplication {
        application { testModule() }

        val recipeId = createPublishableRecipe(title = "Швидкий салат", tag = "salad")

        val version = client.post("/api/v1/recipes/$recipeId/versions") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "changelog":"Додано лимонний сік",
                  "title":"Швидкий салат з лимоном",
                  "servings":3,
                  "ingredients":[{"ingredientId":"11111111-1111-1111-1111-111111111111","name":"Tomato","amount":300.0,"unit":"g"}],
                  "steps":[{"position":1,"text":"Наріжте овочі."},{"position":2,"text":"Додайте лимонний сік."}],
                  "tags":["salad","vegan"],
                  "equipment":["knife"]
                }
                """.trimIndent(),
            )
        }
        val versionBody = version.body<String>()
        assertEquals(HttpStatusCode.Created, version.status)
        assertTrue(versionBody.contains("\"versionNumber\":2"), versionBody)
        assertTrue(versionBody.contains("Додано лимонний сік"), versionBody)
        val versionId = extractJsonString(versionBody, "id")

        val compare = client.post("/api/v1/recipes/$recipeId/versions/$versionId/compare") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"targetVersionNumber":1}""")
        }
        val compareBody = compare.body<String>()
        assertEquals(HttpStatusCode.OK, compare.status)
        assertTrue(compareBody.contains("\"fromVersionNumber\":2"), compareBody)
        assertTrue(compareBody.contains("\"toVersionNumber\":1"), compareBody)
        assertTrue(compareBody.contains("title"), compareBody)

        val feed = client.get("/api/v1/recipes?tag=vegan&sort=newest&page=1&pageSize=10") {
            header(HttpHeaders.Authorization, token)
        }
        val feedBody = feed.body<String>()
        assertEquals(HttpStatusCode.OK, feed.status)
        assertTrue(feedBody.contains("Швидкий салат з лимоном"), feedBody)
        assertTrue(feedBody.contains("\"page\":1"), feedBody)
    }

    @Test
    fun `fork reviews reports and media upload policy are supported`() = testApplication {
        application { testModule() }

        val recipeId = createPublishableRecipe(title = "Запечені овочі", tag = "vegetables")

        val fork = client.post("/api/v1/recipes/$recipeId/fork") {
            header(HttpHeaders.Authorization, "Bearer :phase4-fork-user")
        }
        val forkBody = fork.body<String>()
        assertEquals(HttpStatusCode.Created, fork.status)
        assertTrue(forkBody.contains("\"forkedFromRecipeId\":\"$recipeId\""), forkBody)
        assertTrue(forkBody.contains("\"status\":\"DRAFT\""), forkBody)

        val review = client.post("/api/v1/recipes/$recipeId/reviews") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"rating":5,"comment":"Дуже смачно"}""")
        }
        assertEquals(HttpStatusCode.Created, review.status)
        assertTrue(review.body<String>().contains("Дуже смачно"))

        val reviews = client.get("/api/v1/recipes/$recipeId/reviews") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, reviews.status)
        assertTrue(reviews.body<String>().contains("\"averageRating\":5.0"))

        val report = client.post("/api/v1/recipes/$recipeId/reports") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"spam","comment":"Не схоже на рецепт"}""")
        }
        assertEquals(HttpStatusCode.Created, report.status)
        assertTrue(report.body<String>().contains("\"status\":\"OPEN\""))

        val upload = client.post("/api/v1/recipes/$recipeId/media/upload-url") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"mediaType":"PHOTO","fileName":"cover.jpg","contentType":"image/jpeg","fileSizeBytes":10485760}""")
        }
        val uploadBody = upload.body<String>()
        assertEquals(HttpStatusCode.OK, upload.status)
        assertTrue(uploadBody.contains("\"maxFileSizeBytes\":10485760"), uploadBody)
        assertTrue(uploadBody.contains("\"uploadUrl\":"), uploadBody)

        val tooLarge = client.post("/api/v1/recipes/$recipeId/media/upload-url") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"mediaType":"VIDEO","fileName":"huge.mp4","contentType":"video/mp4","fileSizeBytes":52428801}""")
        }
        assertEquals(HttpStatusCode.BadRequest, tooLarge.status)
        assertTrue(tooLarge.body<String>().contains("MEDIA_FILE_TOO_LARGE"))
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createDraftRecipe(title: String): String {
        val response = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"$title","servings":4,"visibility":"PRIVATE","tags":["draft"],"equipment":["pot"]}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return extractJsonString(response.body(), "id")
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createPublishableRecipe(title: String, tag: String): String {
        val recipeId = createDraftRecipe(title)
        val updated = client.patch("/api/v1/recipes/$recipeId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "ingredients":[{"ingredientId":"11111111-1111-1111-1111-111111111111","name":"Tomato","amount":250.0,"unit":"g"}],
                  "steps":[{"position":1,"text":"Підготуйте інгредієнти."}],
                  "tags":["$tag","vegan"],
                  "equipment":["knife"]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        val published = client.post("/api/v1/recipes/$recipeId/publish") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, published.status)
        return recipeId
    }

    @Test
    fun `repeated create with same Idempotency-Key returns the same recipe and never duplicates`() = testApplication {
        application { testModule() }

        val body = """{"title":"Offline Borscht","servings":4}"""
        val key = "publish-key-001"

        val first = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val second = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            header("Idempotency-Key", key)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        assertEquals(HttpStatusCode.Created, first.status)
        assertEquals(HttpStatusCode.Created, second.status)
        val firstId = extractJsonString(first.body(), "id")
        val secondId = extractJsonString(second.body(), "id")
        assertEquals(firstId, secondId, "Same Idempotency-Key must return the same recipe id")

        // A different key must create a distinct recipe.
        val other = client.post("/api/v1/recipes") {
            header(HttpHeaders.Authorization, token)
            header("Idempotency-Key", "publish-key-002")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, other.status)
        assertTrue(extractJsonString(other.body(), "id") != firstId, "A new key must create a new recipe")
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\":\\\"([^\\\"]+)\\\"")
        return regex.find(json)?.groupValues?.get(1) ?: error("Missing '$key' in $json")
    }
}
