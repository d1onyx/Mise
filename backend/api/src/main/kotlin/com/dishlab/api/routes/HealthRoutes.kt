package com.dishlab.api.routes

import com.dishlab.api.dto.HealthResponse
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HealthResponse(status = "ok"))
    }

    get("/live") {
        call.respond(HealthResponse(status = "alive"))
    }

    get("/ready") {
        call.respondText(
            contentType = ContentType.Application.Json,
            text = """{"status":"ready","checks":{"application":"ok","database":"in-memory","migrations":"available"},"checkedAt":"${Instant.now()}"}""",
        )
    }

    get("/metrics") {
        call.respondText(
            contentType = ContentType.Text.Plain.withParameter("version", "0.0.4"),
            text = ProductionMetrics.render(),
        )
    }
}

object ProductionMetrics {
    private val requests = linkedMapOf<Pair<String, String>, Long>()

    @Synchronized
    fun record(method: String, path: String) {
        val key = method to sanitizePath(path)
        requests[key] = (requests[key] ?: 0L) + 1L
    }

    @Synchronized
    fun render(): String = buildString {
        appendLine("# HELP dishlab_http_requests_total Total HTTP requests handled by method and sanitized path.")
        appendLine("# TYPE dishlab_http_requests_total counter")
        requests.forEach { (key, count) ->
            appendLine("dishlab_http_requests_total{method=\"${key.first}\",path=\"${key.second}\"} $count")
        }
        appendLine("# HELP dishlab_build_info Build metadata for the DishLab backend.")
        appendLine("# TYPE dishlab_build_info gauge")
        appendLine("dishlab_build_info{service=\"dishlab-backend\",version=\"1.0.0-SNAPSHOT\"} 1")
    }

    private fun sanitizePath(path: String): String = path
        .replace(Regex("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"), "/{id}")
        .replace(Regex("/[0-9]+"), "/{number}")
}
