package com.d1onyx.navigation.di

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.dialogs.Dialogs
import com.d1onyx.navigation.AppRouter
import com.d1onyx.navigation.AppRouterHolder
import com.d1onyx.navigation.dialogs.ComposeDialogs
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Wires navigation and dialogs into the app graph.
 *
 * Both are singletons: the dialog queue and the router handle must be the same
 * instance for every feature that touches them.
 */
@ContributesTo(AppScope::class)
@BindingContainer
public interface NavigationBindings {

    /**
     * Features inject [AppRouter]; the holder forwards to the live router once
     * the navigation host attaches it.
     */
    @Binds
    public val AppRouterHolder.bindAppRouter: AppRouter

    public companion object {

        @Provides
        @SingleIn(AppScope::class)
        public fun provideComposeDialogs(): ComposeDialogs = ComposeDialogs()

        @Provides
        public fun provideDialogs(dialogs: ComposeDialogs): Dialogs = dialogs
    }
}
