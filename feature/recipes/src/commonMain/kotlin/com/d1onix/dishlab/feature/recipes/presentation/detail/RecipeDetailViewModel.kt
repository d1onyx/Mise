package com.d1onix.dishlab.feature.recipes.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.GetRecipeUseCase
import com.d1onix.dishlab.domain.ObserveSavedRecipeIdsUseCase
import com.d1onix.dishlab.domain.ToggleSavedRecipeUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithInitCallback
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RecipeDetailAction {
    data object BackClicked : RecipeDetailAction
    data object SaveClicked : RecipeDetailAction
    data object StartCookingClicked : RecipeDetailAction
}

@Immutable
data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val products: List<Product> = emptyList(),
    val isSaved: Boolean = false,
)

@AssistedInject
class RecipeDetailViewModel(
    dependencies: CommonDependencies,
    @Assisted private val recipeId: RecipeId,
    private val getRecipe: GetRecipeUseCase,
    private val getProducts: GetProductsUseCase,
    private val toggleSaved: ToggleSavedRecipeUseCase,
    observeSavedIds: ObserveSavedRecipeIdsUseCase,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithInitCallback, WithMviState<RecipeDetailUiState> {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSavedIds().collect { ids ->
                _uiState.update { it.copy(isSaved = recipeId in ids) }
            }
        }
    }

    override suspend fun onInitialized() {
        val recipe = getRecipe(recipeId) ?: return
        _uiState.update { it.copy(recipe = recipe, products = getProducts(recipe.productIds)) }
    }

    fun onAction(action: RecipeDetailAction) {
        when (action) {
            RecipeDetailAction.BackClicked -> router.goBack()
            RecipeDetailAction.SaveClicked -> launch("toggleSaved") { toggleSaved(recipeId) }
            RecipeDetailAction.StartCookingClicked -> router.openCookingMode(recipeId)
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: RecipeId): RecipeDetailViewModel
    }
}
