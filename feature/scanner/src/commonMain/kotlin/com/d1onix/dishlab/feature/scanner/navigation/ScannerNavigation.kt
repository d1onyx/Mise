package com.d1onix.dishlab.feature.scanner.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object ScanRoute : Route

@Serializable
data class ScanNotFoundRoute(val barcode: String) : Route

interface ScannerRouter {
    /** A product was recognised and added to the session. */
    fun openCombinationGraph()
    fun openNotFound(barcode: String)
    /** Back to the viewfinder, without stacking another «not found» behind it. */
    fun openScanner()
    fun openHome()
    fun goBack()
}
