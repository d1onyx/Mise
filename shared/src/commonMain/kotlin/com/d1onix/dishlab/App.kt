package com.d1onix.dishlab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.navigation.AppNavHost

/** Root of the shared UI. Both platforms enter here with their own graph. */
@Composable
fun App(graph: AppGraph) {
    // First launch only: fills Saved and History so no screen opens empty.
    LaunchedEffect(graph) {
        graph.demoDataSeeder.seedIfNeeded()
    }

    MiseTheme {
        AppNavHost(graph)
    }
}
