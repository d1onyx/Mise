package com.d1onyx.core.essentials.di

import com.d1onyx.core.essentials.coroutines.DefaultDispatcherProvider
import com.d1onyx.core.essentials.coroutines.DispatcherProvider
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * App-wide bindings from the essentials layer.
 *
 * Provides [DispatcherProvider] so any repository can inject it without the
 * host app wiring it by hand. Override in a test graph by contributing a
 * `TestDispatcherProvider` with `replaces`.
 */
@ContributesTo(AppScope::class)
@BindingContainer
public interface EssentialsBindings {

    public companion object {

        @Provides
        public fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider
    }
}
