package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.FilterRecipesUseCase
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.GetRecipesForProductsUseCase
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Recipes for what is currently on the combination graph. */
@Inject
class RecipesViewModel(
    dependencies: CommonDependencies,
    private val getRecipesForProducts: GetRecipesForProductsUseCase,
    private val filterRecipes: FilterRecipesUseCase,
    private val getProducts: GetProductsUseCase,
    private val session: ScanSessionStore,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithMviState<RecipeListUiState> {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.products.collectLatest { ids ->
                load(ids)
            }
        }
    }

    fun onAction(action: RecipeListAction) {
        when (action) {
            is RecipeListAction.QueryChanged -> _uiState.update {
                it.copy(filters = it.filters.copy(query = action.value)).refiltered()
            }

            is RecipeListAction.GroupClicked -> _uiState.update {
                it.copy(expandedGroup = if (it.expandedGroup == action.group) null else action.group)
            }

            is RecipeListAction.OptionClicked -> _uiState.update {
                it.copy(filters = it.filters.toggle(action.group, action.option)).refiltered()
            }

            is RecipeListAction.RecipeClicked -> router.openRecipe(action.id)
            RecipeListAction.BackClicked -> router.goBack()
            RecipeListAction.RetryClicked -> launch("retryRecipes") { load(session.products.value) }
        }
    }

    private suspend fun load(ids: List<com.d1onix.dishlab.domain.model.ProductId>) = try {
        val recipes = if (ids.isEmpty()) emptyList() else getRecipesForProducts(ids)
        val products = getProducts(recipes.flatMap { it.productIds }.distinct())
        _uiState.update { it.copy(all = recipes, products = products.associateBy { p -> p.id }, loadError = false).refiltered() }
    } catch (_: Throwable) {
        _uiState.update { it.copy(all = emptyList(), visible = emptyList(), loadError = true) }
    }

    private fun RecipeListUiState.refiltered(): RecipeListUiState =
        copy(visible = filterRecipes(all, filters))
}
