package com.d1onyx.core.essentials.logger

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

/**
 * Run [block], logging its start, its outcome and how long it took.
 *
 * This is the building block that makes every feature traceable without
 * hand-writing three log calls per operation:
 *
 * ```
 * override suspend fun login(credentials: Credentials): Session =
 *     logged("login") {
 *         api.login(credentials).toSession()
 *     }
 * ```
 *
 * produces
 *
 * ```
 * D/LoginRepositoryImpl: → login
 * D/LoginRepositoryImpl: ← login (142ms)
 * ```
 *
 * Cancellation is re-thrown without being logged as an error — a cancelled
 * coroutine is normal control flow, and treating it as a failure is the most
 * common way these traces turn into noise.
 *
 * @param operation name shown in the trace
 * @param level level used for the start and success records; failures always log at [LogLevel.Error]
 */
public suspend fun <T> Loggable.logged(
    operation: String,
    level: LogLevel = LogLevel.Debug,
    block: suspend () -> T,
): T {
    logger.log(level, logTag, null) { "→ $operation" }
    val startMark = TimeSource.Monotonic.markNow()
    return try {
        val result = block()
        logger.log(level, logTag, null) { "← $operation (${startMark.elapsedNow().inWholeMilliseconds}ms)" }
        result
    } catch (exception: CancellationException) {
        logger.log(level, logTag, null) { "✕ $operation cancelled (${startMark.elapsedNow().inWholeMilliseconds}ms)" }
        throw exception
    } catch (exception: Throwable) {
        logger.log(LogLevel.Error, logTag, exception) {
            "✕ $operation failed (${startMark.elapsedNow().inWholeMilliseconds}ms)"
        }
        throw exception
    }
}

/**
 * Blocking counterpart of [logged], for non-suspending operations.
 */
public inline fun <T> Loggable.loggedBlocking(
    operation: String,
    level: LogLevel = LogLevel.Debug,
    block: () -> T,
): T {
    logger.log(level, logTag, null) { "→ $operation" }
    val startMark = TimeSource.Monotonic.markNow()
    return try {
        val result = block()
        logger.log(level, logTag, null) { "← $operation (${startMark.elapsedNow().inWholeMilliseconds}ms)" }
        result
    } catch (exception: Throwable) {
        logger.log(LogLevel.Error, logTag, exception) {
            "✕ $operation failed (${startMark.elapsedNow().inWholeMilliseconds}ms)"
        }
        throw exception
    }
}
