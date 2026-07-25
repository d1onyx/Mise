package com.d1onyx.core.network.logging

import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.Logger
import io.ktor.client.plugins.logging.Logger as KtorLogger

internal const val HTTP_LOG_TAG = "Http"

/**
 * Routes Ktor's request/response logging into the app's [Logger], so HTTP
 * traffic lands in the same sinks — and the same crash reporter — as everything
 * else, instead of going straight to stdout.
 */
internal class KtorLoggerBridge(
    private val logger: Logger,
) : KtorLogger {

    override fun log(message: String) {
        logger.log(LogLevel.Debug, HTTP_LOG_TAG, null) { message }
    }
}
