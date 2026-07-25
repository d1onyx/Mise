package com.d1onyx.core.network

import com.d1onyx.core.essentials.exceptions.AuthException
import com.d1onyx.core.essentials.exceptions.BackendException
import com.d1onyx.core.essentials.exceptions.ConnectionException
import com.d1onyx.core.essentials.exceptions.RateLimitException
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.network.auth.AuthTokenProvider
import com.d1onyx.core.network.error.BackendExceptionMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpClientFactoryTest {

    private val sink = RecordingLogSink()

    private val config = NetworkConfig(
        baseUrl = "https://api.example.com/",
        isDebug = true,
    )

    @Serializable
    private data class UserDto(val firstName: String, val lastName: String)

    private fun client(
        tokenProvider: AuthTokenProvider = AuthTokenProvider.None,
        mappers: Set<BackendExceptionMapper> = emptySet(),
        handler: MockRequestHandler,
    ): Pair<HttpClient, MockEngine> {
        val engine = MockEngine(handler)
        val httpClient = createHttpClient(
            config = config,
            logger = DefaultLogger(sink),
            tokenProvider = tokenProvider,
            exceptionMappers = mappers,
            engine = engine,
        )
        return httpClient to engine
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `deserializes a snake_case response into camelCase properties`() = runTest {
        val (httpClient, _) = client {
            respond("""{"first_name":"Ada","last_name":"Lovelace"}""", HttpStatusCode.OK, jsonHeaders)
        }

        val user: UserDto = httpClient.get("users/1").body()

        assertEquals(UserDto("Ada", "Lovelace"), user)
    }

    @Test
    fun `ignores unknown response fields`() = runTest {
        val (httpClient, _) = client {
            respond(
                """{"first_name":"Ada","last_name":"Lovelace","nickname":"unexpected"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val user: UserDto = httpClient.get("users/1").body()

        assertEquals("Ada", user.firstName)
    }

    @Test
    fun `attaches a bearer token when one is available`() = runTest {
        val (httpClient, engine) = client(tokenProvider = { "secret-token" }) {
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        httpClient.get("users/1")

        assertEquals("Bearer secret-token", engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `sends no authorization header when logged out`() = runTest {
        val (httpClient, engine) = client(tokenProvider = { null }) {
            respond("{}", HttpStatusCode.OK, jsonHeaders)
        }

        httpClient.get("users/1")

        assertNull(engine.requestHistory.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `maps 401 to AuthException`() = runTest {
        val (httpClient, _) = client {
            respond("""{"errcode":"M_UNAUTHORIZED"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        assertFailsWith<AuthException> { httpClient.get("users/1").body<UserDto>() }
    }

    @Test
    fun `maps 429 to RateLimitException`() = runTest {
        val (httpClient, _) = client {
            respond("{}", HttpStatusCode.TooManyRequests, jsonHeaders)
        }

        assertFailsWith<RateLimitException> { httpClient.get("users/1").body<UserDto>() }
    }

    @Test
    fun `maps other failures to BackendException carrying the server code`() = runTest {
        val (httpClient, _) = client {
            respond(
                """{"errcode":"M_ROOM_MISSING","error":"room not found"}""",
                HttpStatusCode.NotFound,
                jsonHeaders,
            )
        }

        val exception = assertFailsWith<BackendException> { httpClient.get("rooms/1").body<UserDto>() }

        assertEquals(404, exception.httpCode.value)
        assertEquals("M_ROOM_MISSING", exception.serverCode.value)
        assertEquals("room not found", exception.backendMessage)
    }

    @Test
    fun `keeps the status code when the error body is not valid json`() = runTest {
        val (httpClient, _) = client {
            respond("<html>gateway error</html>", HttpStatusCode.BadGateway, jsonHeaders)
        }

        val exception = assertFailsWith<BackendException> { httpClient.get("rooms/1").body<UserDto>() }

        assertEquals(502, exception.httpCode.value)
        assertEquals("", exception.serverCode.value)
    }

    @Test
    fun `applies a feature exception mapper`() = runTest {
        val mapper = BackendExceptionMapper.forServerCode("M_ROOM_MISSING") { RoomMissingException(it) }
        val (httpClient, _) = client(mappers = setOf(mapper)) {
            respond("""{"errcode":"M_ROOM_MISSING"}""", HttpStatusCode.NotFound, jsonHeaders)
        }

        assertFailsWith<RoomMissingException> { httpClient.get("rooms/1").body<UserDto>() }
    }

    @Test
    fun `leaves an unrelated failure to the default mapping`() = runTest {
        val mapper = BackendExceptionMapper.forServerCode("M_SOMETHING_ELSE") { RoomMissingException(it) }
        val (httpClient, _) = client(mappers = setOf(mapper)) {
            respond("""{"errcode":"M_ROOM_MISSING"}""", HttpStatusCode.NotFound, jsonHeaders)
        }

        assertFailsWith<BackendException> { httpClient.get("rooms/1").body<UserDto>() }
    }

    @Test
    fun `maps a transport failure to ConnectionException`() = runTest {
        val (httpClient, _) = client { throw kotlinx.io.IOException("network is down") }

        assertFailsWith<ConnectionException> { httpClient.get("users/1").body<UserDto>() }
    }

    @Test
    fun `routes ktor request logging into the app logger`() = runTest {
        val (httpClient, _) = client {
            respond("""{"first_name":"Ada","last_name":"Lovelace"}""", HttpStatusCode.OK, jsonHeaders)
        }

        httpClient.get("users/1")

        assertTrue(sink.records.any { it.tag == "Http" }, "HTTP traffic must reach the app logger")
    }

    private class RoomMissingException(cause: Throwable) : Exception("room missing", cause)
}
