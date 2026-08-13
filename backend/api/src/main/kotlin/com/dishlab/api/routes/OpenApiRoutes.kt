package com.dishlab.api.routes

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.OpenApiDocSource

/**
 * Spec is derived from the live routing tree (schema-inferred via the Ktor
 * OpenAPI compiler plugin), not hand-maintained JSON — served at
 * GET /swagger/openapi.json, with the interactive UI at GET /swagger.
 */
fun Route.openapiRoutes() {
    swaggerUI(path = "swagger") {
        info = OpenApiInfo(title = "DishLab Backend API", version = "1.0.0-SNAPSHOT")
        remotePath = "openapi.json"
        source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
    }
}
