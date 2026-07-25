package com.d1onyx.core.essentials.logger

import android.util.Log

public actual fun platformLogSink(): LogSink = AndroidLogSink

/**
 * Writes records into LogCat.
 *
 * Tags are truncated to 23 characters: `Log.isLoggable` throws on longer tags
 * on API levels below 26, and a core module cannot assume the minSdk of its host.
 */
public object AndroidLogSink : LogSink {

    private const val MAX_TAG_LENGTH = 23

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val safeTag = if (tag.length <= MAX_TAG_LENGTH) tag else tag.substring(0, MAX_TAG_LENGTH)
        when (level) {
            LogLevel.Verbose -> Log.v(safeTag, message, throwable)
            LogLevel.Debug -> Log.d(safeTag, message, throwable)
            LogLevel.Info -> Log.i(safeTag, message, throwable)
            LogLevel.Warn -> Log.w(safeTag, message, throwable)
            LogLevel.Error -> Log.e(safeTag, message, throwable)
        }
    }
}
