package com.d1onyx.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/**
 * Create the router for a `NavHost`, already debounced.
 *
 * ```
 * val navController = rememberNavController()
 * val router = rememberAppRouter(navController)
 * ```
 */
@Composable
public fun rememberAppRouter(
    navController: NavHostController,
): AppRouter = remember(navController) {
    DebouncingAppRouter(NavControllerAppRouter(navController))
}

/**
 * Makes the router reachable from composables that cannot receive it as a
 * parameter.
 *
 * Prefer passing [AppRouter] into a view-model through DI — this local exists
 * for the UI layer, where a composable genuinely has no constructor. Reading it
 * from a repository or a use case turns navigation into hidden global state.
 */
public val LocalAppRouter: ProvidableCompositionLocal<AppRouter> =
    staticCompositionLocalOf { error("LocalAppRouter is not provided") }
