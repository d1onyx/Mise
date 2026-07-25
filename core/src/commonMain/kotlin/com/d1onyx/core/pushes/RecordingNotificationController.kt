package com.d1onyx.core.pushes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * An in-memory [NotificationController] that records what it was asked to show.
 *
 * Ships in main rather than in test source so feature modules can assert on
 * their notification behaviour without a platform framework:
 *
 * ```
 * val notifications = RecordingNotificationController()
 * // ...
 * assertEquals("New message", notifications.shown.single().title)
 * ```
 */
public class RecordingNotificationController : NotificationController, NotificationTaps {

    private val mutableShown = mutableListOf<AppNotification>()
    private val mutableChannels = mutableListOf<NotificationChannelSpec>()
    private val tapEvents = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 8)

    /**
     * Notifications currently displayed, oldest first.
     */
    public val shown: List<AppNotification> get() = mutableShown.toList()

    /**
     * Channels registered so far.
     */
    public val channels: List<NotificationChannelSpec> get() = mutableChannels.toList()

    override val opened: Flow<Map<String, String>> = tapEvents

    override suspend fun ensureChannels(channels: List<NotificationChannelSpec>) {
        channels.forEach { channel ->
            if (mutableChannels.none { it.id == channel.id }) mutableChannels += channel
        }
    }

    override suspend fun show(notification: AppNotification) {
        mutableShown.removeAll { it.id == notification.id }
        mutableShown += notification
    }

    override suspend fun cancel(id: String) {
        mutableShown.removeAll { it.id == id }
    }

    override suspend fun cancelAll() {
        mutableShown.clear()
    }

    /**
     * Simulate the user tapping a notification carrying [payload].
     */
    public suspend fun simulateTap(payload: Map<String, String>) {
        tapEvents.emit(payload)
    }
}
