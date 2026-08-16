package com.d1onix.dishlab.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.d1onix.dishlab.AppGraph
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.feature.home.navigation.OnboardingRoute
import com.d1onix.dishlab.feature.home.navigation.SettingsRoute
import com.d1onix.dishlab.feature.home.presentation.onboarding.OnboardingScreen
import com.d1onix.dishlab.feature.home.presentation.settings.SettingsScreen
import com.d1onix.dishlab.feature.products.navigation.GraphRoute
import com.d1onix.dishlab.feature.products.navigation.ComparisonRoute
import com.d1onix.dishlab.feature.products.navigation.HistoryRoute
import com.d1onix.dishlab.feature.products.navigation.ConnectionOverviewRoute
import com.d1onix.dishlab.feature.products.presentation.graph.GraphScreen
import com.d1onix.dishlab.feature.products.presentation.comparison.ComparisonScreen
import com.d1onix.dishlab.feature.products.presentation.history.HistoryScreen
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewScreen
import com.d1onix.dishlab.feature.recipes.navigation.RecipeDetailRoute
import com.d1onix.dishlab.feature.recipes.navigation.RecipesRoute
import com.d1onix.dishlab.feature.recipes.navigation.DiscoverRecipesRoute
import com.d1onix.dishlab.feature.recipes.navigation.SavedRoute
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
    val startupProducts by graph.scanSessionStore.startupProducts.collectAsStateWithLifecycle()
    if (startupProducts == null) {
        GraphStartupGate()
        return
    }

    val navController = rememberNavController()
    val router = rememberAppRouter(navController)

    // Lets injected code (view-models, use cases) navigate through AppRouter.
    DisposableEffect(router) {
        graph.routerHolder.attach(router)
        onDispose { graph.routerHolder.detach() }
    }

    CompositionLocalProvider(LocalAppRouter provides router) {
        NavHost(
            navController = navController,
            startDestination = if (startupProducts.isNullOrEmpty()) ScanRoute() else GraphRoute,
        ) {

            composable<SettingsRoute> {
                SettingsScreen(viewModel { graph.settingsViewModel })
            }

            composable<OnboardingRoute> { entry ->
                val route = entry.toRoute<OnboardingRoute>()
                OnboardingScreen(
                    viewModel {
                        graph.onboardingViewModelFactory.create(route.showIntro)
                    },
                )
            }

            composable<ScanRoute> { entry ->
                val route = entry.toRoute<ScanRoute>()
                ScanScreen(
                    viewModel = viewModel {
                        graph.scanViewModelFactory.create(
                            route.showBackNavigation,
                            route.addToComparison,
                        )
                    },
                    showBackNavigation = route.showBackNavigation,
                )
            }

            composable<ScanNotFoundRoute> { entry ->
                val route = entry.toRoute<ScanNotFoundRoute>()
                ScanNotFoundScreen(
                    viewModel {
                        graph.scanNotFoundViewModelFactory.create(
                            route.barcode,
                            route.showBackNavigation,
                            route.addToComparison,
                        )
                    },
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
                val route = entry.toRoute<RecipeDetailRoute>()
                val recipeId = RecipeId(route.recipeId)
                val productIds = route.productIds.map(::ProductId)
                RecipeDetailScreen(
                    viewModel { graph.recipeDetailViewModelFactory.create(recipeId, productIds) },
                )
            }

        }

        PlatformBackHandler {
            handleAppBack(navController::popBackStack, onExit)
        }

        graph.dialogs.Render()
    }
}

/**
 * Hold navigation until the single nullable Room snapshot becomes available so
 * a persisted graph never flashes the scanner before its destination is known.
 */
@Composable
private fun GraphStartupGate() {
    Box(
        modifier = Modifier.fillMaxSize().background(MiseTheme.colors.backgroundDeep),
    )
}

internal fun handleAppBack(popBackStack: () -> Boolean, onExit: () -> Unit) {
    if (!popBackStack()) onExit()
}
