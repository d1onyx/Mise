package com.d1onyx.core.essentials.exceptions

import kotlin.concurrent.Volatile

/**
 * An exception handler that can smoothly process exceptions thrown by the app.
 *
 * Implementations may handle exceptions differently: toasts, alert dialogs, snackbars.
 */
public fun interface ExceptionHandler {
    public fun handleException(exception: Throwable)
}

/**
 * Lets a screen attach an extra action to the error UI, e.g. a "Retry" button.
 */
public interface CustomExceptionHandler {

    public fun getCustomExceptionHandlerAction(exception: Throwable): Action

    public interface Action {

        public val actionName: String
        public val onClick: () -> Unit

        public data class Default(
            override val actionName: String,
            override val onClick: () -> Unit,
        ) : Action
    }
}

/**
 * A mapper that localizes exception messages, making them readable for users.
 */
public interface ExceptionToMessageMapper {

    /**
     * Get a localized error message for the specified exception.
     */
    public fun getLocalizedMessage(exception: Throwable): String

    /**
     * A companion object that allows accessing an [ExceptionToMessageMapper]
     * instance directly:
     *
     * ```
     * val message = ExceptionToMessageMapper.getLocalizedMessage(exception)
     * ```
     */
    public companion object : ExceptionToMessageMapper {

        @Volatile
        private var instance: ExceptionToMessageMapper = PlainExceptionToMessageMapper

        override fun getLocalizedMessage(exception: Throwable): String =
            instance.getLocalizedMessage(exception)

        public fun install(mapper: ExceptionToMessageMapper) {
            instance = mapper
        }

        public fun reset() {
            instance = PlainExceptionToMessageMapper
        }
    }
}

/**
 * An [ExceptionToMessageMapper] that does not localize anything and just returns
 * the raw exception message. Used as the default and in unit tests.
 */
public object PlainExceptionToMessageMapper : ExceptionToMessageMapper {

    override fun getLocalizedMessage(exception: Throwable): String =
        exception.message ?: "Unknown error occurred"
}
