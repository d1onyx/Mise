package com.dishlab.api.middleware

import com.dishlab.api.dto.ApiError
import com.dishlab.api.dto.ErrorResponse
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import com.dishlab.infrastructure.firebase.VerifiedFirebaseUser
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond

suspend fun ApplicationCall.requireFirebaseUser(
    authVerifier: FirebaseAuthVerifier,
): VerifiedFirebaseUser? {

    val authHeader = request.header(HttpHeaders.Authorization) ?: ""

    val bearerToken = if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
        authHeader.substring(7).trim()
    } else {
        null
    }

    val user = bearerToken?.takeIf { it.isNotBlank() }?.let { authVerifier.verify(it) }
    if (user != null) return user

    respond(
        HttpStatusCode.Unauthorized,
        ErrorResponse(
            ApiError(
                code = "UNAUTHORIZED",
                message = "Потрібна авторизація",
                traceId = requestId(),
            ),
        ),
    )
    return null
}
