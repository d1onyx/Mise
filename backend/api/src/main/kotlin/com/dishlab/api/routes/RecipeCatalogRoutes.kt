package com.dishlab.api.routes

import com.dishlab.api.dto.PantryMatchPageResponse
import com.dishlab.api.dto.RecipeCatalogFiltersResponse
import com.dishlab.api.dto.toCatalogResponse
import com.dishlab.api.dto.toResponse
import com.dishlab.api.middleware.requireFirebaseUser
import com.dishlab.application.service.RecipeCatalogService
import com.dishlab.application.service.CurrentUserResolver
import com.dishlab.application.service.RecipeService
import com.dishlab.domain.error.NotFoundError
import com.dishlab.infrastructure.firebase.FirebaseAuthVerifier
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.recipeCatalogRoutes(
    authVerifier: FirebaseAuthVerifier,
    service: RecipeCatalogService,
    currentUserResolver: CurrentUserResolver,
    recipeService: RecipeService,
) {
    route("/api/v1/recipe-catalog") {
        get {
            val user = call.requireFirebaseUser(authVerifier) ?: return@get
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            val query = call.request.queryParameters["q"]
            val category = call.request.queryParameters["category"]
            val catalog = withContext(Dispatchers.IO) {
                service.search(
                    firebaseUid = user.uid,
                    query = query,
                    category = category,
                    ingredient = call.request.queryParameters["ingredient"],
                    page = page,
                    pageSize = pageSize,
                )
            }
            val userRecipes = recipeService.list(
                firebaseUid = user.uid,
                tag = category,
                query = query,
                sort = null,
                page = page,
                pageSize = pageSize,
            )
            call.respond(
                com.dishlab.api.dto.CatalogRecipePageResponse(
                    items = (
                        userRecipes.items.map { it.toCatalogResponse() } +
                            catalog.items.map { it.toCatalogResponse() }
                        ).take(pageSize),
                    page = page,
                    pageSize = pageSize,
                    total = userRecipes.total + catalog.total,
                ),
            )
        }

        get("/filters") {
            // Categories actually present in the active catalog + any tags used by
            // published user-authored recipes — the client filters against this list
            // instead of hardcoding its own.
            val filters = withContext(Dispatchers.IO) {
                service.getCategories(firebaseUid = "anonymous", userRecipeTags = recipeService.listTags())
            }
            call.respond(RecipeCatalogFiltersResponse(categories = filters))
        }

        get("/pantry-match") {
            val ingredients = call.request.queryParameters.getAll("ingredient").orEmpty()
            val category = call.request.queryParameters["category"]
            val tags = call.request.queryParameters.getAll("tag").orEmpty()
            val strictTags = call.request.queryParameters["strictTags"]?.toBooleanStrictOrNull() ?: false
            val partialMatchOnly = call.request.queryParameters["partialMatchOnly"]?.toBooleanStrictOrNull() ?: false
            val exactMatch = call.request.queryParameters["exactMatch"]?.toBooleanStrictOrNull() ?: false
            // Each `exactGroup` is one selected product's synonym tags (comma-separated). Exact match
            // requires every group to be satisfied by at least one of its tags.
            val exactProductGroups = call.request.queryParameters.getAll("exactGroup").orEmpty()
                .map { group -> group.split(",").map(String::trim).filter(String::isNotBlank) }
                .filter { it.isNotEmpty() }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            // Relevance criterion when ingredients is empty (no scanned products yet): no
            // pantry-based WHERE clause is applied (see repository), so every active recipe
            // is a candidate and matched_count is 0 for all of them — the ORDER BY's
            // matched/total ratio term is therefore a no-op tie, and results fall through
            // to aggregated_rating DESC. So the "no products" default is highest-rated
            // recipes first, not catalog/insertion order.
            val result = withContext(Dispatchers.IO) {
                service.findByPantryIngredients(
                    firebaseUid = "anonymous",
                    ingredientNames = ingredients,
                    category = category,
                    tags = tags,
                    strictTags = strictTags,
                    page = page,
                    pageSize = pageSize,
                    partialMatchOnly = partialMatchOnly,
                    exactMatch = exactMatch,
                    exactProductGroups = exactProductGroups,
                )
            }
            call.respond(result.toResponse())
        }

        get("/{recipeId}") {
            // GET / merges two id spaces into one page: catalog recipes ("catalog:<long>") and
            // user-authored recipes (raw UUID, see Recipe.toCatalogResponse in RecipeCatalogDtos.kt).
            // The detail route has to resolve whichever one the client got from that list.
            val rawId = call.parameters["recipeId"] ?: throw NotFoundError("Рецепт не знайдено")
            if (rawId.startsWith("catalog:")) {
                val recipe = withContext(Dispatchers.IO) { service.get("anonymous", call.catalogRecipeId()) }
                call.respond(recipe.toCatalogResponse())
            } else {
                val recipeId = rawId.toUuidOrNull() ?: throw NotFoundError("Рецепт не знайдено")
                call.respond(recipeService.get("anonymous", recipeId).toCatalogResponse())
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.catalogRecipeId(): Long =
    parameters["recipeId"]
        ?.removePrefix("catalog:")
        ?.toLongOrNull()
        ?: throw NotFoundError("Рецепт не знайдено")

private fun String.toUuidOrNull(): java.util.UUID? = runCatching { java.util.UUID.fromString(this) }.getOrNull()
