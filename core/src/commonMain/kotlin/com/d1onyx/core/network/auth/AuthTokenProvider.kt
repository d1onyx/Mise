package com.d1onyx.core.network.auth

/**
 * Supplies the current auth token for outgoing requests.
 *
 * Suspending, unlike the Android original: on a shared core the token usually
 * comes from asynchronous storage (DataStore, Keychain), and a blocking read
 * has nowhere safe to happen on iOS.
 */
public fun interface AuthTokenProvider {

    /**
     * The current auth token, or `null` when the user is not logged in.
     */
    public suspend fun provideToken(): String?

    public companion object {

        /**
         * A provider that never supplies a token — for unauthenticated clients
         * and for tests.
         */
        public val None: AuthTokenProvider = AuthTokenProvider { null }
    }
}
