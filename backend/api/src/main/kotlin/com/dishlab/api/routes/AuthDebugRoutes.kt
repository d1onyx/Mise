package com.dishlab.api.routes

import com.dishlab.api.dto.AuthDebugResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.authDebugRoutes(authVerifier: FirebaseAuthVerifier) {
    route("/api/v1") {
        get("/auth/debug") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(AuthDebugResponse(uid = user.uid))
        }
    }
}
