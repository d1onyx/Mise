package com.d1onix.dishlab.domain.usecases

import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeFilters
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.TimeBucket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterRecipesUseCaseTest {

    private val filter = FilterRecipesUseCaseImpl()

    private val bowl = recipe("bowl", "Banana Oat Breakfast Bowl", 10, RecipeDifficulty.Easy, "Breakfast")
    private val pancakes = recipe("pancakes", "Banana Oat Pancakes", 20, RecipeDifficulty.Medium, "Dessert")
    private val meatballs = recipe("meatballs", "Chicken & Oat Meatballs", 35, RecipeDifficulty.Hard, "Dinner")
    private val all = listOf(bowl, pancakes, meatballs)

    @Test
    fun `empty filters keep every recipe`() {
        assertEquals(all, filter(all, RecipeFilters()))
    }

    @Test
    fun `query matches the name case-insensitively`() {
        assertEquals(listOf(meatballs), filter(all, RecipeFilters(query = "CHICKEN")))
    }

    @Test
    fun `blank query is not a filter`() {
        assertEquals(all, filter(all, RecipeFilters(query = "   ")))
    }

    @Test
    fun `difficulties are a union within the group`() {
        val result = filter(
            all,
            RecipeFilters(difficulties = setOf(RecipeDifficulty.Easy, RecipeDifficulty.Hard)),
        )
        assertEquals(listOf(bowl, meatballs), result)
    }

    @Test
    fun `time buckets overlap and both are accepted`() {
        val result = filter(all, RecipeFilters(times = setOf(TimeBucket.Under15, TimeBucket.Over30)))
        assertEquals(listOf(bowl, meatballs), result)
    }

    @Test
    fun `groups combine with AND`() {
        val result = filter(
            all,
            RecipeFilters(
                difficulties = setOf(RecipeDifficulty.Easy, RecipeDifficulty.Medium),
                times = setOf(TimeBucket.Under30),
                categories = setOf("Dessert"),
            ),
        )
        assertEquals(listOf(pancakes), result)
    }

    @Test
    fun `contradictory filters produce nothing`() {
        val result = filter(
            all,
            RecipeFilters(difficulties = setOf(RecipeDifficulty.Hard), times = setOf(TimeBucket.Under15)),
        )
        assertTrue(result.isEmpty())
    }

    private fun recipe(
        id: String,
        name: String,
        minutes: Int,
        difficulty: RecipeDifficulty,
        category: String,
    ) = Recipe(
        id = RecipeId(id),
        name = name,
        minutes = minutes,
        difficulty = difficulty,
        categories = listOf(category),
        productIds = emptyList(),
        description = "",
        ingredients = emptyList(),
        steps = emptyList(),
    )
}
