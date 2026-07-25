@file:OptIn(ExperimentalCoroutinesApi::class)

package com.d1onyx.core.essentials.flows

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Re-emit the first item within [periodMillis] immediately, and then emit the
 * most recent subsequent item only when the [periodMillis] timeout expires.
 */
public fun <T> Flow<T>.throttle(
    periodMillis: Long,
): Flow<T> = channelFlow {
    val channel = produce(capacity = Channel.CONFLATED) {
        collect { send(it) }
    }
    while (true) {
        channel.receiveCatching()
            .onSuccess {
                val sendJob = launch { send(it) }
                val delayJob = launch { delay(periodMillis) }
                sendJob.join()
                delayJob.join()
            }
            .onClosed { break }
            .onFailure { exception ->
                exception?.let { throw it }
            }
    }
}
