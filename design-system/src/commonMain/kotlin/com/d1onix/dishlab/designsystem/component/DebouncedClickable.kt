package com.d1onix.dishlab.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * A one-shot click handler. It becomes inactive before invoking [onClick] and
 * stays that way for as long as its composable remains in the UI tree.
 *
 * This intentionally has no time window: a slow device must not allow a
 * second navigation, request, or state transition after the first tap.
 */
public class SingleUseClick internal constructor(
    private val onClick: () -> Unit,
) : () -> Unit {
    public var enabled: Boolean by mutableStateOf(true)
        private set

    override fun invoke() {
        if (!enabled) return
        enabled = false
        onClick()
    }
}

@Composable
public fun rememberSingleUseClick(
    onClick: () -> Unit,
): SingleUseClick {
    val currentOnClick by rememberUpdatedState(onClick)
    return remember { SingleUseClick { currentOnClick() } }
}
