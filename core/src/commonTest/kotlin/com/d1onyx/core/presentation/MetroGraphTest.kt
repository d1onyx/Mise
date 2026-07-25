@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.d1onyx.core.presentation

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.exceptions.ExceptionHandler
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that a view-model can actually be built by a Metro graph — the
 * annotations compiling is not the same as the graph resolving.
 */
class MetroGraphTest {

    private val sink = RecordingLogSink()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `builds a view-model with its common dependencies from the graph`() = runTest {
        val graph = createGraphFactory<TestAppGraph.Factory>()
            .create(DefaultLogger(sink), ExceptionHandler { })

        val viewModel = graph.viewModel

        assertTrue(sink.records.any { it.message == "created" })
        assertEquals("GraphTestViewModel", sink.records.first().tag)
        assertEquals(viewModel.logTag, "GraphTestViewModel")
    }

    @DependencyGraph(AppScope::class)
    interface TestAppGraph {

        val viewModel: GraphTestViewModel

        @DependencyGraph.Factory
        fun interface Factory {
            fun create(
                @Provides logger: Logger,
                @Provides exceptionHandler: ExceptionHandler,
            ): TestAppGraph
        }
    }

    @Inject
    class GraphTestViewModel(
        dependencies: CommonDependencies,
    ) : AbstractViewModel(dependencies)
}
