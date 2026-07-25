# navigation

Navigation abstraction and Compose dialogs. Features issue navigation commands
without knowing which engine is underneath.

Self-contained — no version catalog, no convention plugins.

## Install

**Requires the [`core`](../core) template**, which supplies the logger,
`Dialogs` and the DI scopes. Install that first.

**1.** Copy this `navigation/` folder into your project.

**2.** Add it to `settings.gradle.kts`:

```kotlin
include(":core")
include(":navigation")
```

**3.** Add the Compose plugins to the root `build.gradle.kts`, on top of the
four that `core` needs:

```kotlin
plugins {
    // ... the four from core ...
    id("org.jetbrains.compose") version "1.11.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
```

Then depend on it:

```kotlin
implementation(project(":navigation"))
```

## Declare routes

Routes are `@Serializable` values, so arguments are properties — not
stringly-typed URL segments:

```kotlin
@Serializable data object HomeRoute : Route
@Serializable data object SignInRoute : Route
@Serializable data class ChatRoute(val chatId: String) : Route
```

## Set up the host

```kotlin
@Composable
fun App(graph: AppGraph) {
    val navController = rememberNavController()
    val router = rememberAppRouter(navController)

    // Let injected code navigate — see "Navigating from a feature".
    DisposableEffect(router) {
        graph.routerHolder.attach(router)
        onDispose { graph.routerHolder.detach() }
    }

    CompositionLocalProvider(LocalAppRouter provides router) {
        NavHost(navController, startDestination = HomeRoute) {
            composable<HomeRoute> { HomeScreen() }
            composable<ChatRoute> { entry ->
                ChatScreen(entry.toRoute<ChatRoute>().chatId)
            }
        }
        graph.dialogs.Render()
    }
}
```

`rememberAppRouter` already wraps the router in [debouncing](#debouncing).

## Commands

| Call | Effect |
|---|---|
| `launch(route)` | push onto the back stack |
| `replace(route)` | swap the current screen; back does not restore it |
| `restart(route)` | clear the whole stack and start over — for login/logout |
| `goBack()` | pop |

## Navigating from a feature

Give each feature a narrow router interface so it cannot reach screens it should
not know about:

```kotlin
// feature/chat/domain
interface ChatRouter {
    fun openProfile(userId: UserId)
    fun leaveChat()
}

// feature/chat/data or di
@ContributesBinding(AppScope::class)
@Inject
class ChatRouterImpl(private val router: AppRouter) : ChatRouter {
    override fun openProfile(userId: UserId) = router.launch(ProfileRoute(userId.value))
    override fun leaveChat() = router.goBack()
}

// feature/chat/presentation
@Inject
class ChatViewModel(
    dependencies: CommonDependencies,
    private val chatRouter: ChatRouter,
) : AbstractViewModel(dependencies) {

    fun onProfileClicked(userId: UserId) = chatRouter.openProfile(userId)
}
```

Testing a view-model then needs only a fake `ChatRouter` — no Compose, no
`NavController`.

In UI code that has no constructor, read `LocalAppRouter`. Do **not** read it
from a repository or use case; that turns navigation into hidden global state.

### Why the holder exists

`AppRouter` is bound in the graph to `AppRouterHolder`, which forwards to the
real router once the UI attaches it. The real router needs a
`NavHostController`, which only exists inside a composition, so the graph cannot
build it.

Commands issued while nothing is attached are dropped with a warning, not an
exception — a background job finishing after the UI is gone must not crash the
app.

For a stricter design, skip the holder: expose a `Flow<Route>` from the
view-model and collect it in the UI. Navigation then becomes a pure output of
the view-model, at the cost of wiring per screen.

## Debouncing

A double tap on a list item opens the same screen twice: the second click lands
before the first navigation has recomposed the screen away.
`DebouncingAppRouter` drops commands issued within 500 ms of the previous one.

`restart` is never debounced — it is a programmatic flow switch, never a tap,
and dropping it would leave the user in the wrong flow.

```kotlin
// custom window
DebouncingAppRouter(NavControllerAppRouter(navController), debouncePeriodMillis = 300)
```

Every command is logged under the `Navigation` tag.

## Dialogs

`ComposeDialogs` implements `Dialogs` from `core`, so non-UI code can
ask a question and await the answer:

```kotlin
@Inject
class DeleteChatUseCase(
    private val dialogs: Dialogs,
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(chatId: ChatId) {
        val confirmed = dialogs.showAlertDialog(
            DialogConfig.Default(
                title = "Delete chat?",
                message = "This cannot be undone.",
                positiveButton = "Delete",
                negativeButton = "Cancel",
            )
        )
        if (confirmed) repository.delete(chatId)
    }
}
```

Render once, near the root — `graph.dialogs.Render()` above.

- Several dialogs may be pending at once; they stack in request order.
- Omitting `negativeButton` (or leaving it blank) hides that button.
- Cancelling the calling coroutine dismisses its dialog — a view-model cleared
  mid-question leaves nothing on screen.

Tests inspect `pending` without rendering anything:

```kotlin
val dialogs = ComposeDialogs()
val answer = async { dialogs.showAlertDialog(config) }
runCurrent()

assertEquals(1, dialogs.pending.size)
answer.cancel()
```

## Wiring it up

`NavigationBindings` provides `AppRouter` (via the holder), `ComposeDialogs` and
`Dialogs`. Expose what the UI needs on the graph:

```kotlin
interface AppGraph {
    val routerHolder: AppRouterHolder
    val dialogs: ComposeDialogs
}
```

## Notes

- **The engine is Navigation 2, not 3.** `androidx.navigation3` — which the
  original Android app used — is Android-only; there is no KMP port yet.
  `AppRouter` exists precisely so the engine can be swapped later without
  touching features.
- **No `ScreenScope` DSL.** The Android original let a screen declare its own
  toolbar and navigation bar. That needs a design system to render, and is
  deliberately out of scope here — screens compose their own scaffold.
