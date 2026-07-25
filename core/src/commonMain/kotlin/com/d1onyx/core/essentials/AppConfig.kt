package com.d1onyx.core.essentials

/**
 * Build-time flags a shared module may need. Provided by the host application
 * through DI; `BuildConfig` is Android-only and does not exist in common code.
 */
public interface AppBuildConfigValues {
    public val isDebug: Boolean
}

/**
 * Fallback [AppBuildConfigValues] used when the host app registers none.
 *
 * Defaults to debug — the original did the same, on the reasoning that a
 * missing configuration most likely means a test or a sample, where verbose
 * behaviour is preferable to silence.
 */
public object DefaultAppBuildConfigValues : AppBuildConfigValues {
    override val isDebug: Boolean = true
}

/**
 * Implement this and register it in the DI graph to run code once on app start.
 */
public interface WithAppLifecycle {

    /**
     * Callback executed on process start.
     */
    public suspend fun onAppInitialized()
}
