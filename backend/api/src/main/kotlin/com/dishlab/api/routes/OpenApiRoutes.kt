package com.dishlab.api.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.openapiRoutes() {
    get("/openapi.json") {
        call.respondText(
            contentType = ContentType.Application.Json,
            text = OPENAPI_SPEC,
        )
    }
}

private val OPENAPI_SPEC =
    """
    {
      "openapi": "3.1.0",
      "info": {
        "title": "Smart Cooking Ecosystem API",
        "version": "1.0.0-SNAPSHOT",
        "description": "DishLab Ktor backend contract for staging readiness."
      },
      "servers": [
        { "url": "http://localhost:8080", "description": "local" },
        { "url": "https://staging.dishlab.app", "description": "staging" }
      ],
      "security": [{ "bearerAuth": [] }],
      "paths": {
        "/health": { "get": { "summary": "Compatibility health check" } },
        "/live": { "get": { "summary": "Kubernetes liveness probe" } },
        "/ready": { "get": { "summary": "Kubernetes readiness probe" } },
        "/metrics": { "get": { "summary": "Prometheus metrics" } },
        "/api/v1/auth/debug": { "get": { "summary": "Validate bearer token" } },
        "/api/v1/me/profile": { "get": { "summary": "Current user profile" } },
        "/api/v1/ingredients": { "get": { "summary": "Ingredient catalog" } },
        "/api/v1/recipes": { "get": { "summary": "Recipe feed and filters" }, "post": { "summary": "Create recipe draft" } },
        "/api/v1/recipes/{recipeId}": { "get": { "summary": "Recipe details" }, "patch": { "summary": "Update recipe" } },
        "/api/v1/recipe-catalog": { "get": { "summary": "Search the imported recipe catalog" } },
        "/api/v1/recipe-catalog/{recipeId}": { "get": { "summary": "Imported recipe details" } },
        "/api/v1/products/barcode/{barcode}": { "get": { "summary": "Resolve a barcode through the server" } },
        "/api/v1/products/search": { "get": { "summary": "Search products through the server" } },
        "/api/v1/products/normalize": { "post": { "summary": "Normalize scanned products to recipe ingredient tags" } },
        "/api/v1/pantry/inventory": { "get": { "summary": "Pantry filters" }, "post": { "summary": "Create pantry item" } },
        "/api/v1/cooking/sessions": { "post": { "summary": "Start cooking session" } },
        "/api/v1/health/nutrition-logs": { "get": { "summary": "Nutrition logs" }, "post": { "summary": "Create nutrition log" } }
      },
      "components": {
        "securitySchemes": {
          "bearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "Firebase ID token or dev token"
          }
        },
        "schemas": {
          "ErrorResponse": {
            "type": "object",
            "required": ["error"],
            "properties": {
              "error": {
                "type": "object",
                "required": ["code", "message", "traceId"],
                "properties": {
                  "code": { "type": "string" },
                  "message": { "type": "string" },
                  "details": { "type": "object", "additionalProperties": { "type": "string" } },
                  "traceId": { "type": "string" }
                }
              }
            }
          }
        }
      }
    }
    """.trimIndent().replace(Regex("\\s*([{}\\[\\]:,])\\s*"), "$1")
