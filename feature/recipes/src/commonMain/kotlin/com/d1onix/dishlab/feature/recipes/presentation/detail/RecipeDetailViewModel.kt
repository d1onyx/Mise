package com.d1onix.dishlab.feature.recipes.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.GetRecipeUseCase
import com.d1onix.dishlab.domain.ObserveSavedRecipeIdsUseCase
import com.d1onix.dishlab.domain.ToggleSavedRecipeUseCase
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
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
import kotlinx.coroutines.CancellationException

sealed interface RecipeDetailAction {
    data object BackClicked : RecipeDetailAction
    data object RetryClicked : RecipeDetailAction
    data object SaveClicked : RecipeDetailAction
}

@Immutable
data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val products: List<Product> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
)

@AssistedInject
class RecipeDetailViewModel(
    dependencies: CommonDependencies,
    @Assisted private val recipeId: RecipeId,
    @Assisted private val productIds: List<ProductId> = emptyList(),
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
        loadRecipe()
    }

    fun onAction(action: RecipeDetailAction) {
        when (action) {
            RecipeDetailAction.BackClicked -> router.goBack()
            RecipeDetailAction.RetryClicked -> launch("reloadRecipe") { loadRecipe() }
            RecipeDetailAction.SaveClicked -> launch("toggleSaved") { toggleSaved(recipeId) }
        }
    }

    private suspend fun loadRecipe() {
        _uiState.update { it.copy(isLoading = true, loadError = false) }
        try {
            val recipe = getRecipe(recipeId, productIds)
            if (recipe == null) {
                _uiState.update { it.copy(isLoading = false, loadError = true) }
                return
            }
            _uiState.update {
                it.copy(
                    recipe = recipe,
                    products = getProducts(recipe.productIds),
                    isLoading = false,
                )
            }
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            _uiState.update { it.copy(recipe = null, products = emptyList(), isLoading = false, loadError = true) }
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: RecipeId, productIds: List<ProductId>): RecipeDetailViewModel
    }
}
