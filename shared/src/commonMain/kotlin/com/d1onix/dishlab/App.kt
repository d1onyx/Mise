package com.d1onix.dishlab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.navigation.AppNavHost

/** Root of the shared UI. Both platforms enter here with their own graph. */
@Composable
fun App(graph: AppGraph) {
    LaunchedEffect(graph) {
        graph.legacyDemoProductsCleaner.removeIfNeeded()
    }

    MiseTheme {
        AppNavHost(graph)
    }
}
