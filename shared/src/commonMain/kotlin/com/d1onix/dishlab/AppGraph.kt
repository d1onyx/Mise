package com.d1onix.dishlab

import com.d1onix.dishlab.data.demo.LegacyDemoProductsCleaner
import com.d1onix.dishlab.feature.home.presentation.onboarding.OnboardingViewModel
import com.d1onix.dishlab.feature.home.presentation.settings.SettingsViewModel
import com.d1onix.dishlab.feature.products.presentation.graph.GraphViewModel
import com.d1onix.dishlab.feature.products.presentation.comparison.ComparisonViewModel
import com.d1onix.dishlab.feature.products.presentation.history.HistoryViewModel
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewViewModel
import com.d1onix.dishlab.feature.recipes.presentation.detail.RecipeDetailViewModel
import com.d1onix.dishlab.feature.recipes.presentation.list.RecipesViewModel
import com.d1onix.dishlab.feature.recipes.presentation.list.DiscoverRecipesViewModel
import com.d1onix.dishlab.feature.recipes.presentation.list.SavedRecipesViewModel
import com.d1onix.dishlab.feature.scanner.presentation.ScanNotFoundViewModel
import com.d1onix.dishlab.feature.scanner.presentation.ScanViewModel
import com.d1onix.dishlab.domain.repository.ScanSessionStore
import com.d1onyx.navigation.AppRouterHolder
import com.d1onyx.navigation.dialogs.ComposeDialogs

/**
 * What the UI layer is allowed to pull out of the object graph.
 *
 * The graph itself is created once per platform composition root and never
 * travels further than [AppNavHost] — screens receive view-models, not the graph.
 *
 * View-model accessors are unscoped, so each `viewModel { }` call gets a fresh
 * instance owned by its navigation entry.
 */
interface AppGraph {

    val routerHolder: AppRouterHolder
    val dialogs: ComposeDialogs
    val scanSessionStore: ScanSessionStore

    /** Removes legacy bundled product IDs after the app switches to the API. */
    val legacyDemoProductsCleaner: LegacyDemoProductsCleaner

    val settingsViewModel: SettingsViewModel
    val onboardingViewModelFactory: OnboardingViewModel.Factory
    val scanViewModelFactory: ScanViewModel.Factory
    val graphViewModel: GraphViewModel
    val comparisonViewModel: ComparisonViewModel
    val connectionOverviewViewModel: ConnectionOverviewViewModel
    val historyViewModel: HistoryViewModel
    val recipesViewModel: RecipesViewModel
    val discoverRecipesViewModel: DiscoverRecipesViewModel
    val savedRecipesViewModel: SavedRecipesViewModel

    // Screens with an argument build their view-model through an assisted factory.
    val scanNotFoundViewModelFactory: ScanNotFoundViewModel.Factory
    val recipeDetailViewModelFactory: RecipeDetailViewModel.Factory
}
