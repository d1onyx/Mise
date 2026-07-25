package com.d1onix.dishlab.feature.recipes.presentation.cooking

import com.d1onix.dishlab.domain.GetRecipeUseCase
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.CommonDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CookingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the recipe loads on the first step`() = runTest(dispatcher) {
        val viewModel = viewModel()
        testScheduler.advanceUntilIdle()

        assertEquals("Simmer", viewModel.uiState.value.step?.title)
        assertEquals(3, viewModel.uiState.value.stepCount)
        assertFalse(viewModel.uiState.value.isLastStep)
    }

    @Test
    fun `the timer counts down and stops the ticker on pause`() = runTest(dispatcher) {
        val viewModel = viewModel()
        testScheduler.advanceUntilIdle()

        viewModel.onAction(CookingAction.TimerClicked)
        testScheduler.advanceTimeBy(3_100)

        assertEquals(117, viewModel.uiState.value.remainingSeconds)
        assertEquals("1:57", viewModel.uiState.value.timerLabel)
        assertTrue(viewModel.uiState.value.isTimerRunning)

        viewModel.onAction(CookingAction.TimerClicked)
        testScheduler.advanceTimeBy(5_000)

        assertEquals(117, viewModel.uiState.value.remainingSeconds)
        assertEquals(TimerButton.Resume, viewModel.uiState.value.timerButton)
    }

    @Test
    fun `moving between steps resets the timer`() = runTest(dispatcher) {
        val viewModel = viewModel()
        testScheduler.advanceUntilIdle()

        viewModel.onAction(CookingAction.TimerClicked)
        testScheduler.advanceTimeBy(2_100)
        viewModel.onAction(CookingAction.NextClicked)

        assertEquals(1, viewModel.uiState.value.stepIndex)
        assertNull(viewModel.uiState.value.remainingSeconds)
        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun `only minute-scale steps offer a countdown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        testScheduler.advanceUntilIdle()

        viewModel.onAction(CookingAction.NextClicked)
        assertNull(viewModel.uiState.value.timerSeconds) // 30 s step
        viewModel.onAction(CookingAction.NextClicked)
        assertNull(viewModel.uiState.value.timerSeconds) // no timer at all
    }

    @Test
    fun `finishing the last step leaves the screen`() = runTest(dispatcher) {
        val router = FakeRouter()
        val viewModel = viewModel(router)
        testScheduler.advanceUntilIdle()

        viewModel.onAction(CookingAction.NextClicked)
        viewModel.onAction(CookingAction.NextClicked)
        assertTrue(viewModel.uiState.value.isLastStep)

        viewModel.onAction(CookingAction.NextClicked)
        assertEquals(1, router.backCount)
    }

    private fun viewModel(router: RecipesRouter = FakeRouter()) = CookingViewModel(
        dependencies = CommonDependencies(DefaultLogger(RecordingLogSink()), ExceptionHandler { }),
        recipeId = RecipeId("bowl"),
        getRecipe = GetRecipeUseCase { id -> recipe.takeIf { it.id == id } },
        router = router,
    )

    private val recipe = Recipe(
        id = RecipeId("bowl"),
        name = "Banana Oat Bowl",
        minutes = 10,
        difficulty = RecipeDifficulty.Easy,
        categories = listOf("Breakfast"),
        productIds = emptyList(),
        description = "",
        ingredients = emptyList(),
        steps = listOf(
            RecipeStep("Simmer", "Simmer the oats", timerSeconds = 120),
            RecipeStep("Slice", "Slice the banana", timerSeconds = 30),
            RecipeStep("Serve", "Assemble and serve"),
        ),
    )

    private class FakeRouter : RecipesRouter {
        var backCount = 0
        override fun openRecipe(id: RecipeId) = Unit
        override fun openCookingMode(id: RecipeId) = Unit
        override fun goBack() {
            backCount++
        }
    }
}
