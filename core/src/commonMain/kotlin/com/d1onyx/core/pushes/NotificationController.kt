package com.d1onyx.core.pushes

import kotlinx.coroutines.flow.Flow

/**
 * Shows and dismisses local notifications.
 *
 * Features depend on this interface, so a test can substitute
 * [RecordingNotificationController] without any platform framework.
 *
 * Displaying a notification requires the user's permission on both platforms —
 * request it through `PermissionRequester` with `Permission.PostNotifications`
 * from `core:essentials` before calling [show].
 */
public interface NotificationController {

    /**
     * Register [channels], creating any that do not exist yet.
     *
     * Call it on app start, and again after a locale change: Android caches a
     * channel's name and description at creation time, so a channel created
     * under the previous locale keeps showing the old language in system
     * settings until it is recreated.
     */
    public suspend fun ensureChannels(channels: List<NotificationChannelSpec>)

    /**
     * Display [notification], replacing any previous one with the same
     * [AppNotification.id].
     */
    public suspend fun show(notification: AppNotification)

    /**
     * Dismiss the notification with [id], if it is showing.
     */
    public suspend fun cancel(id: String)

    /**
     * Dismiss every notification posted by this app.
     */
    public suspend fun cancelAll()
}

/**
 * Emits the payload of notifications the user tapped.
 *
 * The app collects this and routes accordingly:
 *
 * ```
 * notificationTaps.opened.collect { payload ->
 *     payload["chat_id"]?.let { navigator.openChat(ChatId(it)) }
 * }
 * ```
 *
 * The Android original serialized a whole route object into the intent and
 * revived it with `Class.forName`. That cannot work on Kotlin/Native, which has
 * no such reflection — and it coupled the core to the navigation library. A
 * string map is what both FCM and APNs actually carry anyway.
 */
public interface NotificationTaps {

    public val opened: Flow<Map<String, String>>
}
