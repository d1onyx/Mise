package com.d1onix.dishlab

import android.app.Application
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.platformLogSink

/**
 * The Android composition root: the logger is installed before anything else
 * runs, then the graph is created once for the whole process.
 */
class DishLabApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        Logger.install(DefaultLogger(platformLogSink()))
        graph = createAndroidAppGraph(this)
    }
}
