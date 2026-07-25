package com.d1onyx.core.essentials.collections

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Map every element concurrently, awaiting all results.
 */
public suspend fun <T, R : Any> Iterable<T>.mapAsync(
    mapper: suspend (T) -> R,
): List<R> = mapNotNullAsync(mapper)

/**
 * Map every element concurrently, dropping `null` results.
 */
public suspend fun <T, R : Any> Iterable<T>.mapNotNullAsync(
    mapper: suspend (T) -> R?,
): List<R> = coroutineScope {
    this@mapNotNullAsync
        .map { item -> async { mapper(item) } }
        .awaitAll()
        .filterNotNull()
}
