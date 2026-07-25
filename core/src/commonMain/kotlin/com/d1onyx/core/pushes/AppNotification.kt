package com.d1onyx.core.pushes

/**
 * A notification to display to the user.
 *
 * @param id identifies the notification for replacement and dismissal. Posting
 * twice with the same id updates the existing notification instead of stacking
 * a second one — use a stable id per conversation, per download, and so on.
 * @param payload data delivered back to the app when the user taps the
 * notification. Both FCM and APNs model this as string key-values, so the app
 * maps it to its own route rather than the core doing it.
 */
public data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val channelId: String,
    val payload: Map<String, String> = emptyMap(),
)

/**
 * How loudly a channel announces its notifications.
 *
 * Maps to `NotificationManager.IMPORTANCE_*` on Android. iOS has no equivalent
 * concept — the value is ignored there, and interruption is governed by the
 * authorization the user granted.
 */
public enum class NotificationImportance {
    Low,
    Default,
    High,
}

/**
 * A group of notifications the user can configure together.
 *
 * Android requires every notification to belong to a channel, and a channel's
 * name and description are user-visible — so they must be localized strings
 * resolved by the caller, not hardcoded here.
 *
 * On iOS this maps to a notification category and is largely advisory.
 */
public data class NotificationChannelSpec(
    val id: String,
    val name: String,
    val description: String,
    val importance: NotificationImportance = NotificationImportance.Default,
)
