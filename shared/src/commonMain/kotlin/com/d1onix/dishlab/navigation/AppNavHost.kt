package com.d1onix.dishlab.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.d1onix.dishlab.AppGraph
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.feature.home.navigation.HomeRoute
import com.d1onix.dishlab.feature.home.navigation.AuthRoute
import com.d1onix.dishlab.feature.home.navigation.OnboardingRoute
import com.d1onix.dishlab.feature.home.navigation.ProfileRoute
import com.d1onix.dishlab.feature.home.presentation.HomeScreen
import com.d1onix.dishlab.feature.home.presentation.auth.AuthScreen
import com.d1onix.dishlab.feature.home.presentation.onboarding.OnboardingScreen
import com.d1onix.dishlab.feature.home.presentation.profile.ProfileScreen
import com.d1onix.dishlab.feature.products.navigation.GraphRoute
import com.d1onix.dishlab.feature.products.navigation.ComparisonRoute
import com.d1onix.dishlab.feature.products.navigation.HistoryRoute
import com.d1onix.dishlab.feature.products.navigation.ConnectionOverviewRoute
import com.d1onix.dishlab.feature.products.presentation.graph.GraphScreen
import com.d1onix.dishlab.feature.products.presentation.comparison.ComparisonScreen
import com.d1onix.dishlab.feature.products.presentation.history.HistoryScreen
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewScreen
import com.d1onix.dishlab.feature.recipes.navigation.CookingRoute
import com.d1onix.dishlab.feature.recipes.navigation.DiscoverRecipesRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipeDetailRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRoute
import com.d1onix.dishlab.feature.recipes.navigation.SavedRoute
import com.d1onix.dishlab.feature.recipes.presentation.cooking.CookingScreen
import com.d1onix.dishlab.feature.recipes.presentation.detail.RecipeDetailScreen
import com.d1onix.dishlab.feature.recipes.presentation.list.RecipesScreen
import com.d1onix.dishlab.feature.recipes.presentation.list.DiscoverRecipesScreen
import com.d1onix.dishlab.feature.recipes.presentation.list.SavedRecipesScreen
import com.d1onix.dishlab.feature.scanner.navigation.ScanNotFoundRoute
import com.d1onix.dishlab.feature.scanner.navigation.ScanRoute
import com.d1onix.dishlab.feature.scanner.presentation.ScanNotFoundScreen
import com.d1onix.dishlab.feature.scanner.presentation.ScanScreen
import com.d1onyx.navigation.LocalAppRouter
import com.d1onyx.navigation.rememberAppRouter

/**
 * The one place that knows the whole app: every route and the view-model behind
 * it. View-models come from `viewModel { }`, so each is owned by its navigation
 * entry and cleared when that entry is popped.
 */
@Composable
fun AppNavHost(graph: AppGraph, onExit: () -> Unit) {
    val navController = rememberNavController()
    val router = rememberAppRouter(navController)

    // Lets injected code (view-models, use cases) navigate through AppRouter.
    DisposableEffect(router) {
        graph.routerHolder.attach(router)
        onDispose { graph.routerHolder.detach() }
    }

    CompositionLocalProvider(LocalAppRouter provides router) {
        NavHost(navController = navController, startDestination = ScanRoute()) {

            composable<HomeRoute> {
                HomeScreen(viewModel { graph.homeViewModel })
            }

            composable<ProfileRoute> {
                ProfileScreen(viewModel { graph.profileViewModel })
            }

            composable<AuthRoute> { entry ->
                val destination = entry.toRoute<AuthRoute>().destination
                AuthScreen(viewModel { graph.authViewModelFactory.create(destination) })
            }

            composable<OnboardingRoute> { entry ->
                val route = entry.toRoute<OnboardingRoute>()
                OnboardingScreen(
                    viewModel {
                        graph.onboardingViewModelFactory.create(route.showIntro, route.destination)
                    },
                )
            }

            composable<ScanRoute> { entry ->
                val target = entry.toRoute<ScanRoute>().target
                ScanScreen(viewModel { graph.scanViewModelFactory.create(target) })
            }

            composable<ScanNotFoundRoute> { entry ->
                val route = entry.toRoute<ScanNotFoundRoute>()
                ScanNotFoundScreen(
                    viewModel { graph.scanNotFoundViewModelFactory.create(route.barcode, route.target) },
                )
            }

            composable<GraphRoute> {
                GraphScreen(viewModel { graph.graphViewModel })
            }

            composable<ComparisonRoute> {
                ComparisonScreen(viewModel { graph.comparisonViewModel })
            }

            composable<ConnectionOverviewRoute> {
                ConnectionOverviewScreen(viewModel { graph.connectionOverviewViewModel })
            }

            composable<HistoryRoute> {
                HistoryScreen(viewModel { graph.historyViewModel })
            }

            composable<RecipesRoute> {
                RecipesScreen(viewModel { graph.recipesViewModel })
            }

            composable<DiscoverRecipesRoute> {
                DiscoverRecipesScreen(viewModel { graph.discoverRecipesViewModel })
            }

            composable<SavedRoute> {
                SavedRecipesScreen(viewModel { graph.savedRecipesViewModel })
            }

            composable<RecipeDetailRoute> { entry ->
                val recipeId = RecipeId(entry.toRoute<RecipeDetailRoute>().recipeId)
                RecipeDetailScreen(
                    viewModel { graph.recipeDetailViewModelFactory.create(recipeId) },
                )
            }

            composable<CookingRoute> { entry ->
                val recipeId = RecipeId(entry.toRoute<CookingRoute>().recipeId)
                CookingScreen(viewModel { graph.cookingViewModelFactory.create(recipeId) })
            }
        }

        PlatformBackHandler {
            handleAppBack(navController::popBackStack, onExit)
        }

        graph.dialogs.Render()
    }
}

internal fun handleAppBack(popBackStack: () -> Boolean, onExit: () -> Unit) {
    if (!popBackStack()) onExit()
}
