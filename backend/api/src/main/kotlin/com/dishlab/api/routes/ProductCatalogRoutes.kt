package com.dishlab.api.routes

import com.dishlab.api.dto.CatalogDumpResponse
import com.dishlab.api.dto.CatalogEntryResponse
import com.dishlab.api.dto.ProductCategoriesResponse
import com.dishlab.api.dto.ResolveClientProductRequest
import com.dishlab.api.dto.NormalizeProductsRequest
import com.dishlab.api.dto.NormalizeProductsResponse
import com.dishlab.api.dto.SaveCatalogProductRequest
import com.dishlab.api.dto.ValidateTagRequest
import com.dishlab.api.dto.ValidateTagResponse
import com.dishlab.api.dto.toDomain
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.ProductCatalogService
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.post

fun Route.productCatalogRoutes(
    authVerifier: FirebaseAuthVerifier,
    service: ProductCatalogService,
) {
    route("/api/v1/products") {
        get("/barcode/{barcode}") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val product = service.findByBarcode(
                firebaseUid = user.uid,
                barcode = call.parameters["barcode"].orEmpty(),
                language = call.request.queryParameters["language"],
            )
            if (product == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(product.toResponse())
            }
        }

        post("/resolve") {
            val request = call.receive<ResolveClientProductRequest>()
            call.respond(service.resolveClientProduct("anonymous", request.toDomain()).toResponse())
        }

        get("/search") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(
                service.search(
                    firebaseUid = user.uid,
                    query = call.request.queryParameters["q"].orEmpty(),
                    country = call.request.queryParameters["country"],
                    language = call.request.queryParameters["language"],
                    page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                    pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20,
                ).toResponse(),
            )
        }

        get("/categories") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            call.respond(
                ProductCategoriesResponse(
                    service.searchCategories(
                        firebaseUid = user.uid,
                        query = call.request.queryParameters["q"].orEmpty(),
                        language = call.request.queryParameters["language"].orEmpty(),
                    ),
                ),
            )
        }

        get("/categories/all") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val dump = service.allCategories(
                firebaseUid = user.uid,
                since = call.request.queryParameters["since"]?.takeIf(String::isNotBlank),
            )
            call.respond(
                CatalogDumpResponse(
                    version = dump.version,
                    items = dump.items.map { CatalogEntryResponse(name = it.name, category = it.category) },
                ),
            )
        }

        post("/normalize") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<NormalizeProductsRequest>()
            call.respond(
                NormalizeProductsResponse(
                    service.normalizeProducts(request.products.map { it.toDomain() })
                        .map { it.toResponse() },
                ),
            )
        }

        post("/tags/validate") {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            val request = call.receive<ValidateTagRequest>()
            val result = service.validateAndAddTag(
                firebaseUid = user.uid,
                tag = request.tag,
                productName = request.productName,
            )
            call.respond(ValidateTagResponse(valid = result.valid, tag = result.tag, reason = result.reason))
        }

        post {
            val user = call.requireFirebaseUser(authVerifier) ?: return@post
            call.respond(
                HttpStatusCode.Created,
                service.save(user.uid, call.receive<SaveCatalogProductRequest>().toDomain()).toResponse(),
            )
        }
    }
}
