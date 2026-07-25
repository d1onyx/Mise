package com.d1onyx.core.essentials.logger

/**
 * A destination of log records: LogCat, NSLog, a crash reporter, a file, a test buffer.
 *
 * Sinks receive an already-evaluated [message]. Filtering happens earlier, in
 * [isLoggable], so a dropped record never pays for message construction.
 */
public interface LogSink {

    /**
     * Whether this sink wants records of [level] tagged with [tag].
     *
     * Called before the message lambda is evaluated. Keep it cheap.
     */
    public fun isLoggable(level: LogLevel, tag: String): Boolean = true

    /**
     * Write a single record. Called only when [isLoggable] returned `true`.
     */
    public fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    )
}

/**
 * A sink that discards everything. Use it as the release-build default
 * when no crash reporter is wired in.
 */
public object NoOpLogSink : LogSink {
    override fun isLoggable(level: LogLevel, tag: String): Boolean = false
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?): Unit = Unit
}

/**
 * A sink that forwards every record to each of the [sinks], in order.
 *
 * Lets a project log to LogCat and a crash reporter at once:
 *
 * ```
 * Logger.install(DefaultLogger(CompositeLogSink(platformLogSink(), crashlyticsSink)))
 * ```
 */
public class CompositeLogSink(
    private val sinks: List<LogSink>,
) : LogSink {

    public constructor(vararg sinks: LogSink) : this(sinks.toList())

    override fun isLoggable(level: LogLevel, tag: String): Boolean =
        sinks.any { it.isLoggable(level, tag) }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        sinks.forEach { sink ->
            if (sink.isLoggable(level, tag)) {
                sink.log(level, tag, message, throwable)
            }
        }
    }
}

/**
 * A sink that raises the bar for an underlying [sink], dropping anything
 * below [minLevel]. Typical release setup keeps [LogLevel.Warn] and above.
 */
public class MinLevelLogSink(
    private val sink: LogSink,
    private val minLevel: LogLevel,
) : LogSink {

    override fun isLoggable(level: LogLevel, tag: String): Boolean =
        level.priority >= minLevel.priority && sink.isLoggable(level, tag)

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        sink.log(level, tag, message, throwable)
    }
}
