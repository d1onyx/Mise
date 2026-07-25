package com.d1onyx.core.network.engine

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

public actual fun platformHttpClientEngine(): HttpClientEngine = OkHttp.create()
