package com.d1onyx.navigation

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logW
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * An [AppRouter] that lives in the DI graph and forwards to the real router
 * once the UI has created it.
 *
 * The real router needs a `NavHostController`, which only exists inside a
 * composition — so it cannot be constructed by the graph. This holder bridges
 * the gap: features inject [AppRouter] as usual, and the navigation host
 * attaches the live instance when it starts.
 *
 * ```
 * // in the composition, once
 * val navController = rememberNavController()
 * val router = rememberAppRouter(navController)
 * DisposableEffect(router) {
 *     holder.attach(router)
 *     onDispose { holder.detach() }
 * }
 * ```
 *
 * Commands issued while nothing is attached are dropped with a warning rather
 * than throwing — a background job finishing after the UI is gone must not
 * crash the app.
 *
 * **The holder has to be scoped.** Unscoped, every injection site would get its
 * own instance, and the one the navigation host attaches would not be the one
 * a view-model navigates through — every command would be silently dropped.
 *
 * For a stricter architecture, skip the holder: have the view-model expose a
 * `Flow<Route>` of navigation events and let the UI collect it. That keeps
 * navigation a pure output of the view-model, at the cost of more wiring per
 * screen.
 */
@SingleIn(AppScope::class)
@Inject
public class AppRouterHolder(
    override val logger: Logger = Logger,
) : AppRouter, Loggable {

    override val logTag: String = "Navigation"

    private var delegate: AppRouter? = null

    /**
     * Whether a live router is currently attached.
     */
    public val isAttached: Boolean get() = delegate != null

    public fun attach(router: AppRouter) {
        delegate = router
    }

    public fun detach() {
        delegate = null
    }

    override fun launch(route: Route): Unit = forward("launch") { it.launch(route) }

    override fun restart(route: Route): Unit = forward("restart") { it.restart(route) }

    override fun replace(route: Route): Unit = forward("replace") { it.replace(route) }

    override fun goBack(): Unit = forward("goBack") { it.goBack() }

    private inline fun forward(command: String, action: (AppRouter) -> Unit) {
        val current = delegate
        if (current == null) {
            logW { "no router attached; dropping '$command'" }
            return
        }
        action(current)
    }
}
