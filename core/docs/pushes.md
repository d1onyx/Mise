# Notifications

Local notifications with a shared API over `NotificationManager` (Android) and
`UNUserNotificationCenter` (iOS).


Covers *displaying* notifications. Receiving remote pushes (FCM / APNs tokens
and message callbacks) stays in the app, which then calls `show`.

## Ask permission first

Both platforms require it. Use `PermissionRequester` from `core`:

```kotlin
val status = permissionRequester.requestPermission(Permission.PostNotifications)
if (status != PermissionStatus.Granted) return
```

Posting without permission is not an error — the Android controller logs a
warning and drops the notification.

## Register channels

Android requires every notification to belong to a channel. Do this on app start:

```kotlin
notifications.ensureChannels(
    listOf(
        NotificationChannelSpec(
            id = "messages",
            name = stringProvider.messagesChannelTitle,
            description = stringProvider.messagesChannelDescription,
            importance = NotificationImportance.High,
        )
    )
)
```

Names and descriptions are user-visible, so pass localized strings.

**Call it again after a locale change.** Android caches the name and description
at creation time; a channel created under the previous locale keeps showing the
old language in system settings until recreated. `ensureChannels` recreates
safely — user-changed settings such as sound are preserved.

On iOS the call is accepted and ignored; channels have no equivalent there.

## Show a notification

```kotlin
notifications.show(
    AppNotification(
        id = "chat-${chat.id}",
        title = chat.senderName,
        message = message.preview,
        channelId = "messages",
        payload = mapOf("chat_id" to chat.id.value),
    )
)
```

`id` identifies the notification: posting again with the same id **replaces** it
rather than stacking a duplicate. Use a stable id per conversation or per
download.

`cancel(id)` dismisses one, `cancelAll()` dismisses everything.

## Handling a tap

The `payload` map comes back when the user taps. It is a string map because that
is exactly what FCM and APNs carry — the app maps it to its own route.

**Android** — read it in the launched activity:

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        payloadFromIntent(intent)?.let(::handleNotificationTap)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        payloadFromIntent(intent)?.let(::handleNotificationTap)
    }

    private fun handleNotificationTap(payload: Map<String, String>) {
        payload["chat_id"]?.let { navigator.openChat(ChatId(it)) }
    }
}
```

`onNewIntent` matters — the activity is usually already running.

**iOS** — read `userInfo` in your `UNUserNotificationCenterDelegate` and feed it
into the same handler.

Shared code can observe taps through `NotificationTaps`:

```kotlin
notificationTaps.opened.collect { payload ->
    payload["chat_id"]?.let { navigator.openChat(ChatId(it)) }
}
```

Taps are **not** replayed: one tapped before the app subscribed must not
resurface later. Subscribe early.

## Wiring it up

Platform-specific, so the platform graph provides it:

```kotlin
// androidMain
@Provides
@SingleIn(AppScope::class)
fun provideNotifications(context: Context): NotificationController =
    AndroidNotificationController(
        context = context,
        config = AndroidPushConfig(
            mainActivityClass = MainActivity::class.java,
            smallIconResId = R.drawable.ic_notification,
        ),
    )

// iosMain
@Provides
@SingleIn(AppScope::class)
fun provideNotifications(): NotificationController = IosNotificationController()
```

`smallIconResId` is mandatory on Android — a notification without one is
silently dropped by the system.

## Testing

```kotlin
val notifications = RecordingNotificationController()
val useCase = NotifyOnMessageUseCase(notifications)

useCase(message)

assertEquals("Ada", notifications.shown.single().title)
```

It also implements `NotificationTaps`, so you can drive tap handling:

```kotlin
notifications.simulateTap(mapOf("chat_id" to "42"))
```

## Notes

- The Android original serialized a whole route object into the intent and
  revived it with `Class.forName`. That is impossible on Kotlin/Native and
  coupled the core to the navigation library; the payload map replaces it.
- Notification ids are `String` in the shared API and hashed to `Int` on
  Android. Distinct ids can in principle collide — keep them short and
  structured (`"chat-42"`), not free-form text.
