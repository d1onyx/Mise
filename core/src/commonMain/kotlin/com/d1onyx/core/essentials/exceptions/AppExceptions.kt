package com.d1onyx.core.essentials.exceptions

import com.d1onyx.core.essentials.entities.HttpCode
import com.d1onyx.core.essentials.entities.ServerCode
import com.d1onyx.core.essentials.resources.CoreStringProvider
import com.d1onyx.core.essentials.resources.StringProviderStore

/**
 * An optional marker interface that can be implemented by any exception class
 * to add a support of localized error messages.
 */
public interface WithLocalizedMessage {

    /**
     * Get a localized error message for an exception that implements this interface.
     */
    public fun getLocalizedErrorMessage(stringProviderStore: StringProviderStore): String
}

/**
 * Base top-level exception class for all other in-app exceptions.
 */
public abstract class AbstractAppException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Base exception class for all other in-app exceptions from the core-essentials module.
 * All these exceptions use [CoreStringProvider] for localizing error messages.
 */
public abstract class AbstractCoreAppException(
    message: String,
    cause: Throwable? = null,
) : AbstractAppException(message, cause), WithLocalizedMessage {

    override fun getLocalizedErrorMessage(stringProviderStore: StringProviderStore): String =
        getLocalizedErrorMessage(stringProviderStore<CoreStringProvider>())

    /**
     * Subclasses override this method to specify a localized error message.
     */
    public abstract fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String
}

/**
 * Auth error, usually happens when a user's session has been expired.
 */
public class AuthException(
    cause: Throwable? = null,
) : AbstractCoreAppException("User session has been expired", cause) {

    override fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String =
        stringProvider.authErrorMessage
}

/**
 * Internet connection error, e.g. remote server is not accessible or a connection
 * is not available for some reason.
 */
public class ConnectionException(
    cause: Throwable? = null,
) : AbstractCoreAppException("Network connection error", cause) {

    override fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String =
        stringProvider.connectionErrorMessage
}

/**
 * Represents a remote backend error.
 */
public class BackendException(
    public val httpCode: HttpCode = HttpCode(400),
    public val serverCode: ServerCode = ServerCode(""),
    public val backendMessage: String = "",
    cause: Throwable? = null,
) : AbstractCoreAppException(
    "Server error (serverCode=$serverCode, httpCode=$httpCode): $backendMessage",
    cause,
) {

    override fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String =
        stringProvider.backendErrorMessage(serverCode, httpCode, backendMessage)
}

/**
 * Represents an invalid response from the remote server.
 */
public class InvalidBackendResponseException(
    cause: Throwable? = null,
) : AbstractCoreAppException("Can't parse server response", cause) {

    override fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String =
        stringProvider.invalidBackendResponseErrorMessage
}

/**
 * Thrown when a request to the backend has call limits currently applied to the client.
 */
public class RateLimitException(
    cause: Throwable? = null,
) : AbstractCoreAppException("Too many requests.", cause) {

    override fun getLocalizedErrorMessage(stringProvider: CoreStringProvider): String =
        stringProvider.tooManyRequests
}

/**
 * Something strange happened within the app, e.g. unexpected error, bugs, etc.
 */
public class UnknownException(
    cause: Throwable,
) : AbstractAppException("Unknown exception", cause)
