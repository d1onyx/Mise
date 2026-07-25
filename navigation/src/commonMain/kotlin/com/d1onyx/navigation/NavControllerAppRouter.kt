package com.d1onyx.navigation

import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logD

/**
 * [AppRouter] backed by a `NavHostController` from Compose Navigation.
 *
 * Prefer [rememberAppRouter], which wraps this in [DebouncingAppRouter].
 */
public class NavControllerAppRouter(
    private val navController: NavHostController,
    override val logger: Logger = Logger,
) : AppRouter, Loggable {

    override val logTag: String = "Navigation"

    override fun launch(route: Route) {
        logD { "launch ${route.describe()}" }
        navController.navigate(route)
    }

    override fun restart(route: Route) {
        logD { "restart at ${route.describe()}" }
        navController.navigate(route) {
            // Drop everything, including the previous start destination, so the
            // user cannot press back into the flow they just left.
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun replace(route: Route) {
        logD { "replace with ${route.describe()}" }
        navController.navigate(route) {
            navController.currentBackStackEntry?.destination?.route?.let { current ->
                popUpTo(current) { inclusive = true }
            }
        }
    }

    override fun goBack() {
        logD { "goBack" }
        navController.popBackStack()
    }
}

/**
 * Whether there is anything to go back to — useful for deciding if a screen
 * shows a back button.
 */
public fun NavController.hasBackStackEntries(): Boolean =
    previousBackStackEntry != null

private fun Route.describe(): String = this::class.simpleName ?: "Route"
