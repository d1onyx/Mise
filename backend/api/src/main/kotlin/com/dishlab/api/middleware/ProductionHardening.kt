package com.dishlab.api.middleware

import com.dishlab.api.dto.ApiError
import com.dishlab.api.dto.ErrorResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import java.time.Clock
import java.time.Duration
import java.time.Instant

class RateLimiter(
    private val clock: Clock = Clock.systemUTC(),
) {
    private data class Bucket(var windowStartedAt: Instant, var count: Int)
    private data class Rule(
        val id: String,
        val matches: (String) -> Boolean,
        val limit: Int,
        val window: Duration,
    )

    private val rules = listOf(
        Rule(
            id = "auth",
            matches = { path -> path.startsWith("/api/v1/auth/") },
            limit = 20,
            window = Duration.ofMinutes(1),
        ),
        Rule(
            id = "public-catalog",
            matches = { path ->
                path == "/api/v1/products/resolve" ||
                    path == "/api/v1/recipe-catalog/pantry-match" ||
                    path.matches(Regex("/api/v1/recipe-catalog/[^/]+"))
            },
            limit = 20,
            window = Duration.ofMinutes(1),
        ),
    )
    private val buckets = mutableMapOf<String, Bucket>()

    @Synchronized
    fun check(path: String, key: String): RateLimitDecision {
        val rule = rules.firstOrNull { it.matches(path) } ?: return RateLimitDecision.Allowed
        val now = clock.instant()
        val bucketKey = "${rule.id}:$key"
        val bucket = buckets.getOrPut(bucketKey) { Bucket(windowStartedAt = now, count = 0) }
        if (Duration.between(bucket.windowStartedAt, now) >= rule.window) {
            bucket.windowStartedAt = now
            bucket.count = 0
        }
        if (bucket.count >= rule.limit) {
            val retryAfter = (rule.window.seconds - Duration.between(bucket.windowStartedAt, now).seconds).coerceAtLeast(1)
            return RateLimitDecision.Rejected(rule.limit, 0, retryAfter)
        }
        bucket.count += 1
        return RateLimitDecision.Accepted(rule.limit, (rule.limit - bucket.count).coerceAtLeast(0))
    }
}

sealed interface RateLimitDecision {
    data object Allowed : RateLimitDecision
    data class Accepted(val limit: Int, val remaining: Int) : RateLimitDecision
    data class Rejected(val limit: Int, val remaining: Int, val retryAfterSeconds: Long) : RateLimitDecision
}

fun ApplicationCall.clientRateLimitKey(): String =
    request.header("X-Forwarded-For")?.substringBefore(',')?.trim()?.takeIf { it.isNotBlank() }
        ?: request.origin.remoteHost

suspend fun ApplicationCall.respondRateLimited(decision: RateLimitDecision.Rejected) {
    response.header("X-RateLimit-Limit", decision.limit.toString())
    response.header("X-RateLimit-Remaining", decision.remaining.toString())
    response.header(HttpHeaders.RetryAfter, decision.retryAfterSeconds.toString())
    respond(
        HttpStatusCode.TooManyRequests,
        ErrorResponse(
            error = ApiError(
                code = "RATE_LIMIT_EXCEEDED",
                message = "Забагато запитів. Спробуйте пізніше",
                details = mapOf(
                    "limit" to decision.limit.toString(),
                    "retryAfterSeconds" to decision.retryAfterSeconds.toString(),
                ),
                traceId = requestId(),
            ),
        ),
    )
}

fun ApplicationCall.isCorsPreflight(): Boolean =
    request.httpMethod == HttpMethod.Options && request.header(HttpHeaders.AccessControlRequestMethod) != null

private val allowedOrigins: Set<String> by lazy {
    (System.getenv("CORS_ALLOWED_ORIGINS") ?: "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()
}

fun ApplicationCall.applyCorsHeaders() {
    val origin = request.header(HttpHeaders.Origin) ?: return
    val matched = allowedOrigins.firstOrNull { pattern ->
        if (pattern.endsWith("*")) origin.startsWith(pattern.dropLast(1))
        else origin == pattern
    } ?: return
    response.header(HttpHeaders.AccessControlAllowOrigin, matched)
    response.header(HttpHeaders.Vary, "Origin")
    response.header(HttpHeaders.AccessControlAllowCredentials, "true")
    response.header(HttpHeaders.AccessControlAllowMethods, "GET,POST,PUT,PATCH,DELETE,OPTIONS")
    response.header(HttpHeaders.AccessControlAllowHeaders, "Authorization,Content-Type,X-Request-Id,Idempotency-Key")
    response.header(HttpHeaders.AccessControlMaxAge, "3600")
}

fun ApplicationCall.pathForMetrics(): String = request.path()
