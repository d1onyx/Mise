package com.d1onyx.navigation

import com.d1onyx.core.essentials.datetime.DateTimeProvider

/**
 * Default window in which a second navigation command is ignored.
 */
public const val DEFAULT_NAVIGATION_DEBOUNCE_MILLIS: Long = 500

/**
 * Drops navigation commands that arrive too soon after the previous one.
 *
 * Without it, a double tap on a list item opens the same screen twice — the
 * click lands before the first navigation has recomposed the screen away.
 *
 * [AppRouter.restart] is deliberately **not** debounced: it is a programmatic
 * flow switch (login, logout), never the direct result of a tap, and dropping
 * it would leave the user in the wrong flow.
 */
public class DebouncingAppRouter(
    private val delegate: AppRouter,
    private val dateTimeProvider: DateTimeProvider = DateTimeProvider,
    private val debouncePeriodMillis: Long = DEFAULT_NAVIGATION_DEBOUNCE_MILLIS,
) : AppRouter {

    private var lastActionTimestampMillis = 0L

    override fun launch(route: Route): Unit = debounced { delegate.launch(route) }

    override fun replace(route: Route): Unit = debounced { delegate.replace(route) }

    override fun goBack(): Unit = debounced { delegate.goBack() }

    override fun restart(route: Route) {
        delegate.restart(route)
    }

    private inline fun debounced(action: () -> Unit) {
        val now = dateTimeProvider.currentTimeMillis()
        if (now - lastActionTimestampMillis > debouncePeriodMillis) {
            lastActionTimestampMillis = now
            action()
        }
    }
}
