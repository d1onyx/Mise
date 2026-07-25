package com.d1onyx.navigation

/**
 * Marker for every navigation destination.
 *
 * Routes are `@Serializable` values, which is what makes navigation type-safe —
 * arguments travel as properties instead of as stringly-typed URL segments:
 *
 * ```
 * @Serializable data object HomeRoute : Route
 * @Serializable data class ChatRoute(val chatId: String) : Route
 * ```
 */
public interface Route

/**
 * Navigation commands available to a feature.
 *
 * Features depend on this interface, never on a `NavController`, so a
 * view-model stays testable and the engine underneath can be replaced.
 *
 * Give each feature its own narrow router interface so it cannot navigate to
 * screens it should not know about:
 *
 * ```
 * interface ChatRouter {
 *     fun openProfile(userId: UserId)
 *     fun leaveChat()
 * }
 *
 * @ContributesBinding(AppScope::class)
 * @Inject
 * class ChatRouterImpl(private val router: AppRouter) : ChatRouter {
 *     override fun openProfile(userId: UserId) = router.launch(ProfileRoute(userId.value))
 *     override fun leaveChat() = router.goBack()
 * }
 * ```
 */
public interface AppRouter {

    /**
     * Push [route] onto the back stack.
     */
    public fun launch(route: Route)

    /**
     * Clear the whole back stack and start again at [route].
     *
     * For flow switches — sign-in to main content, or logout.
     */
    public fun restart(route: Route)

    /**
     * Replace the current screen with [route], leaving the rest of the stack
     * untouched. The replaced screen is not restored by a back press.
     */
    public fun replace(route: Route)

    /**
     * Pop the current screen.
     */
    public fun goBack()
}
