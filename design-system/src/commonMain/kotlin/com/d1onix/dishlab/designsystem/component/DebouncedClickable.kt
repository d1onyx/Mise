package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 */
@Composable
public fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    debounceMillis: Long = DEFAULT_CLICK_DEBOUNCE_MILLIS,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var clickable by remember { mutableStateOf(true) }
    return this.clickable(
        enabled = enabled && clickable,
        onClickLabel = onClickLabel,
        role = role,
    ) {
        clickable = false
        onClick()
        coroutineScope.launch {
            delay(debounceMillis)
            clickable = true
        }
    }
}
