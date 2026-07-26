package com.d1onix.dishlab.navigation

import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.feature.home.navigation.HomeRoute
import com.d1onix.dishlab.feature.home.navigation.HomeRouter
import com.d1onix.dishlab.feature.products.navigation.GraphRoute
import com.d1onix.dishlab.feature.products.navigation.HistoryRoute
import com.d1onix.dishlab.feature.products.navigation.ProductsRouter
import com.d1onix.dishlab.feature.products.navigation.ConnectionOverviewRoute
import com.d1onix.dishlab.feature.recipes.navigation.CookingRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipeDetailRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRouter
import com.d1onix.dishlab.feature.recipes.navigation.SavedRoute
import com.d1onix.dishlab.feature.scanner.navigation.ScanNotFoundRoute
import com.d1onix.dishlab.feature.scanner.navigation.ScanRoute
import com.d1onix.dishlab.feature.scanner.navigation.ScannerRouter
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.navigation.AppRouter
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Each feature declares the destinations it may reach; the implementations live
 * here because this is the only module that knows every route.
 */

@ContributesBinding(AppScope::class)
@Inject
class HomeRouterImpl(private val router: AppRouter) : HomeRouter {
    override fun openScanner() = router.launch(ScanRoute)
    override fun openSavedRecipes() = router.launch(SavedRoute)
    override fun openHistory() = router.launch(HistoryRoute)
}

@ContributesBinding(AppScope::class)
@Inject
class ScannerRouterImpl(private val router: AppRouter) : ScannerRouter {
    /** `replace`, so backing out of the graph does not return to the viewfinder. */
    override fun openCombinationGraph() = router.replace(GraphRoute)
    override fun openNotFound(barcode: String) = router.replace(ScanNotFoundRoute(barcode))
    override fun openScanner() = router.replace(ScanRoute)
    override fun openHome() = router.restart(HomeRoute)
    override fun goBack() = router.goBack()
}

@ContributesBinding(AppScope::class)
@Inject
class ProductsRouterImpl(private val router: AppRouter) : ProductsRouter {
    override fun openScanner() = router.launch(ScanRoute)
    override fun openRecipes() = router.launch(RecipesRoute)
    override fun openSavedRecipes() = router.launch(SavedRoute)
    override fun openCombinationGraph() = router.launch(GraphRoute)
    override fun openConnectionOverview() = router.launch(ConnectionOverviewRoute)
    override fun goBack() = router.goBack()
}

@ContributesBinding(AppScope::class)
@Inject
class RecipesRouterImpl(private val router: AppRouter) : RecipesRouter {
    override fun openRecipe(id: RecipeId) = router.launch(RecipeDetailRoute(id.value))
    override fun openCookingMode(id: RecipeId) = router.launch(CookingRoute(id.value))
    override fun goBack() = router.goBack()
}
