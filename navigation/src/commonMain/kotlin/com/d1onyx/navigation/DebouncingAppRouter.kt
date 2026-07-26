package com.d1onyx.navigation

import com.d1onyx.core.essentials.datetime.DateTimeProvider

/**
 * Default window in which a second navigation command is ignored.
 */
public const val DEFAULT_NAVIGATION_DEBOUNCE_MILLIS: Long = 500

/**
 * Drops repeated navigation commands that arrive too soon after each other.
 *
 * Without it, a double tap on a list item opens the same screen twice — the
 * click lands before the first navigation has recomposed the screen away.
 * A different destination is never blocked: async flows may legitimately move
 * through two screens inside the debounce window.
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
    private var lastCommand: Command? = null

    override fun launch(route: Route): Unit =
        debounced(Command(Action.Launch, route)) { delegate.launch(route) }

    override fun replace(route: Route): Unit =
        debounced(Command(Action.Replace, route)) { delegate.replace(route) }

    override fun goBack(): Unit = debounced(Command(Action.GoBack)) { delegate.goBack() }

    override fun restart(route: Route) {
        delegate.restart(route)
    }

    private inline fun debounced(command: Command, action: () -> Unit) {
        val now = dateTimeProvider.currentTimeMillis()
        val repeatedTooSoon =
            command == lastCommand && now - lastActionTimestampMillis <= debouncePeriodMillis
        if (repeatedTooSoon) return

        lastCommand = command
        lastActionTimestampMillis = now
        action()
    }

    private data class Command(
        val action: Action,
        val route: Route? = null,
    )

    private enum class Action {
        Launch,
        Replace,
        GoBack,
    }
}
