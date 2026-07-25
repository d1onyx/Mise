package com.d1onyx.core.essentials.loading

/**
 * The state of an asynchronously loaded value: still loading, loaded, or failed.
 *
 * This replaces `com.elveum:container` in the multiplatform core. That library
 * is JVM-only as of 3.4.0 (a KMP migration is in progress upstream), and a
 * shared module cannot depend on a JVM-only artifact. The shape is deliberately
 * close to `Container`, so swapping back is a mechanical change if the KMP
 * release lands and the richer feature set is wanted.
 */
public sealed interface LoadState<out T> {

    /**
     * The value is being loaded and has never been delivered yet.
     */
    public data object Loading : LoadState<Nothing>

    /**
     * The value has been loaded successfully.
     *
     * @param isStale `true` when this is a cached value that is currently being
     * refreshed — lets the UI show content and a progress indicator at once.
     */
    public data class Success<out T>(
        val value: T,
        val isStale: Boolean = false,
    ) : LoadState<T>

    /**
     * Loading failed with [exception].
     */
    public data class Failure(
        val exception: Throwable,
    ) : LoadState<Nothing>
}

/**
 * The loaded value, or `null` while loading or after a failure.
 */
public fun <T> LoadState<T>.getOrNull(): T? = (this as? LoadState.Success)?.value

/**
 * The failure cause, or `null` in any other state.
 */
public fun <T> LoadState<T>.exceptionOrNull(): Throwable? = (this as? LoadState.Failure)?.exception

public val LoadState<*>.isLoading: Boolean get() = this is LoadState.Loading
public val LoadState<*>.isSuccess: Boolean get() = this is LoadState.Success
public val LoadState<*>.isFailure: Boolean get() = this is LoadState.Failure

/**
 * Transform the loaded value, leaving [LoadState.Loading] and
 * [LoadState.Failure] untouched.
 */
public inline fun <T, R> LoadState<T>.map(transform: (T) -> R): LoadState<R> = when (this) {
    is LoadState.Loading -> LoadState.Loading
    is LoadState.Failure -> this
    is LoadState.Success -> LoadState.Success(transform(value), isStale)
}

/**
 * Handle all three states in one expression.
 */
public inline fun <T, R> LoadState<T>.fold(
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): R = when (this) {
    is LoadState.Loading -> onLoading()
    is LoadState.Success -> onSuccess(value)
    is LoadState.Failure -> onFailure(exception)
}

public inline fun <T> LoadState<T>.onSuccess(action: (T) -> Unit): LoadState<T> = apply {
    if (this is LoadState.Success) action(value)
}

public inline fun <T> LoadState<T>.onFailure(action: (Throwable) -> Unit): LoadState<T> = apply {
    if (this is LoadState.Failure) action(exception)
}
