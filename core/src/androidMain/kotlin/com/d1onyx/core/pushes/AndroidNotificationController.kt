package com.d1onyx.core.pushes

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.d1onyx.core.essentials.logger.Loggable
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.essentials.logger.logW

/**
 * What the Android notification stack needs and the shared code cannot know.
 *
 * @param mainActivityClass launched when a notification is tapped
 * @param smallIconResId required by Android — a notification without one is
 * silently dropped by the system
 */
public data class AndroidPushConfig(
    val mainActivityClass: Class<out Activity>,
    val smallIconResId: Int,
)

/**
 * Android [NotificationController] backed by `NotificationManager`.
 *
 * The tapped notification's payload is delivered as intent extras; read it with
 * [payloadFromIntent] in the launched activity.
 */
public class AndroidNotificationController(
    private val context: Context,
    private val config: AndroidPushConfig,
    override val logger: Logger = Logger,
) : NotificationController, Loggable {

    override val logTag: String = "Notifications"

    private val notificationManager: NotificationManager?
        get() = context.getSystemService()

    override suspend fun ensureChannels(channels: List<NotificationChannelSpec>) {
        val manager = notificationManager ?: return
        channels.forEach { spec ->
            // Recreating an existing channel is a no-op for user-changed
            // settings, but it does refresh the name and description — which is
            // exactly what a locale change needs.
            manager.createNotificationChannel(
                NotificationChannel(spec.id, spec.name, spec.importance.toPlatform()).apply {
                    description = spec.description
                },
            )
        }
    }

    override suspend fun show(notification: AppNotification) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            logW { "notifications are disabled; dropping '${notification.id}'" }
            return
        }
        val manager = notificationManager ?: return
        val built = NotificationCompat.Builder(context, notification.channelId)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setSmallIcon(config.smallIconResId)
            .setAutoCancel(true)
            .setContentIntent(pendingIntentFor(notification))
            .build()
        manager.notify(notification.id.hashCode(), built)
    }

    override suspend fun cancel(id: String) {
        notificationManager?.cancel(id.hashCode())
    }

    override suspend fun cancelAll() {
        notificationManager?.cancelAll()
    }

    private fun pendingIntentFor(notification: AppNotification): PendingIntent {
        val intent = Intent(context, config.mainActivityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_HAS_PAYLOAD, true)
            notification.payload.forEach { (key, value) -> putExtra(key, value) }
        }
        return PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            // FLAG_IMMUTABLE is mandatory from API 31; UPDATE_CURRENT keeps the
            // extras of the newest notification for a reused id.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal companion object {
        const val EXTRA_HAS_PAYLOAD: String = "com.d1onyx.core.pushes.HAS_PAYLOAD"
    }
}

/**
 * Extract the payload of the notification that launched this intent, or `null`
 * when the intent did not come from a notification.
 *
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     payloadFromIntent(intent)?.let(::handleNotificationTap)
 * }
 * ```
 */
public fun payloadFromIntent(intent: Intent?): Map<String, String>? {
    if (intent == null) return null
    if (!intent.getBooleanExtra(AndroidNotificationController.EXTRA_HAS_PAYLOAD, false)) return null
    val extras = intent.extras ?: return emptyMap()
    return extras.keySet()
        .filter { it != AndroidNotificationController.EXTRA_HAS_PAYLOAD }
        .mapNotNull { key ->
            @Suppress("DEPRECATION")
            (extras.getString(key))?.let { key to it }
        }
        .toMap()
}

private fun NotificationImportance.toPlatform(): Int = when (this) {
    NotificationImportance.Low -> NotificationManager.IMPORTANCE_LOW
    NotificationImportance.Default -> NotificationManager.IMPORTANCE_DEFAULT
    NotificationImportance.High -> NotificationManager.IMPORTANCE_HIGH
}
