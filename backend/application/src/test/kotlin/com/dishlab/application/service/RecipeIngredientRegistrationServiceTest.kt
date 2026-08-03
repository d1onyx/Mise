package com.dishlab.application.service

import com.dishlab.domain.model.RecipeIngredient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RecipeIngredientRegistrationServiceTest {

    @Test
    fun `existing ingredient is linked without calling AI`() = runBlocking {
        val ingredients = InMemoryIngredientRepository()
        var validationCalls = 0
        val service = service(
            ingredients = ingredients,
            validator = RecipeIngredientValidationProvider { _, _ ->
                validationCalls += 1
                error("AI must not be called for a known ingredient")
            },
        )

        val recipe = service.createDraft(
            USER_ID,
            RecipeInput(
                title = "Tomato plate",
                servings = 1,
                ingredients = listOf(RecipeIngredient(name = "tomato", amount = 1.0, unit = "шт")),
            ),
        )

        assertEquals(0, validationCalls)
        assertEquals(4, ingredients.findAll().size)
        assertEquals("Tomato", recipe.currentVersion.ingredients.single().name)
        assertNotNull(recipe.currentVersion.ingredients.single().ingredientId)
        Unit
    }

    @Test
    fun `validated unknown ingredient is registered and linked to recipe`() = runBlocking {
        val ingredients = InMemoryIngredientRepository()
        val service = service(
            ingredients = ingredients,
            validator = RecipeIngredientValidationProvider { requested, _ ->
                RecipeIngredientsValidationResult(
                    items = requested.map {
                        IngredientValidationItem(
                            name = it.name,
                            isFood = true,
                            isEdible = true,
                            isAmountReasonable = true,
                            canonicalTags = listOf("en:cheese"),
                        )
                    },
                )
            },
        )

        val recipe = service.createDraft(
            USER_ID,
            RecipeInput(
                title = "Cheese plate",
                servings = 1,
                ingredients = listOf(RecipeIngredient(name = "сир", amount = 1.0, unit = "шт")),
            ),
        )

        val stored = ingredients.findByName("сир")
        assertNotNull(stored)
        assertEquals(stored.id, recipe.currentVersion.ingredients.single().ingredientId)
        assertEquals(listOf("en:cheese"), recipe.currentVersion.ingredients.single().canonicalTags)
        assertEquals(5, ingredients.findAll().size)
        Unit
    }

    @Test
    fun `unknown ingredient is saved but not registered when AI validation is unavailable`() = runBlocking {
        val ingredients = InMemoryIngredientRepository()
        val service = service(
            ingredients = ingredients,
            validator = RecipeIngredientValidationProvider { requested, _ ->
                RecipeIngredientsValidationResult(
                    items = requested.map {
                        IngredientValidationItem(
                            name = it.name,
                            isFood = false,
                            isEdible = false,
                            isAmountReasonable = false,
                        )
                    },
                    available = false,
                    unavailableReason = "timeout",
                )
            },
        )

        val recipe = service.createDraft(
            USER_ID,
            RecipeInput(
                title = "Unknown plate",
                servings = 1,
                ingredients = listOf(RecipeIngredient(name = "невідоме", amount = 1.0, unit = "шт")),
            ),
        )

        assertEquals("невідоме", recipe.currentVersion.ingredients.single().name)
        assertEquals(null, recipe.currentVersion.ingredients.single().ingredientId)
        assertEquals(null, ingredients.findByName("невідоме"))
        assertEquals(4, ingredients.findAll().size)
        Unit
    }

    @Test
    fun `ai tag enrichment updates saved recipe in background`() = runBlocking {
        val ingredients = InMemoryIngredientRepository()
        val recipes = InMemoryRecipeRepository()
        val service = service(
            recipes = recipes,
            ingredients = ingredients,
            validator = RecipeIngredientValidationProvider { requested, _ ->
                RecipeIngredientsValidationResult(
                    items = requested.map {
                        IngredientValidationItem(
                            name = it.name,
                            isFood = false,
                            isEdible = false,
                            isAmountReasonable = false,
                        )
                    },
                    available = false,
                    unavailableReason = "timeout",
                )
            },
            ingredientCatalog = FakeIngredientTagCatalog,
            tagValidator = object : TagValidationProvider {
                override suspend fun validate(tag: String, productName: String): TagValidationResult =
                    TagValidationResult(valid = true, tag = "chicken")
            },
            backgroundScope = CoroutineScope(coroutineContext),
        )

        val recipe = service.createDraft(
            USER_ID,
            RecipeInput(
                title = "Chicken plate",
                servings = 1,
                ingredients = listOf(RecipeIngredient(name = "курка", amount = 1.0, unit = "шт")),
            ),
        )

        assertEquals(emptyList(), recipe.currentVersion.ingredients.single().canonicalTags)
        yield()
        assertEquals(
            listOf("en:chicken"),
            recipes.findById(recipe.id)?.currentVersion?.ingredients?.single()?.canonicalTags,
        )
    }

    private fun service(
        recipes: RecipeRepository = InMemoryRecipeRepository(),
        ingredients: IngredientRepository,
        validator: RecipeIngredientValidationProvider,
        ingredientCatalog: IngredientTagCatalog? = null,
        tagValidator: TagValidationProvider? = null,
        backgroundScope: CoroutineScope? = null,
    ) = RecipeService(
        currentUserResolver = CurrentUserResolver(InMemoryUserAccountRepository()),
        recipes = recipes,
        ingredientValidationProvider = validator,
        ingredientRepository = ingredients,
        ingredientCatalog = ingredientCatalog,
        tagValidator = tagValidator,
        backgroundScope = backgroundScope ?: CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
    )

    private object FakeIngredientTagCatalog : IngredientTagCatalog {
        override fun search(query: String, limit: Int): List<String> = emptyList()
        override fun addNew(name: String, category: String): Boolean = true
        override fun resolve(name: String): String? = name.takeIf { it == "chicken" }
        override fun categories(): List<String> = listOf("other")
        override fun all(): List<IngredientTagEntry> = emptyList()
        override fun version(): String = "test"
    }

    private companion object {
        const val USER_ID = "ingredient-registration-test-user"
    }
}
