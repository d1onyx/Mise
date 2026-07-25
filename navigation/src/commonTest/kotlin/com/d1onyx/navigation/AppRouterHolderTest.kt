package com.d1onyx.navigation

import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.RecordingLogSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppRouterHolderTest {

    private data object HomeRoute : Route

    private class RecordingRouter : AppRouter {
        val commands = mutableListOf<String>()
        override fun launch(route: Route) { commands += "launch" }
        override fun restart(route: Route) { commands += "restart" }
        override fun replace(route: Route) { commands += "replace" }
        override fun goBack() { commands += "goBack" }
    }

    private val sink = RecordingLogSink()
    private val holder = AppRouterHolder(DefaultLogger(sink))

    @Test
    fun `starts detached`() {
        assertFalse(holder.isAttached)
    }

    @Test
    fun `forwards every command once attached`() {
        val router = RecordingRouter()
        holder.attach(router)

        holder.launch(HomeRoute)
        holder.restart(HomeRoute)
        holder.replace(HomeRoute)
        holder.goBack()

        assertEquals(listOf("launch", "restart", "replace", "goBack"), router.commands)
    }

    @Test
    fun `drops commands while detached instead of throwing`() {
        holder.launch(HomeRoute)

        assertTrue(sink.records.any { it.level == LogLevel.Warn })
    }

    @Test
    fun `stops forwarding after detach`() {
        val router = RecordingRouter()
        holder.attach(router)
        holder.detach()

        holder.launch(HomeRoute)

        assertTrue(router.commands.isEmpty())
        assertFalse(holder.isAttached)
    }

    @Test
    fun `re-attaching replaces the previous router`() {
        val first = RecordingRouter()
        val second = RecordingRouter()
        holder.attach(first)
        holder.attach(second)

        holder.launch(HomeRoute)

        assertTrue(first.commands.isEmpty())
        assertEquals(listOf("launch"), second.commands)
    }
}
