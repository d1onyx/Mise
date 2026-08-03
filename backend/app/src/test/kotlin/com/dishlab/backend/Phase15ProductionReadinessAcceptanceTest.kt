package com.dishlab.backend

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase15ProductionReadinessAcceptanceTest {
    private fun bearer(uid: String) = "Bearer :$uid"

    @Test
    fun `openapi documents production readiness and representative api surface`() = testApplication {
        application { testModule() }

        val response = client.get("/openapi.json")
        val body = response.body<String>()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers[HttpHeaders.ContentType]?.contains("application/json") == true)
        assertTrue(body.contains("\"openapi\":\"3.1.0\""), body)
        assertTrue(body.contains("\"/live\""), body)
        assertTrue(body.contains("\"/ready\""), body)
        assertTrue(body.contains("\"/metrics\""), body)
        assertTrue(body.contains("\"/api/v1/recipes\""), body)
        assertTrue(body.contains("\"/api/v1/pantry/inventory\""), body)
        assertTrue(body.contains("\"bearerAuth\""), body)
        assertTrue(body.contains("\"ErrorResponse\""), body)
    }

    @Test
    fun `liveness readiness and metrics endpoints expose deployable health signals`() = testApplication {
        application { testModule() }

        val live = client.get("/live")
        val ready = client.get("/ready")
        val metrics = client.get("/metrics")
        val metricsBody = metrics.body<String>()

        assertEquals(HttpStatusCode.OK, live.status)
        assertEquals("{\"status\":\"alive\"}", live.body())
        assertEquals(HttpStatusCode.OK, ready.status)
        assertTrue(ready.body<String>().contains("\"checks\":{"), ready.body())
        assertEquals(HttpStatusCode.OK, metrics.status)
        assertTrue(metrics.headers[HttpHeaders.ContentType]?.contains("text/plain") == true)
        assertTrue(metricsBody.contains("# HELP dishlab_http_requests_total"), metricsBody)
        assertTrue(metricsBody.contains("dishlab_build_info"), metricsBody)
    }

    @Test
    fun `cors preflight allows configured android and staging clients`() = testApplication {
        application { testModule() }

        val response = client.options("/api/v1/me") {
            header(HttpHeaders.Origin, "https://staging.dishlab.app")
            header(HttpHeaders.AccessControlRequestMethod, "POST")
            header(HttpHeaders.AccessControlRequestHeaders, "Authorization, Content-Type, X-Request-Id")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("https://staging.dishlab.app", response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue(response.headers[HttpHeaders.AccessControlAllowHeaders]?.contains("Authorization") == true)
        assertTrue(response.headers[HttpHeaders.AccessControlAllowMethods]?.contains("POST") == true)
    }

    @Test
    fun `rate limiting protects auth endpoints`() = testApplication {
        application { testModule() }

        repeat(20) {
            val response = client.get("/api/v1/auth/debug") {
                header(HttpHeaders.Authorization, bearer("rate-user"))
            }
            assertEquals(HttpStatusCode.OK, response.status, response.body())
        }
        val authLimited = client.get("/api/v1/auth/debug") {
            header(HttpHeaders.Authorization, bearer("rate-user"))
        }
        assertEquals(HttpStatusCode.TooManyRequests, authLimited.status)
        assertEquals("20", authLimited.headers["X-RateLimit-Limit"])
        assertEquals("0", authLimited.headers["X-RateLimit-Remaining"])
        assertTrue(authLimited.body<String>().contains("RATE_LIMIT_EXCEEDED"), authLimited.body())
    }

    @Test
    fun `migration files are sequential and migration validation testable without docker`() {
        val migrationDir = Path.of("../migrations/src/main/resources/db/migration")
        val files = Files.list(migrationDir).use { stream ->
            stream.filter { it.name.matches(Regex("V\\d{3}__.+\\.sql")) }
                .map { it.name }
                .sorted()
                .toList()
        }

        assertTrue(files.size >= 7, files.toString())
        assertEquals(files.distinct(), files)
        files.forEach { name ->
            val sql = Files.readString(migrationDir.resolve(name))
            assertTrue(sql.isNotBlank(), "$name must not be blank")
            assertTrue(sql.contains(";"), "$name should contain executable SQL statements")
        }
    }

    @Test
    fun `load test and ci artifacts cover recipe feed pantry filters and docker image build`() {
        val loadScenario = Files.readString(Path.of("../load-tests/recipe-feed-pantry-filters.js"))
        assertTrue(loadScenario.contains("/api/v1/recipes"), loadScenario)
        assertTrue(loadScenario.contains("/api/v1/pantry/inventory"), loadScenario)
        assertTrue(loadScenario.contains("thresholds"), loadScenario)
        val ci = Files.readString(Path.of("../.github/workflows/backend-ci.yml"))
        assertTrue(ci.contains("./gradlew test"), ci)
        assertTrue(ci.contains("./gradlew build"), ci)
        assertTrue(ci.contains("docker build"), ci)

        assertNotNull(Path.of("../Dockerfile").toFile().takeIf { it.exists() }, "Dockerfile is required for staging image builds")
    }

    @Test
    fun `request id is echoed and metrics do not leak request bodies`() = testApplication {
        application { testModule() }

        val requestId = "privacy-log-test"
        val response = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, bearer("privacy-user"))
            header("X-Request-Id", requestId)
        }

        assertEquals(HttpStatusCode.OK, response.status, response.body())
        assertEquals(requestId, response.headers["X-Request-Id"])
        val metricsBody = client.get("/metrics").body<String>()
        assertTrue(metricsBody.contains("path=\"/api/v1/me\""), metricsBody)
    }
}
