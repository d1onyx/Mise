package com.dishlab.backend

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase0AcceptanceTest {
    @Test
    fun `health returns ok envelope and request id`() = testApplication {
        application { testModule() }

        val response = client.get("/health") {
            header("X-Request-Id", "test-request-1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("test-request-1", response.headers["X-Request-Id"])
        assertEquals("{\"status\":\"ok\"}", response.body())
    }

    @Test
    fun `missing auth token returns structured unauthorized error`() = testApplication {
        application { testModule() }

        val response = client.get("/api/v1/auth/debug")
        val body = response.body<String>()

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNotNull(response.headers["X-Request-Id"])
        assertTrue(body.contains("\"code\":\"UNAUTHORIZED\""), body)
        assertTrue(body.contains("\"trace_id\":"), body)
    }

    @Test
    fun `dev firebase auth stub accepts colon bearer token`() = testApplication {
        application { testModule() }

        val response = client.get("/api/v1/auth/debug") {
            header(HttpHeaders.Authorization, "Bearer :user-123")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"uid\":\"user-123\"}", response.body())
    }

    @Test
    fun `open api base is exposed`() = testApplication {
        application { testModule() }

        val response = client.get("/openapi.json")
        val body = response.body<String>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body.contains("\"title\":\"Smart Cooking Ecosystem API\""), body)
    }
}
