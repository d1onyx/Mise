package com.dishlab.api.routes

import com.dishlab.api.dto.DeletePantryItemResponse
import com.dishlab.api.dto.FreezeInventoryRequest
import com.dishlab.api.dto.PantryActionRequest
import com.dishlab.api.dto.PantryItemRequest
import com.dishlab.api.dto.PantryTransactionsResponse
import com.dishlab.api.dto.toInput
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.PantryItemFilter
import com.dishlab.application.service.PantryService
import com.dishlab.domain.error.NotFoundError
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.pantryRoutes(
    authVerifier: FirebaseAuthVerifier,
    pantryService: PantryService,
) {
    route("/api/v1/pantry/inventory") {
        get {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(
                pantryService.list(
                    firebaseUid = user.uid,
                    filter = PantryItemFilter(
                        location = call.request.queryParameters["location"],
                        category = call.request.queryParameters["category"],
                        expiry = call.request.queryParameters["expiry"],
                        search = call.request.queryParameters["search"],
                        includeDeleted = call.request.queryParameters["includeDeleted"]?.toBooleanStrictOrNull() ?: false,
                    ),
                ).toResponse(),
            )
        }

        post {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<PantryItemRequest>()
            call.respond(HttpStatusCode.Created, pantryService.add(user.uid, request.toInput()).toResponse())
        }

        get("/expiring") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(pantryService.expiring(user.uid).toResponse())
        }

        get("/transactions") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val transactions = pantryService.transactions(user.uid)
            call.respond(PantryTransactionsResponse(transactions.map { it.toResponse() }, transactions.size))
        }

        patch("/{itemId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<PantryItemRequest>()
            call.respond(pantryService.update(user.uid, call.itemId(), request.toInput()).toResponse())
        }

        delete("/{itemId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            val item = pantryService.tombstone(user.uid, call.itemId())
            call.respond(DeletePantryItemResponse(item.id.toString(), item.deleted))
        }

        post("/{itemId}/consume") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<PantryActionRequest>()
            call.respond(
                pantryService.consume(
                    firebaseUid = user.uid,
                    itemId = call.itemId(),
                    quantity = request.quantity,
                    reason = request.reason,
                    recipeId = request.recipeId?.let { UUID.fromString(it) },
                ).toResponse(),
            )
        }

        post("/{itemId}/waste") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<PantryActionRequest>()
            call.respond(pantryService.waste(user.uid, call.itemId(), request.quantity, request.reason).toResponse())
        }

        post("/{itemId}/freeze") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<FreezeInventoryRequest>()
            call.respond(pantryService.freeze(user.uid, call.itemId(), request.extraDays).toResponse())
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.itemId(): UUID =
    parameters["itemId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Позицію інвентарю не знайдено")
