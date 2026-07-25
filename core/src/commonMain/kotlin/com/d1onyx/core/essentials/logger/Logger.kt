package com.d1onyx.core.essentials.logger

import kotlin.concurrent.Volatile

/**
 * Universal logger accessible from any location in the code.
 *
 * The message is passed as a lambda so a filtered-out record costs nothing —
 * no string concatenation happens unless a sink actually wants the record.
 *
 * Use it either through injection:
 *
 * ```
 * class LoginRepository(private val logger: Logger)
 * ```
 *
 * or through the global facade, which is what most call sites do:
 *
 * ```
 * Logger.d("Auth") { "session refreshed" }
 * ```
 */
public interface Logger {

    /**
     * Record a log entry. Implementations decide whether to evaluate [message] at all.
     */
    public fun log(
        level: LogLevel,
        tag: String,
        throwable: Throwable? = null,
        message: () -> String,
    )

    /**
     * The global [Logger] facade.
     *
     * Defaults to a console logger, so logging works in unit tests and in
     * pure-Kotlin modules without any setup. Applications replace it once,
     * on startup, via [install].
     */
    public companion object : Logger {

        @Volatile
        private var instance: Logger = DefaultLogger(ConsoleLogSink)

        override fun log(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) {
            instance.log(level, tag, throwable, message)
        }

        /**
         * Replace the global logger. Call once, as early as possible on app start.
         */
        public fun install(logger: Logger) {
            instance = logger
        }

        /**
         * Restore the console logger. Intended for tests.
         */
        public fun reset() {
            instance = DefaultLogger(ConsoleLogSink)
        }
    }
}

/**
 * The standard [Logger], writing everything into a [LogSink].
 *
 * Wire the platform sink on app start:
 *
 * ```
 * Logger.install(DefaultLogger(platformLogSink()))
 * ```
 */
public class DefaultLogger(
    private val sink: LogSink,
) : Logger {

    override fun log(level: LogLevel, tag: String, throwable: Throwable?, message: () -> String) {
        if (!sink.isLoggable(level, tag)) return
        sink.log(level, tag, message(), throwable)
    }
}

public fun Logger.v(tag: String, message: () -> String): Unit =
    log(LogLevel.Verbose, tag, null, message)

public fun Logger.d(tag: String, message: () -> String): Unit =
    log(LogLevel.Debug, tag, null, message)

public fun Logger.i(tag: String, message: () -> String): Unit =
    log(LogLevel.Info, tag, null, message)

public fun Logger.w(tag: String, throwable: Throwable? = null, message: () -> String): Unit =
    log(LogLevel.Warn, tag, throwable, message)

public fun Logger.e(tag: String, throwable: Throwable? = null, message: () -> String): Unit =
    log(LogLevel.Error, tag, throwable, message)
