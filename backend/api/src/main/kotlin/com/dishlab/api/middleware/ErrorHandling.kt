package com.dishlab.api.middleware

import com.dishlab.api.dto.ApiError
import com.dishlab.api.dto.ErrorResponse
import com.dishlab.domain.error.AppError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.application.install

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<AppError> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.httpStatusCode),
                ErrorResponse(
                    error = ApiError(
                        code = cause.code,
                        message = cause.message,
                        details = cause.details,
                        traceId = call.requestId(),
                    ),
                ),
            )
        }

        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ApiError(
                        code = "INTERNAL_ERROR",
                        message = cause.message ?: "Unknown error",
                        traceId = call.requestId(),
                    ),
                ),
            )
        }
    }
}
