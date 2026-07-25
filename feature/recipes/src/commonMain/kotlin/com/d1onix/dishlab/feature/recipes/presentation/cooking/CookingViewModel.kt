package com.d1onix.dishlab.feature.recipes.presentation.cooking

import androidx.compose.runtime.Immutable
import com.d1onix.dishlab.domain.GetRecipeUseCase
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.RecipeStep
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithInitCallback
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@Immutable
data class CookingUiState(
    val recipe: Recipe? = null,
    val stepIndex: Int = 0,
    val remainingSeconds: Int? = null,
    val isTimerRunning: Boolean = false,
) {
    val step: RecipeStep? get() = recipe?.steps?.getOrNull(stepIndex)
    val stepCount: Int get() = recipe?.steps?.size ?: 0
    val isLastStep: Boolean get() = recipe != null && stepIndex == stepCount - 1

    /** Only steps with a minute-scale timer get the countdown panel. */
    val timerSeconds: Int? get() = step?.timerSeconds?.takeIf { it >= 60 }

    /** `mm:ss` — a number format, not copy, so it stays in the state. */
    val timerLabel: String
        get() {
            val seconds = remainingSeconds ?: timerSeconds ?: 0
            return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        }

    val timerButton: TimerButton
        get() = when {
            isTimerRunning -> TimerButton.Pause
            remainingSeconds != null && remainingSeconds < (timerSeconds ?: 0) -> TimerButton.Resume
            else -> TimerButton.Start
        }
}

enum class TimerButton { Start, Pause, Resume }

sealed interface CookingAction {
    data object CloseClicked : CookingAction
    data object PreviousClicked : CookingAction
    data object NextClicked : CookingAction
    data object TimerClicked : CookingAction
}

@AssistedInject
class CookingViewModel(
    dependencies: CommonDependencies,
    @Assisted private val recipeId: RecipeId,
    private val getRecipe: GetRecipeUseCase,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithInitCallback, WithMviState<CookingUiState> {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null

    override suspend fun onInitialized() {
        val recipe = getRecipe(recipeId) ?: return
        _uiState.update { it.copy(recipe = recipe) }
    }

    fun onAction(action: CookingAction) {
        when (action) {
            CookingAction.CloseClicked -> close()
            CookingAction.PreviousClicked -> previousStep()
            CookingAction.NextClicked -> nextStep()
            CookingAction.TimerClicked -> toggleTimer()
        }
    }

    private fun nextStep() {
        if (_uiState.value.isLastStep) {
            close()
            return
        }
        stopTimer()
        _uiState.update { it.copy(stepIndex = it.stepIndex + 1, remainingSeconds = null) }
    }

    private fun previousStep() {
        if (_uiState.value.stepIndex == 0) return
        stopTimer()
        _uiState.update { it.copy(stepIndex = it.stepIndex - 1, remainingSeconds = null) }
    }

    private fun toggleTimer() {
        if (_uiState.value.isTimerRunning) {
            stopTimer()
            return
        }
        val total = _uiState.value.timerSeconds ?: return
        val from = _uiState.value.remainingSeconds?.takeIf { it > 0 } ?: total
        _uiState.update { it.copy(remainingSeconds = from, isTimerRunning = true) }
        ticker = viewModelScope.launch {
            while (isActive && (_uiState.value.remainingSeconds ?: 0) > 0) {
                delay(1000)
                _uiState.update { it.copy(remainingSeconds = (it.remainingSeconds ?: 0) - 1) }
            }
            _uiState.update { it.copy(isTimerRunning = false) }
        }
    }

    private fun close() {
        stopTimer()
        router.goBack()
    }

    private fun stopTimer() {
        ticker?.cancel()
        ticker = null
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: RecipeId): CookingViewModel
    }
}
