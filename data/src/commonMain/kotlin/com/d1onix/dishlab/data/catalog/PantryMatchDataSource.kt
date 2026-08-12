package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.domain.model.Ingredient
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@SingleIn(AppScope::class)
@Inject
class PantryMatchDataSource(
    private val client: HttpClient,
) {
    suspend fun match(
        ingredients: List<String>,
        exactGroups: List<List<String>> = emptyList(),
        page: Int = 1,
        pageSize: Int = 20,
    ): PantryMatchPageDto = client.get("api/v1/recipe-catalog/pantry-match") {
        ingredients.forEach { parameter("ingredient", it) }
        exactGroups.forEach { group -> parameter("exactGroup", group.joinToString(",")) }
        parameter("page", page)
        parameter("pageSize", pageSize)
    }.body()

}

/** Fetches the complete recipe because pantry-match intentionally returns card-sized items. */
@SingleIn(AppScope::class)
@Inject
class RecipeCatalogRemoteDataSource(
    private val client: HttpClient,
) {
    suspend fun recipe(id: RecipeId): CatalogRecipeDto =
        client.get("api/v1/recipe-catalog/${id.value}").body()

    suspend fun recipes(page: Int, pageSize: Int): CatalogRecipePageDto =
        client.get("api/v1/recipe-catalog") {
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()
}

@Serializable
data class PantryMatchPageDto(
    val items: List<PantryMatchedRecipeDto>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

@Serializable
data class CatalogRecipePageDto(
    val items: List<CatalogRecipeDto>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

@Serializable
data class PantryMatchedRecipeDto(
    val recipe: CatalogRecipeDto,
    val matchedCount: Int,
    val totalIngredients: Int,
    val matchPercent: Int,
)

@Serializable
data class CatalogRecipeDto(
    val id: String,
    val title: String,
    val totalTime: String? = null,
    val description: String? = null,
    val category: String? = null,
    val ingredients: List<CatalogIngredientDto>,
    val steps: List<CatalogRecipeStepDto>,
)

@Serializable
data class CatalogIngredientDto(
    val name: String,
    val quantity: String? = null,
)

@Serializable
data class CatalogRecipeStepDto(
    val position: Int,
    val text: String,
)

fun PantryMatchedRecipeDto.toDomain(): Recipe = recipe.toDomain()

fun CatalogRecipeDto.toDomain(): Recipe = Recipe(
    id = RecipeId(id),
    name = title,
    minutes = totalTime?.filter(Char::isDigit)?.toIntOrNull() ?: 0,
    difficulty = RecipeDifficulty.Easy,
    categories = listOfNotNull(category),
    productIds = emptyList(),
    description = description.orEmpty(),
    ingredients = ingredients.map { Ingredient(quantity = it.quantity.orEmpty(), name = it.name) },
    steps = steps.sortedBy(CatalogRecipeStepDto::position).map { RecipeStep(title = "", description = it.text) },
)
