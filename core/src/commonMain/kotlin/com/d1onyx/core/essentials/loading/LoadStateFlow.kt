package com.d1onyx.core.essentials.loading

import com.d1onyx.core.essentials.logger.LogLevel
import com.d1onyx.core.essentials.logger.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wrap a value stream into [LoadState], emitting [LoadState.Loading] first and
 * converting any thrown exception into [LoadState.Failure].
 *
 * Every failure is logged under [tag], which is what makes a feature's data
 * layer traceable without a log call at each call site.
 */
public fun <T> Flow<T>.asLoadState(tag: String): Flow<LoadState<T>> =
    map<T, LoadState<T>> { LoadState.Success(it) }
        .onStart { emit(LoadState.Loading) }
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            Logger.log(LogLevel.Error, tag, throwable) { "load failed" }
            emit(LoadState.Failure(throwable))
        }

/**
 * Run a one-shot suspending [loader] as a [LoadState] stream.
 *
 * ```
 * val profile: Flow<LoadState<Profile>> = loadStateOf("Profile") { api.fetchProfile() }
 * ```
 */
public fun <T> loadStateOf(
    tag: String,
    loader: suspend () -> T,
): Flow<LoadState<T>> = flow { emit(loader()) }.asLoadState(tag)
