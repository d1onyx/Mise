package com.d1onyx.core.essentials.logger

import platform.Foundation.NSLog

public actual fun platformLogSink(): LogSink = IosLogSink

/**
 * Writes records through `NSLog`, so they show up in Xcode and in the device console.
 *
 * The message is passed as a `%s` argument rather than as the format string
 * itself — otherwise a logged value containing `%` would be interpreted as a
 * format specifier and corrupt the output.
 */
public object IosLogSink : LogSink {

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        NSLog("%s/%s: %s", level.shortName, tag, message)
        throwable?.let { NSLog("%s/%s: %s", level.shortName, tag, it.stackTraceToString()) }
    }
}
