package com.d1onix.dishlab.feature.recipes.presentation.list

import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.FilterRecipesUseCase
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.domain.GetRecipeCatalogFiltersUseCase
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
    private val getCatalogFilters: GetRecipeCatalogFiltersUseCase,
    private val filterRecipes: FilterRecipesUseCase,
    private val getProducts: GetProductsUseCase,
    private val session: ScanSessionStore,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithMviState<RecipeListUiState> {

    private val _uiState = MutableStateFlow(RecipeListUiState(isLoading = true))
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            session.products.collectLatest { ids ->
                load(ids)
            }
        }
        launch("loadRecipeCatalogFilters") {
            val catalogFilters = runCatching { getCatalogFilters() }.getOrNull() ?: return@launch
            _uiState.update { it.copy(catalogFilters = catalogFilters) }
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

            is RecipeListAction.CatalogFilterGroupClicked -> _uiState.update {
                it.copy(
                    expandedCatalogFilterGroup = if (it.expandedCatalogFilterGroup == action.group) null else action.group,
                )
            }

            is RecipeListAction.CatalogFilterOptionClicked -> {
                _uiState.update { state ->
                    state.copy(catalogFilterSelection = state.catalogFilterSelection.toggle(action.group, action.option))
                }
                launch("filterRecipes") { load(session.products.value) }
            }

            is RecipeListAction.RecipeClicked -> router.openRecipe(action.id, session.products.value)
            RecipeListAction.BackClicked -> router.goBack()
            RecipeListAction.RetryClicked -> launch("retryRecipes") { load(session.products.value) }
            RecipeListAction.LoadNextPage -> loadNextPage()
        }
    }

    private suspend fun load(ids: List<com.d1onix.dishlab.domain.model.ProductId>) {
        _uiState.update { it.copy(isLoading = true, loadError = false) }
        try {
            val selectedFilters = _uiState.value.catalogFilterSelection
            val result = if (ids.isEmpty()) null else getRecipesForProducts(ids, page = 1, pageSize = PAGE_SIZE, filters = selectedFilters)
            val recipes = result?.items.orEmpty()
            val products = getProducts(recipes.flatMap { it.productIds }.distinct())
            _uiState.update {
                it.copy(
                    all = recipes,
                    products = products.associateBy { product -> product.id },
                    isLoading = false,
                    loadError = false,
                    hasNextPage = result?.hasNextPage == true,
                    isLoadingMore = false,
                ).refiltered()
            }
        } catch (_: Throwable) {
            _uiState.update {
                it.copy(all = emptyList(), visible = emptyList(), isLoading = false, loadError = true)
            }
        }
    }

    private fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) return
        launch("loadMoreRecipes") {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val page = getRecipesForProducts(
                    session.products.value,
                    state.all.size / PAGE_SIZE + 1,
                    PAGE_SIZE,
                    state.catalogFilterSelection,
                )
                val all = (state.all + page.items).distinctBy { it.id }
                val products = getProducts(page.items.flatMap { it.productIds }.distinct())
                _uiState.update {
                    it.copy(
                        all = all,
                        products = it.products + products.associateBy { product -> product.id },
                        hasNextPage = page.hasNextPage,
                        isLoadingMore = false,
                    ).refiltered()
                }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun RecipeListUiState.refiltered(): RecipeListUiState =
        copy(visible = filterRecipes(all, filters))

    private companion object { const val PAGE_SIZE = 20 }
}
