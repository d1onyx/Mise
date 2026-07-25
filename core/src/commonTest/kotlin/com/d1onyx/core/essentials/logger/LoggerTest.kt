package com.d1onyx.core.essentials.logger

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoggerTest {

    private val sink = RecordingLogSink()

    @BeforeTest
    fun setUp() {
        Logger.install(DefaultLogger(sink))
    }

    @AfterTest
    fun tearDown() {
        Logger.reset()
    }

    @Test
    fun `records level tag and message`() {
        Logger.d("Auth") { "session refreshed" }

        val record = sink.records.single()
        assertEquals(LogLevel.Debug, record.level)
        assertEquals("Auth", record.tag)
        assertEquals("session refreshed", record.message)
    }

    @Test
    fun `attaches throwable to error records`() {
        val failure = IllegalStateException("boom")

        Logger.e("Auth", failure) { "login failed" }

        val record = sink.records.single()
        assertEquals(LogLevel.Error, record.level)
        assertSame(failure, record.throwable)
    }

    @Test
    fun `does not build the message when the sink filters the record out`() {
        Logger.install(DefaultLogger(MinLevelLogSink(sink, LogLevel.Warn)))
        var messageWasBuilt = false

        Logger.d("Auth") {
            messageWasBuilt = true
            "expensive"
        }

        assertFalse(messageWasBuilt, "message lambda must not run for a filtered-out record")
        assertTrue(sink.records.isEmpty())
    }

    @Test
    fun `reset restores the console logger`() {
        Logger.reset()

        Logger.d("Auth") { "after reset" }

        assertTrue(sink.records.isEmpty(), "records must no longer reach the uninstalled sink")
    }
}
