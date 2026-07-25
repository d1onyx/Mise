package com.d1onyx.core.network.engine

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

public actual fun platformHttpClientEngine(): HttpClientEngine = Darwin.create()
