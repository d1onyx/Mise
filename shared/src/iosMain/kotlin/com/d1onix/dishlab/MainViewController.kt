package com.d1onix.dishlab

import androidx.compose.ui.window.ComposeUIViewController
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.platformLogSink
import dev.zacsweers.metro.createGraphFactory

/**
 * The iOS entry point. The graph is created once, here — this is the platform's
 * composition root.
 */
fun MainViewController(): platform.UIKit.UIViewController {
    Logger.install(DefaultLogger(platformLogSink()))
    val graph = createGraphFactory<IosAppGraph.Factory>().create()
    return ComposeUIViewController { App(graph) }
}
