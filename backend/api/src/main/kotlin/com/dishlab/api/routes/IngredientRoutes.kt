package com.dishlab.api.routes

import com.dishlab.api.dto.CreateCustomIngredientRequest
import com.dishlab.api.dto.SubstitutesResponse
import com.dishlab.api.dto.UnitConvertRequest
import com.dishlab.api.dto.toDomain
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.IngredientService
import com.dishlab.application.service.UnitConversionService
import com.dishlab.domain.error.NotFoundError
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.ingredientRoutes(
    authVerifier: FirebaseAuthVerifier,
    ingredientService: IngredientService,
    unitConversionService: UnitConversionService,
) {
    route("/api/v1") {
        route("/ingredients") {
            get("/search") {
                val user = call.requireFirebaseUser(authVerifier) ?: return@get
                val query = call.request.queryParameters["q"].orEmpty()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                call.respond(ingredientService.search(user.uid, query, page, pageSize).toResponse())
            }

            post("/custom") {
                val user = call.requireFirebaseUser(authVerifier) ?: return@post
                val request = call.receive<CreateCustomIngredientRequest>()
                call.respond(
                    HttpStatusCode.Created,
                    ingredientService.createCustom(
                        firebaseUid = user.uid,
                        name = request.name,
                        defaultUnit = request.defaultUnit,
                        allergens = request.allergens,
                        nutrition = request.nutritionPer100g?.toDomain(),
                    ).toResponse(),
                )
            }

            get("/{ingredientId}") {
                val user = call.requireFirebaseUser(authVerifier) ?: return@get
                call.respond(ingredientService.get(user.uid, call.ingredientId()).toResponse())
            }

            get("/{ingredientId}/substitutes") {
                val user = call.requireFirebaseUser(authVerifier) ?: return@get
                val substitutes = ingredientService.substitutes(user.uid, call.ingredientId()).map { it.toResponse() }
                call.respond(SubstitutesResponse(substitutes))
            }
        }

        route("/tools") {
            post("/unit-convert") {
                val user = call.requireFirebaseUser(authVerifier) ?: return@post
                val request = call.receive<UnitConvertRequest>()
                ingredientService.search(user.uid, "", 1, 1)
                call.respond(
                    unitConversionService.convert(
                        amount = request.amount,
                        fromUnit = request.fromUnit,
                        toUnit = request.toUnit,
                        densityGramPerMl = request.densityGramPerMl,
                    ).toResponse(),
                )
            }

            get("/temperature-guide") {
                call.respond(mapOf("status" to "planned"))
            }

            get("/equipment-settings") {
                call.respond(mapOf("status" to "planned"))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.ingredientId(): UUID =
    parameters["ingredientId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Інгредієнт не знайдено")
