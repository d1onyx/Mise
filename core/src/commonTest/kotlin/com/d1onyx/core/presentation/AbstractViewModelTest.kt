@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.d1onyx.core.presentation

import androidx.lifecycle.ViewModelStore
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.base.AbstractViewModel
import com.d1onyx.core.presentation.base.getMixinState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AbstractViewModelTest {

    private val sink = RecordingLogSink()

    // No DI container is started: an @Inject class is just a class, so tests
    // build it directly with fakes.
    private val dependencies = CommonDependencies(
        logger = DefaultLogger(sink),
        exceptionHandler = ExceptionHandler { },
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testViewModel(onInit: suspend () -> Unit = {}) =
        TestViewModel(dependencies, onInit)

    @Test
    fun `logs creation under the concrete class name`() = runTest {
        testViewModel()

        val record = sink.records.first()
        assertEquals("TestViewModel", record.tag)
        assertEquals("created", record.message)
    }

    @Test
    fun `logs the cleared callback when the store is cleared`() = runTest {
        val store = ViewModelStore()
        store.put("key", testViewModel())

        store.clear()

        assertTrue(sink.records.any { it.message == "cleared" })
    }

    @Test
    fun `runs onInitialized only after the subclass constructor finished`() = runTest {
        val viewModel = testViewModel()

        // Not yet: the callback is dispatched, never inline.
        assertEquals(0, viewModel.initCount)
        assertTrue(viewModel.constructorFinished, "constructor must complete first")

        advanceUntilIdle()

        assertEquals(1, viewModel.initCount)
    }

    @Test
    fun `traces onInitialized as a logged operation`() = runTest {
        testViewModel()

        advanceUntilIdle()

        assertTrue(sink.records.any { it.message.startsWith("→ onInitialized") })
        assertTrue(sink.records.any { it.message.startsWith("← onInitialized") })
    }

    @Test
    fun `logs a failing onInitialized at error level`() = runTest {
        val failure = IllegalStateException("boom")

        testViewModel(onInit = { throw failure })
        advanceUntilIdle()

        val errorRecord = sink.records.single { it.level == LogLevel.Error }
        assertTrue(errorRecord.message.startsWith("✕ onInitialized failed"))
        assertSame(failure, errorRecord.throwable)
    }

    @Test
    fun `getMixinState returns the same instance for repeated calls`() = runTest {
        val viewModel = testViewModel()

        val first = viewModel.getMixinState { StringBuilder() }
        val second = viewModel.getMixinState { StringBuilder() }

        assertSame(first, second)
    }

    private class TestViewModel(
        dependencies: CommonDependencies,
        private val onInit: suspend () -> Unit,
    ) : AbstractViewModel(dependencies), WithInitCallback {

        var initCount: Int = 0
        val constructorFinished: Boolean

        init {
            constructorFinished = true
        }

        override suspend fun onInitialized() {
            initCount++
            onInit()
        }
    }
}
