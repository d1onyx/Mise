package com.dishlab.backend

import com.dishlab.infrastructure.firebase.DevFirebaseAuthVerifier
import com.dishlab.application.service.CatalogRecipePage
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
 * credentials. Acceptance tests use `application { testModule() }`.
 */
fun Application.testModule() = appModule(DevFirebaseAuthVerifier(), TestRecipeCatalogRepository())

private class TestRecipeCatalogRepository : RecipeCatalogRepository {
    private val bookmarked = mutableSetOf<Pair<String, Long>>()
    private val recipes = listOf(
        recipe(38, "Low-Fat Berry Blue Frozen Dessert", listOf("flour", "water", "milk", "egg", "butter")),
        recipe(513667, "Zebra Cake", listOf("butter", "flour", "egg", "water")),
        recipe(777, "Egg-only omelette", listOf("egg")),
        recipe(778, "Flour-only flatbread", listOf("flour")),
    )

    override fun search(firebaseUid: String, query: String?, category: String?, ingredient: String?, page: Int, pageSize: Int) =
        CatalogRecipePage(recipes.filter { query.isNullOrBlank() || it.title.contains(query, true) }.map { withBookmark(firebaseUid, it) }, page, pageSize, recipes.size)

    override fun findByPantryIngredients(firebaseUid: String, ingredientNames: List<String>, category: String?, tags: List<String>, strictTags: Boolean, page: Int, pageSize: Int, partialMatchOnly: Boolean, exactMatch: Boolean, exactProductGroups: List<List<String>>) : PantryMatchPage {
        val selected = (ingredientNames + tags).map { it.removePrefix("en:") }.toSet()
        val matches = recipes.mapNotNull { raw ->
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
            if (qualifies) PantryMatchedRecipe(withBookmark(firebaseUid, raw), selected.count(names::contains), names.size) else null
        }
        return PantryMatchPage(matches, page, pageSize, matches.size)
    }

    override fun findById(firebaseUid: String, recipeId: Long) = recipes.find { it.id == recipeId }?.let { withBookmark(firebaseUid, it) }
    override fun setBookmarked(firebaseUid: String, recipeId: Long, bookmarked: Boolean) { if (bookmarked) this.bookmarked += firebaseUid to recipeId else this.bookmarked -= firebaseUid to recipeId }
    override fun getCategories() = listOf("Dessert")
    private fun withBookmark(uid: String, recipe: CatalogRecipe) = recipe.copy(bookmarked = uid to recipe.id in bookmarked)
    private fun recipe(id: Long, title: String, names: List<String>) = CatalogRecipe(id, title, category = "Dessert", ingredients = names.map { CatalogRecipeIngredient(it, canonicalTags = listOf("en:$it")) }, steps = listOf(CatalogRecipeStep(1, "Mix")))
}
