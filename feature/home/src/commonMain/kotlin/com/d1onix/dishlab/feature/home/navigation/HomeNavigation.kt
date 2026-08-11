package com.d1onix.dishlab.feature.home.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute : Route

@Serializable
data class OnboardingRoute(
    val showIntro: Boolean = true,
) : Route

/**
 * Everything Home is allowed to navigate to. The implementation lives in the
 * app host — the only module that knows all the routes.
 */
interface SettingsRouter {
    fun openPreferenceSetup()
    fun goBack()
}
