# View-models

View-model base class with self-logging lifecycle and a mixin system for
reusable behaviour.


Brings `androidx.lifecycle` ViewModel (multiplatform), so screens work the same
on Android and iOS.

## A minimal view-model

```kotlin
@Inject
class LoginViewModel(
    dependencies: CommonDependencies,
    private val login: LoginUseCase,
) : AbstractViewModel(dependencies)
```

`CommonDependencies` bundles the logger and the exception handler. It is a
constructor parameter so Metro validates it at compile time — a missing binding
fails the build, not the screen.

You get lifecycle logging for free:

```
D/LoginViewModel: created
D/LoginViewModel: cleared
```

## Running work: WithMviState

Add the mixin to get a progress flag, tracing and error routing:

```kotlin
@Inject
class LoginViewModel(
    dependencies: CommonDependencies,
    private val login: LoginUseCase,
) : AbstractViewModel(dependencies), WithMviState<LoginUiState> {

    fun onSubmit(credentials: Credentials) = launch("login") {
        login(credentials)
    }
}
```

`launch(operation)`:

1. raises `progressStateFlow`
2. traces the block — `→ login` / `← login (142ms)`
3. on failure, logs at error level **and** passes the exception to the app's
   `ExceptionHandler`
4. lowers the flag

Cancellation propagates untouched and never reaches the handler.

Bind the flag straight into UI state:

```kotlin
val uiState: StateFlow<LoginUiState> = combine(
    progressStateFlow,
    credentialsFlow,
) { inProgress, credentials -> LoginUiState(inProgress, credentials) }
    .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000), LoginUiState())
```

### Keeping the spinner during navigation

Default policy hides it when the block finishes. If success navigates away,
keep it up so the screen does not flash:

```kotlin
fun onSubmit(credentials: Credentials) =
    launch("login", HideProgressPolicy.OnError) {
        login(credentials)
        navigator.toHome()
    }
```

## Initialization: WithInitCallback

```kotlin
@Inject
class ProfileViewModel(
    dependencies: CommonDependencies,
    private val loadProfile: LoadProfileUseCase,
) : AbstractViewModel(dependencies), WithInitCallback {

    override suspend fun onInitialized() {
        profile = loadProfile()
    }
}
```

Called once, after the constructor completes, wrapped in a traced operation.
A failure inside it is logged at error level and swallowed — one screen failing
to initialise must not crash the app.

## Custom mixins

A mixin is an interface that owns state through `getMixinState`:

```kotlin
interface WithSearchQuery : ViewModelMixin {

    val query: StateFlow<String>
        get() = getMixinState(::SearchState).query

    fun onQueryChanged(value: String) {
        getMixinState(::SearchState).query.value = value
    }
}

internal class SearchState(
    val query: MutableStateFlow<String> = MutableStateFlow(""),
)
```

State is created on first access and cached per view-model. If it implements
`AutoCloseable`, it is closed in `onCleared`.

Two rules learned the hard way:

- **The initializer is required.** No reflective default — Kotlin/Native cannot
  construct a class at runtime.
- **Put the state class at top level**, not nested in the interface. Keys are
  `KClass`, and a class nested in an interface cannot be referenced by
  constructor from the interface body.

## Logging inside a view-model

`AbstractViewModel` implements `Loggable`, so all helpers are available and
tagged with the class name:

```kotlin
logD { "user tapped retry" }
logE(exception) { "could not refresh" }
```

## Testing

No container, no DI setup:

```kotlin
class LoginViewModelTest {

    private val sink = RecordingLogSink()
    private val handled = mutableListOf<Throwable>()

    private val dependencies = CommonDependencies(
        logger = DefaultLogger(sink),
        exceptionHandler = ExceptionHandler { handled += it },
    )

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reports a failed login`() = runTest {
        val viewModel = LoginViewModel(dependencies, FailingLoginUseCase())

        viewModel.onSubmit(credentials)
        advanceUntilIdle()

        assertTrue(handled.single() is BackendException)
    }
}
```

`Dispatchers.setMain` is required — `viewModelScope` runs on `Dispatchers.Main`.

To assert `onCleared` behaviour, drive it through a store:

```kotlin
val store = ViewModelStore()
store.put("key", viewModel)
store.clear()
```

## Notes

- `WithMviState` has no reducer helpers; those were built on `com.elveum:container`
  (JVM-only). Compose a `StateFlow` from `progressStateFlow` and your own flows.
- There is no `awaitDependencies()` — it only existed because Hilt injected
  members *after* construction. Constructor injection has no such window.
