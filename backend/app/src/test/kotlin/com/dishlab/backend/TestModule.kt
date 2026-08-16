package com.dishlab.backend

import com.dishlab.infrastructure.firebase.DevFirebaseAuthVerifier
import com.dishlab.application.service.CatalogFilters
import com.dishlab.application.service.CatalogRecipePage
import com.dishlab.application.service.InMemoryRecipeRepository
import com.dishlab.application.service.PantryMatchPage
import com.dishlab.application.service.RecipeCatalogRepository
import com.dishlab.domain.model.CatalogRecipe
import com.dishlab.domain.model.CatalogRecipeIngredient
import com.dishlab.domain.model.CatalogRecipeStep
import com.dishlab.domain.model.PantryMatchedRecipe
import io.ktor.server.application.Application

/**
 * Test composition root — wires the full app but swaps the production
 * Firebase verifier for [DevFirebaseAuthVerifier], which accepts `Bearer :<uid>`
 * tokens. Skips `FirebaseInitializer.init()`, so tests need no Firebase
 * credentials. Also forces an in-memory recipe repository so acceptance tests
 * never write fixture recipes into a real Postgres reachable via `.env`.
 * Acceptance tests use `application { testModule() }`.
 */
fun Application.testModule() = appModule(
    DevFirebaseAuthVerifier(),
    TestRecipeCatalogRepository(),
    InMemoryRecipeRepository(),
)

private class TestRecipeCatalogRepository : RecipeCatalogRepository {
    private val recipes = listOf(
        recipe(38, "Low-Fat Berry Blue Frozen Dessert", listOf("flour", "water", "milk", "egg", "butter")),
        recipe(513667, "Zebra Cake", listOf("butter", "flour", "egg", "water")),
        recipe(777, "Egg-only omelette", listOf("egg")),
        recipe(778, "Flour-only flatbread", listOf("flour")),
    )

    override fun search(
        firebaseUid: String,
        query: String?,
        categories: List<String>,
        cuisines: List<String>,
        equipment: List<String>,
        techniques: List<String>,
        ingredient: String?,
        page: Int,
        pageSize: Int,
    ): CatalogRecipePage {
        val filtered = recipes
            .filter { query.isNullOrBlank() || it.title.contains(query, true) }
            .filter { categories.isEmpty() || categories.any { c -> c.equals(it.category, ignoreCase = true) } }
        return CatalogRecipePage(filtered, page, pageSize, filtered.size)
    }

    override fun findByPantryIngredients(
        firebaseUid: String,
        ingredientNames: List<String>,
        categories: List<String>,
        cuisines: List<String>,
        equipment: List<String>,
        techniques: List<String>,
        tags: List<String>,
        strictTags: Boolean,
        page: Int,
        pageSize: Int,
        partialMatchOnly: Boolean,
        exactMatch: Boolean,
        exactProductGroups: List<List<String>>,
    ) : PantryMatchPage {
        val selected = (ingredientNames + tags).map { it.removePrefix("en:") }.toSet()
        val matches = recipes
            .filter { categories.isEmpty() || categories.any { c -> c.equals(it.category, ignoreCase = true) } }
            .mapNotNull { raw ->
                val names = raw.ingredients.flatMap { it.canonicalTags }.map { it.removePrefix("en:") }.toSet()
                val grouped = exactProductGroups.all { group -> group.any { it.removePrefix("en:") in names } }
                val plainMatch = selected.any(names::contains)
                val qualifies = when {
                    exactProductGroups.isNotEmpty() && exactMatch -> grouped
                    exactProductGroups.isNotEmpty() -> grouped || plainMatch
                    exactMatch -> selected.all(names::contains)
                    partialMatchOnly -> plainMatch
                    else -> true
                }
                if (qualifies) {
                    val withMatchFlags = raw.copy(
                        ingredients = raw.ingredients.map { ingredient ->
                            ingredient.copy(matched = ingredient.canonicalTags.any { it.removePrefix("en:") in selected })
                        },
                    )
                    PantryMatchedRecipe(withMatchFlags, selected.count(names::contains), names.size)
                } else {
                    null
                }
            }
        return PantryMatchPage(matches, page, pageSize, matches.size)
    }

    override fun findById(firebaseUid: String, recipeId: Long, ingredientNames: List<String>): CatalogRecipe? {
        val raw = recipes.find { it.id == recipeId } ?: return null
        val selected = ingredientNames.map { it.removePrefix("en:") }.toSet()
        return raw.copy(
            ingredients = raw.ingredients.map { ingredient ->
                ingredient.copy(matched = ingredient.canonicalTags.any { it.removePrefix("en:") in selected })
            },
        )
    }
    override fun getFilters() = CatalogFilters(
        categories = listOf("Dessert"),
        cuisines = listOf("American"),
        equipment = listOf("oven"),
        techniques = listOf("bake"),
    )
    private fun recipe(id: Long, title: String, names: List<String>) = CatalogRecipe(id, title, category = "Dessert", ingredients = names.map { CatalogRecipeIngredient(it, canonicalTags = listOf("en:$it")) }, steps = listOf(CatalogRecipeStep(1, "Mix")))
}
