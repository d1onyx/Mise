package com.dishlab.api.routes

import com.dishlab.api.dto.CompleteCookingSessionRequest
import com.dishlab.api.dto.CreateTimerRequest
import com.dishlab.api.dto.DeleteTimerResponse
import com.dishlab.api.dto.StartCookingSessionRequest
import com.dishlab.api.dto.UpdateCookingSessionRequest
import com.dishlab.api.dto.UpdateTimerRequest
import com.dishlab.api.dto.toInput
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.CookingService
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

fun Route.cookingRoutes(
    authVerifier: FirebaseAuthVerifier,
    cookingService: CookingService,
) {
    route("/api/v1/cooking-sessions") {
        post {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<StartCookingSessionRequest>()
            call.respond(HttpStatusCode.Created, cookingService.start(user.uid, request.toInput()).toResponse())
        }

        get("/{sessionId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(cookingService.details(user.uid, call.sessionId()).toResponse())
        }

        patch("/{sessionId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<UpdateCookingSessionRequest>()
            call.respond(cookingService.update(user.uid, call.sessionId(), request.toInput()).toResponse())
        }

        post("/{sessionId}/complete") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CompleteCookingSessionRequest>()
            call.respond(cookingService.complete(user.uid, call.sessionId(), request.toInput()).toResponse())
        }
    }

    route("/api/v1/timers") {
        post {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CreateTimerRequest>()
            call.respond(HttpStatusCode.Created, cookingService.createTimer(user.uid, request.toInput()).toResponse())
        }

        patch("/{timerId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<UpdateTimerRequest>()
            call.respond(cookingService.updateTimer(user.uid, call.timerId(), request.toInput()).toResponse())
        }

        delete("/{timerId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            val timer = cookingService.deleteTimer(user.uid, call.timerId())
            call.respond(DeleteTimerResponse(timer.id.toString(), deleted = true))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.sessionId(): UUID =
    parameters["sessionId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Кулінарну сесію не знайдено")

private fun io.ktor.server.application.ApplicationCall.timerId(): UUID =
    parameters["timerId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Таймер не знайдено")
