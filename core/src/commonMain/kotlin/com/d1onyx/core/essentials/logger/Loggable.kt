package com.d1onyx.core.essentials.logger

/**
 * A mixin that gives any class a logger and a tag derived from its own name,
 * so a feature starts logging by implementing one interface:
 *
 * ```
 * class LoginRepositoryImpl : LoginRepository, Loggable {
 *     override suspend fun login(credentials: Credentials) {
 *         logD { "login attempt for ${credentials.email}" }
 *     }
 * }
 * ```
 *
 * Override [logTag] to group several classes under one tag — usually the
 * feature name, which is what makes LogCat filtering by feature possible:
 *
 * ```
 * class LoginViewModel : Loggable {
 *     override val logTag: String get() = "Auth"
 * }
 * ```
 */
public interface Loggable {

    /**
     * The tag applied to every record from this class. Defaults to the simple
     * class name, which is `null` for anonymous objects — hence the fallback.
     */
    public val logTag: String
        get() = this::class.simpleName ?: "Anonymous"

    /**
     * The logger used by this class. Defaults to the global facade; override it
     * to inject a logger explicitly, which is what tests do.
     */
    public val logger: Logger
        get() = Logger
}

public fun Loggable.logV(message: () -> String): Unit =
    logger.log(LogLevel.Verbose, logTag, null, message)

public fun Loggable.logD(message: () -> String): Unit =
    logger.log(LogLevel.Debug, logTag, null, message)

public fun Loggable.logI(message: () -> String): Unit =
    logger.log(LogLevel.Info, logTag, null, message)

public fun Loggable.logW(throwable: Throwable? = null, message: () -> String): Unit =
    logger.log(LogLevel.Warn, logTag, throwable, message)

public fun Loggable.logE(throwable: Throwable? = null, message: () -> String): Unit =
    logger.log(LogLevel.Error, logTag, throwable, message)
