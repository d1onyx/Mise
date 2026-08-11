package com.d1onix.dishlab.feature.recipes.presentation.list

import com.d1onix.dishlab.domain.FilterRecipesUseCase
import com.d1onix.dishlab.domain.GetAllRecipesUseCase
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
    private val filterRecipes: FilterRecipesUseCase,
    private val getProducts: GetProductsUseCase,
    private val router: RecipesRouter,
) : AbstractViewModel(dependencies), WithMviState<RecipeListUiState> {
    private val mutableState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = mutableState.asStateFlow()

    init {
        launch("loadRecipeCatalogue") {
            val page = getAllRecipes(1, PAGE_SIZE)
            val recipes = page.items
            val products = getProducts(recipes.flatMap { it.productIds }.distinct())
            mutableState.update {
                it.copy(
                    all = recipes,
                    products = products.associateBy { product -> product.id },
                    hasNextPage = page.hasNextPage,
                )
                    .refiltered()
            }
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
            is RecipeListAction.OptionClicked -> mutableState.update {
                it.copy(filters = it.filters.toggle(action.group, action.option)).refiltered()
            }
            is RecipeListAction.RecipeClicked -> router.openRecipe(action.id)
            RecipeListAction.BackClicked -> router.goBack()
            RecipeListAction.RetryClicked -> Unit
            RecipeListAction.LoadNextPage -> loadNextPage()
        }
    }

    private fun RecipeListUiState.refiltered(): RecipeListUiState =
        copy(visible = filterRecipes(all, filters))

    private fun loadNextPage() = launch("loadMoreCatalogueRecipes") {
        val state = mutableState.value
        if (state.isLoadingMore || !state.hasNextPage) return@launch
        mutableState.update { it.copy(isLoadingMore = true) }
        val page = getAllRecipes(state.all.size / PAGE_SIZE + 1, PAGE_SIZE)
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
