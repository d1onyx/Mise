package com.d1onyx.core.essentials.resources

import com.d1onyx.core.essentials.entities.HttpCode
import com.d1onyx.core.essentials.entities.ServerCode

/**
 * Error messages provider for all exceptions from the core-essentials module.
 */
public interface CoreStringProvider : StringProvider {
    public val connectionErrorMessage: String
    public val authErrorMessage: String
    public val unknownErrorMessage: String
    public val invalidBackendResponseErrorMessage: String
    public val tooManyRequests: String
    public val logoutAction: String
    public val deleteAction: String
    public val cancelAction: String
    public val tryAgainAction: String
    public val okAction: String
    public val openAction: String

    public fun backendErrorMessage(
        serverCode: ServerCode,
        httpCode: HttpCode,
        backendMessage: String,
    ): String
}
