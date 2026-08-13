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
            val categories = call.request.queryParameters.getAll("category").orEmpty()
            val cuisines = call.request.queryParameters.getAll("cuisine").orEmpty()
            val equipment = call.request.queryParameters.getAll("equipment").orEmpty()
            val techniques = call.request.queryParameters.getAll("technique").orEmpty()
            val catalog = withContext(Dispatchers.IO) {
                service.search(
                    firebaseUid = user.uid,
                    query = query,
                    categories = categories,
                    cuisines = cuisines,
                    equipment = equipment,
                    techniques = techniques,
                    ingredient = call.request.queryParameters["ingredient"],
                    page = page,
                    pageSize = pageSize,
                )
            }
            // User-authored recipes only carry a flat `tags` list (RecipeService.list's single
            // `tag` param) — they have no cuisine/equipment/technique dimension to filter on the
            // way the SQLite catalog's keywords do. Once any of those groups is active, merging
            // in unfiltered user recipes would silently ignore the filter for half the results,
            // so skip that half entirely rather than show results that didn't match.
            val userRecipes = if (cuisines.isEmpty() && equipment.isEmpty() && techniques.isEmpty()) {
                recipeService.list(
                    firebaseUid = user.uid,
                    tag = categories.firstOrNull(),
                    query = query,
                    sort = null,
                    page = page,
                    pageSize = pageSize,
                )
            } else {
                null
            }
            call.respond(
                com.dishlab.api.dto.CatalogRecipePageResponse(
                    items = (
                        userRecipes?.items.orEmpty().map { it.toCatalogResponse() } +
                            catalog.items.map { it.toCatalogResponse() }
                        ).take(pageSize),
                    page = page,
                    pageSize = pageSize,
                    total = (userRecipes?.total ?: 0) + catalog.total,
                ),
            )
        }

        get("/filters") {
            // Categories/cuisines/equipment/techniques actually present in the active catalog
            // (grouped server-side — see CatalogFilters — so the client renders separate filter
            // groups instead of one flat list) + any tags used by published user-authored
            // recipes. Cached in the repository keyed on SQLite's data_version, so this does not
            // rescan the catalog on every call — see SqliteRecipeCatalogRepository.getFilters.
            val filters = withContext(Dispatchers.IO) {
                service.getFilters(firebaseUid = "anonymous", userRecipeTags = recipeService.listTags())
            }
            call.respond(
                RecipeCatalogFiltersResponse(
                    categories = filters.categories,
                    cuisines = filters.cuisines,
                    equipment = filters.equipment,
                    techniques = filters.techniques,
                ),
            )
        }

        get("/pantry-match") {
            val ingredients = call.request.queryParameters.getAll("ingredient").orEmpty()
            val categories = call.request.queryParameters.getAll("category").orEmpty()
            val cuisines = call.request.queryParameters.getAll("cuisine").orEmpty()
            val equipment = call.request.queryParameters.getAll("equipment").orEmpty()
            val techniques = call.request.queryParameters.getAll("technique").orEmpty()
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
                    categories = categories,
                    cuisines = cuisines,
                    equipment = equipment,
                    techniques = techniques,
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
