package com.dishlab.api.routes

import com.dishlab.api.dto.SetConsentRequest
import com.dishlab.api.dto.UpdateAllergensRequest
import com.dishlab.api.dto.UpdateDietsRequest
import com.dishlab.api.dto.UpdateEquipmentRequest
import com.dishlab.api.dto.UpdateGoalsRequest
import com.dishlab.api.dto.UpdateProfileRequest
import com.dishlab.api.dto.UpdateSettingsRequest
import com.dishlab.api.dto.toDomain
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.IdentityService
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.meRoutes(
    authVerifier: FirebaseAuthVerifier,
    identityService: IdentityService,
) {
    route("/api/v1/me") {
        get {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(identityService.getMe(user.uid).toResponse())
        }

        patch("/profile") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<UpdateProfileRequest>()
            call.respond(identityService.updateProfile(user.uid, request.toDomain()).toResponse())
        }

        put("/allergens") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@put
            val request = call.receive<UpdateAllergensRequest>()
            call.respond(identityService.updateAllergens(user.uid, request.allergens).toResponse())
        }

        put("/diets") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@put
            val request = call.receive<UpdateDietsRequest>()
            call.respond(identityService.updateDiets(user.uid, request.diets).toResponse())
        }

        put("/equipment") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@put
            val request = call.receive<UpdateEquipmentRequest>()
            call.respond(identityService.updateEquipment(user.uid, request.equipment).toResponse())
        }

        put("/goals") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@put
            val request = call.receive<UpdateGoalsRequest>()
            call.respond(identityService.updateGoals(user.uid, request.goals).toResponse())
        }

        patch("/settings") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val request = call.receive<UpdateSettingsRequest>()
            call.respond(identityService.updateSettings(user.uid, request.toDomain()).toResponse())
        }

        post("/consents") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<SetConsentRequest>()
            call.respond(identityService.setConsent(user.uid, request.toDomain()).toResponse())
        }

        delete {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            call.respond(identityService.deleteMe(user.uid).toResponse())
        }
    }
}
