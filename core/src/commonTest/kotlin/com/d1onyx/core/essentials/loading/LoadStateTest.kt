package com.d1onyx.core.essentials.loading

import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadStateTest {

    @AfterTest
    fun tearDown() {
        Logger.reset()
    }

    @Test
    fun `map transforms only the success value`() {
        assertEquals(LoadState.Success(4), LoadState.Success(2).map { it * 2 })
        assertEquals(LoadState.Loading, LoadState.Loading.map { it })

        val failure = LoadState.Failure(IllegalStateException("boom"))
        assertEquals(failure, failure.map { it })
    }

    @Test
    fun `map preserves the stale flag`() {
        val mapped = LoadState.Success(2, isStale = true).map { it * 2 }

        assertEquals(LoadState.Success(4, isStale = true), mapped)
    }

    @Test
    fun `getOrNull returns a value only on success`() {
        assertEquals(2, LoadState.Success(2).getOrNull())
        assertNull(LoadState.Loading.getOrNull())
        assertNull(LoadState.Failure(IllegalStateException()).getOrNull())
    }

    @Test
    fun `asLoadState emits loading before the value`() = runTest {
        val states = flow { emit(7) }.asLoadState("Test").toList()

        assertEquals(listOf(LoadState.Loading, LoadState.Success(7)), states)
    }

    @Test
    fun `asLoadState converts a thrown exception into a failure and logs it`() = runTest {
        val sink = RecordingLogSink()
        Logger.install(DefaultLogger(sink))
        val failure = IllegalStateException("boom")

        val states = flow<Int> { throw failure }.asLoadState("Test").toList()

        assertEquals(LoadState.Loading, states.first())
        assertEquals(failure, (states.last() as LoadState.Failure).exception)
        assertTrue(sink.records.any { it.level == LogLevel.Error && it.tag == "Test" })
    }
}
