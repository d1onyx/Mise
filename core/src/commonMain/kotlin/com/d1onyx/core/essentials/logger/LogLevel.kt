package com.d1onyx.core.essentials.logger

/**
 * Severity of a log record, ordered from the most verbose to the most severe.
 *
 * [priority] is used by sinks to filter records out, so a sink configured with
 * [LogLevel.Info] drops everything below it without ever building the message.
 */
public enum class LogLevel(public val priority: Int) {
    Verbose(2),
    Debug(3),
    Info(4),
    Warn(5),
    Error(6),
}
