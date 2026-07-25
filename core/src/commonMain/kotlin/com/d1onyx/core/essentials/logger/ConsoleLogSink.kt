package com.d1onyx.core.essentials.logger

/**
 * A [LogSink] that prints into stdout. Works on every target, which makes it
 * the right default for unit tests and for pure-Kotlin modules that run
 * outside an app process.
 */
public object ConsoleLogSink : LogSink {

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        println("${level.shortName}/$tag: $message")
        throwable?.printStackTrace()
    }
}

internal val LogLevel.shortName: String
    get() = when (this) {
        LogLevel.Verbose -> "V"
        LogLevel.Debug -> "D"
        LogLevel.Info -> "I"
        LogLevel.Warn -> "W"
        LogLevel.Error -> "E"
    }
