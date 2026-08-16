package com.d1onix.dishlab.data.storage

import com.d1onix.dishlab.domain.model.Ingredient
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep
import com.d1onyx.core.datastore.InMemoryKeyValueStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoredSavedRecipesRepositoryTest {

    // A recipe reached through pantry-match/backend catalogue, not the bundled demo list —
    // the id that used to disappear from the Saved screen.
    private val pantryMatchedRecipe = Recipe(
        id = RecipeId("pantry-match-42"),
        name = "Lentil Soup",
        minutes = 25,
        difficulty = RecipeDifficulty.Easy,
        categories = listOf("soup"),
        productIds = listOf(ProductId("product-1")),
        description = "Warm and filling.",
        ingredients = listOf(Ingredient(quantity = "1 cup", name = "lentils", matched = true)),
        steps = listOf(RecipeStep(title = "Simmer", description = "Cook until soft.")),
    )

    @Test
    fun `saving a recipe not in the bundled catalogue makes it resolve from local storage`() = runTest {
        val repository = StoredSavedRecipesRepository(InMemoryKeyValueStorage())

        repository.toggle(pantryMatchedRecipe)

        assertEquals(setOf(pantryMatchedRecipe.id), repository.saved.first())
        assertEquals(listOf(pantryMatchedRecipe), repository.savedRecipes.first())
    }

    @Test
    fun `toggling twice unsaves the recipe`() = runTest {
        val repository = StoredSavedRecipesRepository(InMemoryKeyValueStorage())

        repository.toggle(pantryMatchedRecipe)
        repository.toggle(pantryMatchedRecipe)

        assertTrue(repository.saved.first().isEmpty())
        assertTrue(repository.savedRecipes.first().isEmpty())
    }

    @Test
    fun `saved recipe content survives a fresh repository instance over the same storage`() = runTest {
        val storage = InMemoryKeyValueStorage()
        StoredSavedRecipesRepository(storage).toggle(pantryMatchedRecipe)

        val reopened = StoredSavedRecipesRepository(storage)

        assertEquals(listOf(pantryMatchedRecipe), reopened.savedRecipes.first())
    }
}
