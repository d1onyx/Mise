package com.dishlab.api.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.call
import io.ktor.server.http.content.resolveResource
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource

private const val SWAGGER_UI_VERSION = "5.32.13"

/**
 * Spec is derived from the live routing tree (schema-inferred via the Ktor
 * OpenAPI compiler plugin), not hand-maintained JSON — served at
 * GET /swagger/openapi.json, with the interactive UI at GET /swagger.
 */
fun Route.openapiRoutes() {
    // swaggerUI's default `packageLocation` points the rendered HTML at
    // https://unpkg.com/swagger-ui-dist, so the page is blank on a VPS behind a
    // firewall or offline. Serving the org.webjars:swagger-ui jar's bundled
    // assets locally and pointing packageLocation there instead means /swagger
    // never leaves the box. /swagger/openapi.json is unaffected either way — it
    // was never CDN-dependent.
    // The plugin always renders asset URLs as "$packageLocation@$version/...", CDN-style, even
    // for a local path — so the static mount has to include the literal "@$SWAGGER_UI_VERSION"
    // segment too, matching what the rendered page actually requests.
    swaggerUI(path = "swagger") {
        info = OpenApiInfo(title = "DishLab Backend API", version = "1.0.0-SNAPSHOT")
        remotePath = "openapi.json"
        source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
        packageLocation = "/swagger-ui-dist"
        this.version = SWAGGER_UI_VERSION
    }
    // resolveResource splits its `resourcePackage` argument on dots too (it treats it as Java
    // package notation), so a version number containing dots ("5.32.13") CANNOT live in that
    // argument — it silently gets exploded into "5/32/13" and every lookup 404s. The version has
    // to be part of `path` instead, which is only split on '/' and '\\'.
    get("/swagger-ui-dist@$SWAGGER_UI_VERSION/{fileName...}") {
        val fileName = call.parameters.getAll("fileName")?.joinToString("/")
        val content = fileName?.let {
            call.resolveResource("$SWAGGER_UI_VERSION/$it", "META-INF/resources/webjars/swagger-ui")
        }
        if (content != null) call.respond(content) else call.respond(HttpStatusCode.NotFound)
    }
}
