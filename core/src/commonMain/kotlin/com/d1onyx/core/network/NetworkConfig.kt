package com.d1onyx.core.network

import com.d1onyx.core.essentials.AppBuildConfigValues
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Basic network configuration, supplied by the host application through DI.
 */
public data class NetworkConfig(
    val baseUrl: String,
    override val isDebug: Boolean,
    val timeout: Duration = 10.seconds,
    val longPollTimeout: Duration = 30.seconds,
    val longPollRetryTimeout: Duration = 5.seconds,
) : AppBuildConfigValues
