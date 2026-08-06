package com.d1onix.dishlab.feature.scanner.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
enum class ScanTarget { Graph, Comparison }

@Serializable
data class ScanRoute(val target: ScanTarget = ScanTarget.Graph) : Route

@Serializable
data class ScanNotFoundRoute(val barcode: String, val target: ScanTarget = ScanTarget.Graph) : Route

interface ScannerRouter {
    /** A product was recognised and added to the session. */
    fun openCombinationGraph()
    fun openNotFound(barcode: String, target: ScanTarget)
    /** Back to the viewfinder, without stacking another «not found» behind it. */
    fun openScanner(target: ScanTarget = ScanTarget.Graph)
    fun openComparison()
    fun openAuth()
    fun openHome()
    fun openRecipes()
    fun goBack()
}
