package com.dishlab.api.routes

import com.dishlab.api.dto.BookmarkResponse
import com.dishlab.api.dto.CompareRecipeVersionRequest
import com.dishlab.api.dto.CreateRecipeReportRequest
import com.dishlab.api.dto.CreateRecipeReviewRequest
import com.dishlab.api.dto.CreateRecipeVersionRequest
import com.dishlab.api.dto.MediaUploadUrlRequest
import com.dishlab.api.dto.RecipeVersionsResponse
import com.dishlab.api.dto.RecipeWriteRequest
import com.dishlab.api.dto.toDetailsResponse
import com.dishlab.api.dto.toInput
import com.dishlab.api.dto.toMediaType
import com.dishlab.api.dto.toResponse
import com.dishlab.api.dto.RecipeValidationResponseDto
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.CurrentUserResolver
import com.dishlab.application.service.RecipeService
import com.dishlab.domain.error.NotFoundError
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.recipeRoutes(
    authVerifier: FirebaseAuthVerifier,
    currentUserResolver: CurrentUserResolver,
    recipeService: RecipeService,
) {
    route("/api/v1/recipes") {
        get {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val viewer = currentUserResolver.resolve(user.uid)
            call.respond(
                recipeService.list(
                    firebaseUid = user.uid,
                    tag = call.request.queryParameters["tag"],
                    query = call.request.queryParameters["q"],
                    sort = call.request.queryParameters["sort"],
                    page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                    pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20,
                    ownedOnly = call.request.queryParameters["ownedOnly"]?.toBooleanStrictOrNull() ?: false,
                ).toResponse(viewer.id),
            )
        }

        post {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<RecipeWriteRequest>()
            val idempotencyKey = call.request.header("Idempotency-Key")?.takeIf { it.isNotBlank() }
            val viewer = currentUserResolver.resolve(user.uid)
            call.respond(
                HttpStatusCode.Created,
                recipeService.createDraft(user.uid, request.toInput(), idempotencyKey).toResponse(viewer.id),
            )
        }

        post("/validate") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<RecipeWriteRequest>()
            call.respond(recipeService.validateRecipe(user.uid, request.toInput()).toResponse())
        }

        get("/{recipeId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val viewer = currentUserResolver.resolve(user.uid)
            val recipe = recipeService.get(user.uid, call.recipeId())
            val reviews = recipeService.reviews(user.uid, recipe.id).items.map { it.toResponse() }
            call.respond(recipe.toDetailsResponse(reviews, viewer.id))
        }

        patch("/{recipeId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@patch
            val viewer = currentUserResolver.resolve(user.uid)
            val request = call.receive<RecipeWriteRequest>()
            call.respond(recipeService.updateDraft(user.uid, call.recipeId(), request.toInput()).toResponse(viewer.id))
        }

        delete("/{recipeId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            val recipeId = call.recipeId()
            recipeService.delete(user.uid, recipeId)
            call.respond(HttpStatusCode.NoContent)
        }

        post("/{recipeId}/publish") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val viewer = currentUserResolver.resolve(user.uid)
            call.respond(recipeService.publish(user.uid, call.recipeId()).toResponse(viewer.id))
        }

        post("/{recipeId}/fork") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val viewer = currentUserResolver.resolve(user.uid)
            call.respond(HttpStatusCode.Created, recipeService.fork(user.uid, call.recipeId()).toResponse(viewer.id))
        }

        get("/{recipeId}/versions") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(RecipeVersionsResponse(recipeService.versions(user.uid, call.recipeId()).map { it.toResponse() }))
        }

        post("/{recipeId}/versions") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CreateRecipeVersionRequest>()
            call.respond(HttpStatusCode.Created, recipeService.createVersion(user.uid, call.recipeId(), request.changelog, request.toInput()).toResponse())
        }

        get("/{recipeId}/versions/{versionId}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(recipeService.version(user.uid, call.recipeId(), call.versionId()).toResponse())
        }

        post("/{recipeId}/versions/{versionId}/compare") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CompareRecipeVersionRequest>()
            call.respond(recipeService.compare(user.uid, call.recipeId(), call.versionId(), request.targetVersionNumber).toResponse())
        }

        post("/{recipeId}/bookmark") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val recipe = recipeService.bookmark(user.uid, call.recipeId(), bookmarked = true)
            call.respond(BookmarkResponse(recipe.id.toString(), bookmarked = true))
        }

        delete("/{recipeId}/bookmark") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@delete
            val recipe = recipeService.bookmark(user.uid, call.recipeId(), bookmarked = false)
            call.respond(BookmarkResponse(recipe.id.toString(), bookmarked = false))
        }

        get("/{recipeId}/reviews") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(recipeService.reviews(user.uid, call.recipeId()).toResponse())
        }

        post("/{recipeId}/reviews") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CreateRecipeReviewRequest>()
            call.respond(HttpStatusCode.Created, recipeService.addReview(user.uid, call.recipeId(), request.rating, request.comment).toResponse())
        }

        post("/{recipeId}/reports") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<CreateRecipeReportRequest>()
            call.respond(HttpStatusCode.Created, recipeService.report(user.uid, call.recipeId(), request.reason, request.comment).toResponse())
        }

        post("/{recipeId}/media/upload-url") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<MediaUploadUrlRequest>()
            call.respond(
                recipeService.mediaUploadUrl(
                    firebaseUid = user.uid,
                    recipeId = call.recipeId(),
                    mediaType = request.toMediaType(),
                    fileName = request.fileName,
                    fileSizeBytes = request.fileSizeBytes,
                ).toResponse(),
            )
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.recipeId(): UUID =
    parameters["recipeId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Рецепт не знайдено")

private fun io.ktor.server.application.ApplicationCall.versionId(): UUID =
    parameters["versionId"]?.let { UUID.fromString(it) } ?: throw NotFoundError("Версію рецепта не знайдено")
