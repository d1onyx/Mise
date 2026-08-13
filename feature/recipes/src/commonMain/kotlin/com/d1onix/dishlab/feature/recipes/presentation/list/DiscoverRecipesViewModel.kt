package com.d1onix.dishlab.feature.recipes.presentation.list

import com.d1onix.dishlab.domain.FilterRecipesUseCase
import com.d1onix.dishlab.domain.GetAllRecipesUseCase
import com.d1onix.dishlab.domain.GetRecipeCatalogFiltersUseCase
import com.d1onix.dishlab.domain.GetProductsUseCase
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Inject
class DiscoverRecipesViewModel(
    dependencies: CommonDependencies,
    private val getAllRecipes: GetAllRecipesUseCase,
    private val getCatalogFilters: GetRecipeCatalogFiltersUseCase,
    private val filterRecipes: FilterRecipesUseCase,
    private val getProducts: GetProductsUseCase,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithMviState<RecipeListUiState> {
    private val mutableState = MutableStateFlow(RecipeListUiState(isLoading = true))
    val uiState: StateFlow<RecipeListUiState> = mutableState.asStateFlow()

    init {
        loadFirstPage()
    }

    private fun loadFirstPage() = launch("loadRecipeCatalogue") {
        try {
            val category = mutableState.value.filters.categories.singleOrNull()
            val page = getAllRecipes(1, PAGE_SIZE, category)
            val catalogCategories = runCatching { getCatalogFilters().categories }.getOrDefault(emptyList())
            val recipes = page.items
            val products = getProducts(recipes.flatMap { it.productIds }.distinct())
            mutableState.update {
                it.copy(
                    all = recipes,
                    products = products.associateBy { product -> product.id },
                    hasNextPage = page.hasNextPage,
                    catalogCategories = catalogCategories.ifEmpty { it.catalogCategories },
                    isLoading = false,
                    loadError = false,
                )
                    .refiltered()
            }
        } catch (_: Throwable) {
            mutableState.update { it.copy(isLoading = false, loadError = true) }
        }
    }

    fun onAction(action: RecipeListAction) {
        when (action) {
            is RecipeListAction.QueryChanged -> mutableState.update {
                it.copy(filters = it.filters.copy(query = action.value)).refiltered()
            }
            is RecipeListAction.GroupClicked -> mutableState.update {
                it.copy(expandedGroup = if (it.expandedGroup == action.group) null else action.group)
            }
            is RecipeListAction.OptionClicked -> when (action.group) {
                FilterGroupId.Category -> {
                    mutableState.update { state ->
                        state.copy(
                            filters = state.filters.copy(
                                categories = if (action.option in state.filters.categories) emptySet() else setOf(action.option),
                            ),
                        ).refiltered()
                    }
                    loadFirstPage()
                }
                else -> mutableState.update {
                    it.copy(filters = it.filters.toggle(action.group, action.option)).refiltered()
                }
            }
            is RecipeListAction.RecipeClicked -> router.openRecipe(action.id)
            RecipeListAction.BackClicked -> router.goBack()
            RecipeListAction.RetryClicked -> loadFirstPage()
            RecipeListAction.LoadNextPage -> loadNextPage()
        }
    }

    private fun RecipeListUiState.refiltered(): RecipeListUiState =
        copy(visible = filterRecipes(all, filters))

    private fun loadNextPage() = launch("loadMoreCatalogueRecipes") {
        val state = mutableState.value
        if (state.isLoadingMore || !state.hasNextPage) return@launch
        mutableState.update { it.copy(isLoadingMore = true) }
        val page = getAllRecipes(
            state.all.size / PAGE_SIZE + 1,
            PAGE_SIZE,
            state.filters.categories.singleOrNull(),
        )
        mutableState.update {
            it.copy(
                all = (it.all + page.items).distinctBy { recipe -> recipe.id },
                hasNextPage = page.hasNextPage,
                isLoadingMore = false,
            ).refiltered()
        }
    }

    private companion object { const val PAGE_SIZE = 20 }
}
