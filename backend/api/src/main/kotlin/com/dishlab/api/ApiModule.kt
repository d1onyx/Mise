package com.dishlab.api

import com.dishlab.api.middleware.RateLimitDecision
import com.dishlab.api.middleware.RateLimiter
import com.dishlab.api.middleware.RequestIdHeader
import com.dishlab.api.middleware.RequestIdKey
import com.dishlab.api.middleware.applyCorsHeaders
import com.dishlab.api.middleware.clientRateLimitKey
import com.dishlab.api.middleware.installErrorHandling
import com.dishlab.api.middleware.isCorsPreflight
import com.dishlab.api.middleware.newRequestId
import com.dishlab.api.middleware.pathForMetrics
import com.dishlab.api.middleware.respondRateLimited
import com.dishlab.api.routes.ProductionMetrics
import com.dishlab.api.routes.authDebugRoutes
import com.dishlab.api.routes.healthRoutes
import com.dishlab.api.routes.ingredientRoutes
import com.dishlab.api.routes.mediaRoutes
import com.dishlab.api.routes.meRoutes
import com.dishlab.api.routes.openapiRoutes
import com.dishlab.api.routes.recipeRoutes
import com.dishlab.api.routes.recipeCatalogRoutes
import com.dishlab.api.routes.productCatalogRoutes
import com.dishlab.application.service.IdentityService
import com.dishlab.application.service.IngredientService
import com.dishlab.application.service.CurrentUserResolver
import com.dishlab.application.service.RecipeService
import com.dishlab.application.service.RecipeCatalogService
import com.dishlab.application.service.ProductCatalogService
import com.dishlab.application.service.UnitConversionService
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import java.io.File

fun Application.configureApi(
    authVerifier: FirebaseAuthVerifier,
    identityService: IdentityService,
    currentUserResolver: CurrentUserResolver,
    ingredientService: IngredientService,
    unitConversionService: UnitConversionService,
    recipeService: RecipeService,
    recipeCatalogService: RecipeCatalogService?,
    productCatalogService: ProductCatalogService,
    baseUrl: String = System.getenv("BASE_URL") ?: "http://10.0.1.12:8080",
) {
    val appLog = environment.log
    val rateLimiter = RateLimiter()

    intercept(ApplicationCallPipeline.Setup) {
        val requestId = call.request.header(RequestIdHeader)?.takeIf { it.isNotBlank() } ?: newRequestId()
        val method = call.request.httpMethod.value
        val path = call.request.path()
        call.attributes.put(RequestIdKey, requestId)
        call.response.header(RequestIdHeader, requestId)
        call.applyCorsHeaders()
        ProductionMetrics.record(method, call.pathForMetrics())
        appLog.info("requestId={} method={} path={}", requestId, method, path)

        if (call.isCorsPreflight()) {
            call.respond(HttpStatusCode.OK)
            finish()
            return@intercept
        }

        when (val decision = rateLimiter.check(path, call.clientRateLimitKey())) {
            RateLimitDecision.Allowed -> Unit
            is RateLimitDecision.Accepted -> {
                call.response.header("X-RateLimit-Limit", decision.limit.toString())
                call.response.header("X-RateLimit-Remaining", decision.remaining.toString())
            }
            is RateLimitDecision.Rejected -> {
                call.respondRateLimited(decision)
                finish()
            }
        }
    }

    installErrorHandling()

    routing {
        healthRoutes()
        openapiRoutes()
        authDebugRoutes(authVerifier)
        meRoutes(authVerifier, identityService)
        ingredientRoutes(authVerifier, ingredientService, unitConversionService)
        recipeRoutes(authVerifier, currentUserResolver, recipeService)
        recipeCatalogService?.let {
            recipeCatalogRoutes(authVerifier, it, currentUserResolver, recipeService)
        }
        productCatalogRoutes(authVerifier, productCatalogService)
        mediaRoutes(authVerifier, baseUrl)
        staticFiles("/uploads", File("uploads"))
    }
}
