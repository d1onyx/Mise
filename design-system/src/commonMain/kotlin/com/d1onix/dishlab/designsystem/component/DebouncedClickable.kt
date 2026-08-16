package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlin.time.TimeSource

/** Default window in which a repeat tap on the same clickable is ignored. */
public const val DEFAULT_CLICK_DEBOUNCE_MILLIS: Long = 500

/**
 * Like [Modifier.clickable], but ignores taps that land within
 * [debounceMillis] of the previous one.
 *
 * Every `Mise*Button`/`MisePanel` click goes through this, so a double tap
 * anywhere in the app fires [onClick] once — not a per-screen workaround, one
 * fix at the root all clickables share. This guards against a fast repeat
 * click firing the handler again before the first click's effect (navigation,
 * a network call, a toggle) has had a chance to change the UI underneath it.
 *
 * The guard is plain state read and written only inside the click callback
 * itself, never during composition — an earlier version gated `enabled` on a
 * `mutableStateOf` flipped from inside this same composable, but a state
 * write there only invalidates this function's own recompose scope, not the
 * caller's `Box`/`Modifier` expression that actually renders it, so the
 * updated `enabled` value never reached the real clickable and the debounce
 * silently never fired.
 */
@Composable
public fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    debounceMillis: Long = DEFAULT_CLICK_DEBOUNCE_MILLIS,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier {
    val originMark = remember { TimeSource.Monotonic.markNow() }
    val lastClickMillis = remember { LongArray(1) { -1L } }
    return this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        val nowMillis = originMark.elapsedNow().inWholeMilliseconds
        if (lastClickMillis[0] < 0 || nowMillis - lastClickMillis[0] >= debounceMillis) {
            lastClickMillis[0] = nowMillis
            onClick()
        }
    }
}
