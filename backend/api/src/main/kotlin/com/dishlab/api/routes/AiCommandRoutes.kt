package com.dishlab.api.routes

import com.dishlab.api.dto.InterpretCommandRequest
import com.dishlab.api.dto.toResponse
import com.dishlab.api.dto.InterpretCommandResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.AiCommandService
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.aiCommandRoutes(
    authVerifier: FirebaseAuthVerifier,
    aiCommandService: AiCommandService,
) {
    route("/api/v1/ai-commands") {
        post("/interpret") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<InterpretCommandRequest>()
            call.respond<List<InterpretCommandResponse>>(aiCommandService.interpret(request.text, request.context).toResponse())
        }
    }
}
