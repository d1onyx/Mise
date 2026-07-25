package com.d1onix.dishlab.feature.products.navigation

import com.d1onyx.navigation.Route
import kotlinx.serialization.Serializable

/** The combination graph of everything scanned in this session. */
@Serializable
data object GraphRoute : Route

@Serializable
data object HistoryRoute : Route

interface ProductsRouter {
    fun openScanner()
    fun openRecipes()
    fun openSavedRecipes()
    fun openCombinationGraph()
    fun goBack()
}
