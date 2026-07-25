package com.d1onyx.core.pushes

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingNotificationControllerTest {

    private val controller = RecordingNotificationController()

    private val messages = NotificationChannelSpec(
        id = "messages",
        name = "Messages",
        description = "New chat messages",
        importance = NotificationImportance.High,
    )

    private fun notification(id: String = "chat-1") = AppNotification(
        id = id,
        title = "New message",
        message = "Ada: hello",
        channelId = messages.id,
        payload = mapOf("chat_id" to "42"),
    )

    @Test
    fun `records a shown notification`() = runTest {
        controller.show(notification())

        assertEquals("New message", controller.shown.single().title)
    }

    @Test
    fun `replaces a notification posted under the same id`() = runTest {
        controller.show(notification(id = "chat-1"))
        controller.show(notification(id = "chat-1").copy(message = "Ada: are you there?"))

        assertEquals(1, controller.shown.size)
        assertEquals("Ada: are you there?", controller.shown.single().message)
    }

    @Test
    fun `keeps notifications with different ids side by side`() = runTest {
        controller.show(notification(id = "chat-1"))
        controller.show(notification(id = "chat-2"))

        assertEquals(2, controller.shown.size)
    }

    @Test
    fun `cancel drops only the requested notification`() = runTest {
        controller.show(notification(id = "chat-1"))
        controller.show(notification(id = "chat-2"))

        controller.cancel("chat-1")

        assertEquals(listOf("chat-2"), controller.shown.map { it.id })
    }

    @Test
    fun `cancelAll clears everything`() = runTest {
        controller.show(notification(id = "chat-1"))
        controller.show(notification(id = "chat-2"))

        controller.cancelAll()

        assertTrue(controller.shown.isEmpty())
    }

    @Test
    fun `ensureChannels registers a channel once`() = runTest {
        controller.ensureChannels(listOf(messages))
        controller.ensureChannels(listOf(messages))

        assertEquals(1, controller.channels.size)
        assertEquals("messages", controller.channels.single().id)
    }

    @Test
    fun `emits the payload of a tapped notification`() = runTest {
        val tapped = async { controller.opened.first() }
        // Taps are deliberately not replayed — a notification tapped before the
        // app subscribed must not resurface later. So the collector has to be
        // running before the tap is simulated.
        runCurrent()

        controller.simulateTap(mapOf("chat_id" to "42"))

        assertEquals(mapOf("chat_id" to "42"), tapped.await())
    }
}
