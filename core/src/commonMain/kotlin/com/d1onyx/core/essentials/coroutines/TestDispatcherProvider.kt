package com.d1onyx.core.essentials.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A [DispatcherProvider] that routes every dispatcher to one instance.
 *
 * Ships in main source, not test source, so feature modules can use it without
 * a test-fixtures dependency. Point every dispatcher at the `StandardTestDispatcher`
 * from `runTest`, and data-layer coroutines run on the test scheduler:
 *
 * ```
 * @Test
 * fun loadsMessages() = runTest {
 *     val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
 *     val repository = ChatRepositoryImpl(FakeChatApi(), dispatchers)
 *
 *     val messages = repository.messages(chatId)   // no real threads, no delay
 *
 *     assertEquals(expected, messages)
 * }
 * ```
 */
public class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
}
