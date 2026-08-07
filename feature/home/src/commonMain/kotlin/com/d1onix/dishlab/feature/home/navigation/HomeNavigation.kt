package com.d1onix.dishlab.feature.home.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : Route

@Serializable
data object ProfileRoute : Route

@Serializable
enum class ProtectedDestination { Previous, Profile, Comparison, Recipes, Saved, History }

@Serializable
data class AuthRoute(val destination: ProtectedDestination = ProtectedDestination.Previous) : Route

@Serializable
data class OnboardingRoute(
    val showIntro: Boolean = true,
    val destination: ProtectedDestination = ProtectedDestination.Previous,
) : Route

/**
 * Everything Home is allowed to navigate to. The implementation lives in the
 * app host — the only module that knows all the routes.
 */
interface HomeRouter {
    fun openScanner()
    fun openSavedRecipes()
    fun openHistory()
    fun openProfile()
    fun openAuth(destination: ProtectedDestination = ProtectedDestination.Previous)
    fun openPostRegistrationOnboarding(destination: ProtectedDestination)
    fun completeProtectedNavigation(destination: ProtectedDestination)
    fun openPreferenceSetup()
    fun openComparison()
    fun openRecipes()
    fun goBack()
}
