package com.d1onyx.core.essentials.logger

/**
 * A [LogSink] that keeps every record in memory.
 *
 * Ships in main rather than in test source so that feature modules can assert
 * on their own logging without each one re-implementing this:
 *
 * ```
 * val sink = RecordingLogSink()
 * Logger.install(DefaultLogger(sink))
 * // ...
 * assertTrue(sink.records.any { it.level == LogLevel.Error })
 * ```
 */
public class RecordingLogSink(
    private val minLevel: LogLevel = LogLevel.Verbose,
) : LogSink {

    private val mutableRecords = mutableListOf<Record>()

    /**
     * Everything recorded so far, oldest first.
     */
    public val records: List<Record> get() = mutableRecords.toList()

    override fun isLoggable(level: LogLevel, tag: String): Boolean =
        level.priority >= minLevel.priority

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        mutableRecords += Record(level, tag, message, throwable)
    }

    public fun clear() {
        mutableRecords.clear()
    }

    public data class Record(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
    )
}
