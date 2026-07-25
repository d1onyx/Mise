@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.d1onyx.core.presentation

import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.base.AbstractViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WithMviStateTest {

    private val sink = RecordingLogSink()
    private val handledExceptions = mutableListOf<Throwable>()

    private val dependencies = CommonDependencies(
        logger = DefaultLogger(sink),
        exceptionHandler = ExceptionHandler { handledExceptions += it },
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `raises and lowers the progress flag around an action`() = runTest {
        val viewModel = TestViewModel(dependencies)
        val gate = CompletableDeferred<Unit>()

        viewModel.run("load") { gate.await() }
        advanceUntilIdle()
        assertTrue(viewModel.progressStateFlow.value, "progress must be up while running")

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.progressStateFlow.value, "progress must be down once finished")
    }

    @Test
    fun `traces the action`() = runTest {
        TestViewModel(dependencies).run("load") { }
        advanceUntilIdle()

        assertTrue(sink.records.any { it.message.startsWith("→ load") })
        assertTrue(sink.records.any { it.message.startsWith("← load") })
    }

    @Test
    fun `routes a failure to the exception handler and logs it`() = runTest {
        val failure = IllegalStateException("boom")

        TestViewModel(dependencies).run("load") { throw failure }
        advanceUntilIdle()

        assertSame(failure, handledExceptions.single())
        assertTrue(sink.records.any { it.level == LogLevel.Error && it.throwable === failure })
    }

    @Test
    fun `hides progress after a failure`() = runTest {
        val viewModel = TestViewModel(dependencies)

        viewModel.run("load") { throw IllegalStateException("boom") }
        advanceUntilIdle()

        assertFalse(viewModel.progressStateFlow.value)
    }

    @Test
    fun `keeps progress up on success under the OnError policy`() = runTest {
        val viewModel = TestViewModel(dependencies)

        viewModel.run("load", WithMviState.HideProgressPolicy.OnError) { }
        advanceUntilIdle()

        assertTrue(viewModel.progressStateFlow.value, "OnError must leave the indicator up on success")
    }

    @Test
    fun `does not report cancellation to the exception handler`() = runTest {
        val viewModel = TestViewModel(dependencies)

        viewModel.run("load") { CompletableDeferred<Unit>().await() }
        advanceUntilIdle()
        viewModel.cancelRunningWork()
        advanceUntilIdle()

        assertEquals(emptyList(), handledExceptions)
        assertTrue(sink.records.none { it.level == LogLevel.Error })
    }

    private class TestViewModel(
        dependencies: CommonDependencies,
    ) : AbstractViewModel(dependencies), WithMviState<Unit> {

        fun run(
            operation: String,
            policy: WithMviState.HideProgressPolicy = WithMviState.HideProgressPolicy.OnFinally,
            action: suspend () -> Unit,
        ) = launch(operation, policy, action)

        fun cancelRunningWork() {
            coroutineScope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        }
    }
}
