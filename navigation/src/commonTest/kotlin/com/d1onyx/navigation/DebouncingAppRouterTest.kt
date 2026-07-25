package com.d1onyx.navigation

import com.d1onyx.core.essentials.datetime.DateTimeProvider
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DebouncingAppRouterTest {

    private data object HomeRoute : Route
    private data object ChatRoute : Route

    private class RecordingRouter : AppRouter {
        val commands = mutableListOf<String>()
        override fun launch(route: Route) { commands += "launch:${route::class.simpleName}" }
        override fun restart(route: Route) { commands += "restart:${route::class.simpleName}" }
        override fun replace(route: Route) { commands += "replace:${route::class.simpleName}" }
        override fun goBack() { commands += "goBack" }
    }

    /** A clock the test moves by hand. */
    private class MovableClock(var millis: Long = 1_000) : DateTimeProvider {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
        override fun timeZone(): TimeZone = TimeZone.UTC
        override fun currentTimeMillis(): Long = millis
    }

    private val delegate = RecordingRouter()
    private val clock = MovableClock()
    private val router = DebouncingAppRouter(delegate, clock, debouncePeriodMillis = 500)

    @Test
    fun `passes the first command through`() {
        router.launch(HomeRoute)

        assertEquals(listOf("launch:HomeRoute"), delegate.commands)
    }

    @Test
    fun `drops a second command inside the debounce window`() {
        router.launch(HomeRoute)
        clock.millis += 100
        router.launch(ChatRoute)

        assertEquals(listOf("launch:HomeRoute"), delegate.commands)
    }

    @Test
    fun `allows a command once the window has passed`() {
        router.launch(HomeRoute)
        clock.millis += 501
        router.launch(ChatRoute)

        assertEquals(listOf("launch:HomeRoute", "launch:ChatRoute"), delegate.commands)
    }

    @Test
    fun `debounces goBack against a double tap`() {
        router.goBack()
        clock.millis += 50
        router.goBack()

        assertEquals(listOf("goBack"), delegate.commands)
    }

    @Test
    fun `debounces across different command types`() {
        router.launch(HomeRoute)
        clock.millis += 100
        router.goBack()

        assertEquals(listOf("launch:HomeRoute"), delegate.commands)
    }

    @Test
    fun `never debounces restart`() {
        router.restart(HomeRoute)
        router.restart(ChatRoute)

        assertEquals(listOf("restart:HomeRoute", "restart:ChatRoute"), delegate.commands)
    }

    @Test
    fun `restart does not consume the debounce window`() {
        router.restart(HomeRoute)
        router.launch(ChatRoute)

        assertEquals(listOf("restart:HomeRoute", "launch:ChatRoute"), delegate.commands)
    }
}
