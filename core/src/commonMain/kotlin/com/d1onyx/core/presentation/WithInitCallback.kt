package com.d1onyx.core.presentation

import com.d1onyx.core.presentation.base.AbstractViewModel
import com.d1onyx.core.presentation.base.ViewModelMixin

/**
 * A mixin that provides an [onInitialized] callback for any view-model.
 *
 * [AbstractViewModel] detects this interface and invokes the callback once,
 * after construction completes, wrapping it in a logged operation:
 *
 * ```
 * D/LoginViewModel: created
 * D/LoginViewModel: → onInitialized
 * D/LoginViewModel: ← onInitialized (34ms)
 * ```
 *
 * A failure inside [onInitialized] is logged at error level with its stack
 * trace rather than silently cancelling the view-model's scope.
 */
public interface WithInitCallback : ViewModelMixin {

    /**
     * Called once, shortly after the view-model has been constructed.
     */
    public suspend fun onInitialized()
}
