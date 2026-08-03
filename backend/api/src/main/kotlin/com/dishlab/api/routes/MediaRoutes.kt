package com.dishlab.api.routes

import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

private val uploadsDir = File("uploads").also { it.mkdirs() }

@Serializable
data class MediaUploadResponse(val url: String)

fun Route.mediaRoutes(authVerifier: FirebaseAuthVerifier, baseUrl: String) {
    route("/api/v1/media") {
        post("/upload") {
            call.requireFirebaseUser(authVerifier) ?: return@post

            val multipart = call.receiveMultipart()
            var savedUrl: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "jpg")
                        ?.lowercase()
                        ?.take(5)
                        ?: "jpg"
                    val filename = "${UUID.randomUUID()}.$ext"
                    val file = File(uploadsDir, filename)
                    part.streamProvider().use { input ->
                        file.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                    savedUrl = "$baseUrl/uploads/$filename"
                }
                part.dispose()
            }

            val url = savedUrl
            if (url != null) {
                call.respond(HttpStatusCode.Created, MediaUploadResponse(url))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file part found"))
            }
        }
    }
}
