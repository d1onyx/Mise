package com.d1onix.dishlab.feature.scanner.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class ScanRoute(
    /** False only for the scanner that is the app's first destination. */
    val showBackNavigation: Boolean = false,
    /** A recognised product is added to the comparison and returns there immediately. */
    val addToComparison: Boolean = false,
) : Route

@Serializable
data class ScanNotFoundRoute(
    val barcode: String,
    val showBackNavigation: Boolean,
    val addToComparison: Boolean = false,
) : Route

interface ScannerRouter {
    /** A product was recognised and added to the session. */
    fun openCombinationGraph()
    fun openComparison()
    fun openNotFound(barcode: String, showBackNavigation: Boolean, addToComparison: Boolean)
    /** Back to the viewfinder, without stacking another «not found» behind it. */
    fun openScanner(showBackNavigation: Boolean, addToComparison: Boolean)
    fun openRecipes()
    fun goBack()
}
