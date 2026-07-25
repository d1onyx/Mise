package com.d1onyx.core.network.engine

import io.ktor.client.engine.HttpClientEngine

/**
 * The HTTP engine of the current platform: OkHttp on Android, Darwin (NSURLSession)
 * on iOS.
 *
 * Tests override it by passing Ktor's `MockEngine` to
 * [com.d1onyx.core.network.createHttpClient] instead.
 */
public expect fun platformHttpClientEngine(): HttpClientEngine
