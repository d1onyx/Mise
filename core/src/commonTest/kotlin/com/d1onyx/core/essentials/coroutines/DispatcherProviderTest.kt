@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.d1onyx.core.essentials.coroutines

import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DispatcherProviderTest {

    @Test
    fun `default provider exposes a real io dispatcher on every platform`() {
        // The point of the expect/actual: this resolves on iOS too, where
        // Dispatchers.IO does not exist.
        val provider = DefaultDispatcherProvider

        assertSame(provider.io, provider.io, "io must be stable across reads")
    }

    @Test
    fun `test provider routes every dispatcher to the given one`() {
        val dispatcher = StandardTestDispatcher()
        val provider = TestDispatcherProvider(dispatcher)

        assertSame(dispatcher, provider.main)
        assertSame(dispatcher, provider.default)
        assertSame(dispatcher, provider.io)
    }

    @Test
    fun `work dispatched through the test provider runs on the test scheduler`() = runTest {
        val provider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        var ran = false

        withContext(provider.io) { ran = true }

        assertEquals(true, ran)
    }
}
