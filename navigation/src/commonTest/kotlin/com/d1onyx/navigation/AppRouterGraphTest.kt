package com.d1onyx.navigation

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The holder only works if the graph hands out one instance: the navigation host
 * attaches the live router to the same object a view-model navigates through.
 * Unscoped, every command would be dropped and navigation would appear dead.
 */
class AppRouterGraphTest {

    @Test
    fun `the holder is the same instance everywhere in the graph`() {
        val graph = createGraphFactory<TestGraph.Factory>()
            .create(DefaultLogger(RecordingLogSink()))

        assertSame(graph.holder, graph.router)
        assertSame(graph.holder, graph.consumer.router)
    }

    @Test
    fun `a route issued by an injected consumer reaches the attached router`() {
        val graph = createGraphFactory<TestGraph.Factory>()
            .create(DefaultLogger(RecordingLogSink()))
        val recording = RecordingRouter()

        graph.holder.attach(recording)
        graph.consumer.router.launch(TestRoute)

        assertEquals(listOf<Route>(TestRoute), recording.launched)
        assertTrue(graph.holder.isAttached)
    }

    @DependencyGraph(AppScope::class)
    interface TestGraph {

        val holder: AppRouterHolder
        val router: AppRouter
        val consumer: RouterConsumer

        @DependencyGraph.Factory
        fun interface Factory {
            fun create(@Provides logger: Logger): TestGraph
        }
    }

    /** Stands in for a view-model that injects `AppRouter`. */
    @Inject
    class RouterConsumer(val router: AppRouter)

    private object TestRoute : Route

    private class RecordingRouter : AppRouter {
        val launched = mutableListOf<Route>()
        override fun launch(route: Route) {
            launched += route
        }

        override fun restart(route: Route) = Unit
        override fun replace(route: Route) = Unit
        override fun goBack() = Unit
    }
}
