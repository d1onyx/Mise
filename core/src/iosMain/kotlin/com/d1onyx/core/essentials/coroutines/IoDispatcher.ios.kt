package com.d1onyx.core.essentials.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native has no Dispatchers.IO. Its file and network APIs are not
// thread-blocking in the way the JVM's are, so Default is the right pool.
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
