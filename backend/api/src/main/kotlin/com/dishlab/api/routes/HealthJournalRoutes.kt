package com.dishlab.api.routes

import com.dishlab.api.dto.DeleteNutritionLogResponse
import com.dishlab.api.dto.NutritionLogRequest
import com.dishlab.api.dto.PhotoNutritionJobRequest
import com.dishlab.api.dto.WaterLogRequest
import com.dishlab.api.dto.WeightLogRequest
import com.dishlab.api.dto.toInput
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.HealthService
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
import java.time.LocalDate
import java.util.UUID

fun Route.healthJournalRoutes(
    authVerifier: FirebaseAuthVerifier,
    healthService: HealthService,
) {
    route("/api/v1/me") {
        get("/nutrition-dashboard") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val date = call.request.queryParameters["date"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
            call.respond(healthService.dashboard(user.uid, date).toResponse())
        }

        get("/nutrition-logs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(
                healthService.listNutritionLogs(
                    firebaseUid = user.uid,
                    from = call.request.queryParameters["from"]?.let { LocalDate.parse(it) },
                    to = call.request.queryParameters["to"]?.let { LocalDate.parse(it) },
                ).toResponse(),
            )
        }

        post("/nutrition-logs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<NutritionLogRequest>()
            call.respond(HttpStatusCode.Created, healthService.createNutritionLog(user.uid, request.toInput()).toResponse())
        }

        patch("/nutrition-logs/{logId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<NutritionLogRequest>()
            call.respond(healthService.updateNutritionLog(user.uid, call.logId(), request.toInput()).toResponse())
        }

        delete("/nutrition-logs/{logId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            val deleted = healthService.deleteNutritionLog(user.uid, call.logId())
            call.respond(DeleteNutritionLogResponse(deleted.id.toString(), deleted.deleted))
        }

        post("/water-logs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<WaterLogRequest>()
            call.respond(HttpStatusCode.Created, healthService.addWater(user.uid, request.toInput()).toResponse())
        }

        post("/weight-logs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<WeightLogRequest>()
            call.respond(HttpStatusCode.Created, healthService.addWeight(user.uid, request.toInput()).toResponse())
        }

        get("/weight-logs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(healthService.listWeights(user.uid).toResponse())
        }

        post("/photo-nutrition-jobs") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<PhotoNutritionJobRequest>()
            call.respond(HttpStatusCode.Created, healthService.createPhotoJob(user.uid, request.toInput()).toResponse())
        }

        get("/photo-nutrition-jobs/{jobId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(healthService.getPhotoJob(user.uid, call.jobId()).toResponse())
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.logId(): UUID =
    parameters["logId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Запис харчування не знайдено")

private fun io.ktor.server.application.ApplicationCall.jobId(): UUID =
    parameters["jobId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Завдання оцінки фото не знайдено")
