package com.d1onyx.core.pushes

import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logW
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS [NotificationController] backed by `UNUserNotificationCenter`.
 *
 * Notifications are delivered immediately (no trigger). The payload travels in
 * `userInfo`, where the app's `UNUserNotificationCenterDelegate` reads it on tap.
 *
 * Channels have no iOS equivalent: [ensureChannels] is accepted and ignored, so
 * shared code can call it unconditionally.
 */
public class IosNotificationController(
    private val center: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter(),
    override val logger: Logger = Logger,
) : NotificationController, Loggable {

    override val logTag: String = "Notifications"

    override suspend fun ensureChannels(channels: List<NotificationChannelSpec>) {
        // Intentionally empty — iOS has no notification channels.
    }

    override suspend fun show(notification: AppNotification) {
        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            setBody(notification.message)
            setUserInfo(notification.payload.mapKeys { (key, _) -> key })
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id,
            content = content,
            trigger = null,
        )
        suspendCoroutine { continuation ->
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    logW { "failed to post '${notification.id}': ${error.localizedDescription}" }
                }
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun cancel(id: String) {
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
    }

    override suspend fun cancelAll() {
        center.removeAllDeliveredNotifications()
        center.removeAllPendingNotificationRequests()
    }
}
