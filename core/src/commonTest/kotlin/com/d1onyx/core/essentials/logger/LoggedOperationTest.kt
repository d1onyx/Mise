package com.d1onyx.core.essentials.logger

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoggedOperationTest {

    private val sink = RecordingLogSink()

    private val subject = object : Loggable {
        override val logTag: String = "Auth"
        override val logger: Logger = DefaultLogger(sink)
    }

    @Test
    fun `logs entry and exit around a successful operation`() = runTest {
        val result = subject.logged("login") { "session" }

        assertEquals("session", result)
        assertEquals(2, sink.records.size)
        assertTrue(sink.records[0].message.startsWith("→ login"))
        assertTrue(sink.records[1].message.startsWith("← login"))
        assertTrue(sink.records.all { it.level == LogLevel.Debug })
    }

    @Test
    fun `logs a failure at error level and rethrows`() = runTest {
        val failure = IllegalStateException("boom")

        val thrown = assertFailsWith<IllegalStateException> {
            subject.logged<Unit>("login") { throw failure }
        }

        assertSame(failure, thrown)
        val errorRecord = sink.records.last()
        assertEquals(LogLevel.Error, errorRecord.level)
        assertTrue(errorRecord.message.startsWith("✕ login failed"))
        assertSame(failure, errorRecord.throwable)
    }

    @Test
    fun `treats cancellation as normal control flow rather than an error`() = runTest {
        val started = CompletableDeferred<Unit>()

        val job = launch {
            subject.logged("login") {
                started.complete(Unit)
                CompletableDeferred<Unit>().await() // never completes
            }
        }
        started.await()
        job.cancel()
        job.join()

        val lastRecord = sink.records.last()
        assertEquals(LogLevel.Debug, lastRecord.level)
        assertTrue(lastRecord.message.startsWith("✕ login cancelled"))
        assertTrue(sink.records.none { it.level == LogLevel.Error })
    }

    @Test
    fun `defaults the tag to the class name`() {
        val defaultTagged = object : Loggable {
            override val logger: Logger = DefaultLogger(sink)
        }

        defaultTagged.logD { "hello" }

        assertEquals("Anonymous", sink.records.single().tag)
    }
}
