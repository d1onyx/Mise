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
    return MainViewController("http://127.0.0.1:8080/")
}

/** Allows the iOS host to inject a LAN or hosted API URL when needed. */
fun MainViewController(apiBaseUrl: String): platform.UIKit.UIViewController {
    Logger.install(DefaultLogger(platformLogSink()))
    val graph = createGraphFactory<IosAppGraph.Factory>().create(
        BackendRuntimeConfig(
            baseUrl = apiBaseUrl,
            isDebug = true,
            developmentToken = ":dishlab-mobile",
        ),
    )
    return ComposeUIViewController { App(graph) }
}
