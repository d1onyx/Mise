package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.repository.SavedRecipesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeSavedRecipesRepository(recipes: List<Recipe>) : SavedRecipesRepository {
    private val recipesFlow = MutableStateFlow(recipes)
    override val saved: Flow<Set<RecipeId>> = MutableStateFlow(recipes.map { it.id }.toSet())
    override val savedRecipes: Flow<List<Recipe>> = recipesFlow
    override suspend fun toggle(recipe: Recipe) = Unit
}

class GetSavedRecipeUseCaseTest {

    private val recipe = Recipe(
        id = RecipeId("pantry-match-42"),
        name = "Lentil Soup",
        minutes = 25,
        difficulty = RecipeDifficulty.Easy,
        categories = listOf("soup"),
        productIds = emptyList(),
        description = "Warm and filling.",
        ingredients = emptyList(),
        steps = emptyList(),
    )

    @Test
    fun `resolves a recipe cached locally by save`() = runTest {
        val useCase = GetSavedRecipeUseCaseImpl(FakeSavedRecipesRepository(listOf(recipe)))

        assertEquals(recipe, useCase(recipe.id))
    }

    @Test
    fun `returns null for an id that was never saved`() = runTest {
        val useCase = GetSavedRecipeUseCaseImpl(FakeSavedRecipesRepository(emptyList()))

        assertNull(useCase(RecipeId("never-saved")))
    }
}
