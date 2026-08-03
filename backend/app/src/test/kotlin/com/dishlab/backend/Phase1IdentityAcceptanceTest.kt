package com.dishlab.backend

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Phase1IdentityAcceptanceTest {
    private val token = "Bearer :phase1-user"

    @Test
    fun `get me creates and returns minimal authenticated user`() = testApplication {
        application { testModule() }

        val response = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, token)
        }
        val body = response.body<String>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("\"firebaseUid\":\"phase1-user\""), body)
        assertTrue(body.contains("\"profile\":null"), body)
        assertTrue(body.contains("\"allergens\":[]"), body)
        assertTrue(body.contains("\"equipment\":[]"), body)
    }

    @Test
    fun `patch me profile validates and persists profile`() = testApplication {
        application { testModule() }

        val invalid = client.patch("/api/v1/me/profile") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"A","locale":"uk-UA"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue(invalid.body<String>().contains("VALIDATION_ERROR"))

        val valid = client.patch("/api/v1/me/profile") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Олена","locale":"uk-UA","timezone":"Europe/Kyiv"}""")
        }
        assertEquals(HttpStatusCode.OK, valid.status)

        val me = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        assertTrue(me.contains("\"displayName\":\"Олена\""), me)
        assertTrue(me.contains("\"locale\":\"uk-UA\""), me)
    }

    @Test
    fun `put allergens, diet, equipment, goals patch settings and post consents persist on me`() = testApplication {
        application { testModule() }

        assertEquals(
            HttpStatusCode.OK,
            client.put("/api/v1/me/allergens") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"allergens":["gluten","peanut"]}""")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.put("/api/v1/me/diets") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"diets":["vegan","keto"]}""")
            }.status,
        )

        assertEquals(
            HttpStatusCode.OK,
            client.put("/api/v1/me/equipment") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"equipment":["oven","blender"]}""")
            }.status,
        )

        assertEquals(
            HttpStatusCode.OK,
            client.put("/api/v1/me/goals") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"goals":["muscle-gain","lose_weight"]}""")
            }.status,
        )

        assertEquals(
            HttpStatusCode.OK,
            client.patch("/api/v1/me/settings") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"language":"uk-UA","units":"metric","notificationsEnabled":true}""")
            }.status,
        )
        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/v1/me/consents") {
                header(HttpHeaders.Authorization, token)
                contentType(ContentType.Application.Json)
                setBody("""{"type":"AI_PHOTO_ESTIMATE","granted":true}""")
            }.status,
        )

        val me = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, token)
        }.body<String>()

        assertTrue(me.contains("\"allergens\":[\"gluten\",\"peanut\"]"), me)
        assertTrue(me.contains("\"diets\":[\"vegan\",\"keto\"]"), me)
        assertTrue(me.contains("\"equipment\":[\"oven\",\"blender\"]"), me)
        assertTrue(me.contains("\"goals\":[\"muscle-gain\",\"lose_weight\"]"), me)
        assertTrue(me.contains("\"language\":\"uk-UA\""), me)
        assertTrue(me.contains("\"AI_PHOTO_ESTIMATE\""), me)
    }

    @Test
    fun `delete me marks user deleted and anonymizes profile`() = testApplication {
        application { testModule() }

        client.patch("/api/v1/me/profile") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Олена","locale":"uk-UA"}""")
        }

        val deleted = client.delete("/api/v1/me") {
            header(HttpHeaders.Authorization, token)
        }
        assertEquals(HttpStatusCode.OK, deleted.status)

        val me = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, token)
        }.body<String>()
        assertTrue(me.contains("\"deleted\":true"), me)
        assertTrue(me.contains("\"profile\":null"), me)
    }
}
