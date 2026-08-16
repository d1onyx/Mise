package com.d1onix.dishlab.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Default window a clickable stays disabled for after it fires. */
public const val DEFAULT_CLICK_DEBOUNCE_MILLIS: Long = 500

/**
 * A click handler that goes inactive the instant it fires and comes back
 * after [debounceMillis] — [enabled] is read directly by the caller of
 * [rememberDebouncedClick], in the same `Box`/`Modifier` expression that
 * renders the clickable, so its `false` value is guaranteed to reach the
 * render tree. An earlier attempt wrapped `Modifier.clickable`'s `enabled`
 * inside a separate `@Composable fun Modifier.debouncedClickable()`; toggling
 * a state read there only invalidated *that* function's own recompose scope,
 * not the caller's, so the disabled value never reached the actual widget.
 */
public class DebouncedClick internal constructor(
    private val scope: CoroutineScope,
    private val debounceMillis: Long,
    private val onClick: () -> Unit,
) : () -> Unit {
    public var enabled: Boolean by mutableStateOf(true)
        private set

    override fun invoke() {
        if (!enabled) return
        enabled = false
        onClick()
        scope.launch {
            delay(debounceMillis)
            enabled = true
        }
    }
}

@Composable
public fun rememberDebouncedClick(
    debounceMillis: Long = DEFAULT_CLICK_DEBOUNCE_MILLIS,
    onClick: () -> Unit,
): DebouncedClick {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    return remember { DebouncedClick(scope, debounceMillis) { currentOnClick() } }
}
