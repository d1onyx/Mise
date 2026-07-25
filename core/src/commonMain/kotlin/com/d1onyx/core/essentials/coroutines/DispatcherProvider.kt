package com.d1onyx.core.essentials.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The set of coroutine dispatchers the app uses, behind an interface.
 *
 * Inject this into repositories and data sources instead of reaching for
 * `Dispatchers.*` directly. Two reasons:
 *
 * 1. **`Dispatchers.IO` does not exist on Kotlin/Native.** Hardcoding it builds
 *    on Android and fails to resolve on iOS. [io] maps to the right thing per
 *    platform — a real IO pool on the JVM, `Dispatchers.Default` on Native,
 *    which is correct there because its file and network APIs do not block a
 *    thread the way the JVM's do.
 * 2. **Tests can replace it.** A [TestDispatcherProvider] backed by a single
 *    `StandardTestDispatcher` makes data-layer code run on the test scheduler,
 *    so `runTest` controls its timing instead of racing real threads.
 *
 * ```
 * @ContributesBinding(AppScope::class)
 * @Inject
 * class ChatRepositoryImpl(
 *     private val api: ChatApi,
 *     private val dispatchers: DispatcherProvider,
 * ) : ChatRepository {
 *     override suspend fun messages(chatId: ChatId): List<Message> =
 *         withContext(dispatchers.io) { api.fetch(chatId).map { it.toDomain() } }
 * }
 * ```
 */
public interface DispatcherProvider {

    /**
     * The main/UI thread. Backed by `Dispatchers.Main`.
     */
    public val main: CoroutineDispatcher

    /**
     * CPU-bound work: parsing, sorting, mapping large lists.
     */
    public val default: CoroutineDispatcher

    /**
     * Blocking IO: disk, network, database. See the class note on why this is
     * not simply `Dispatchers.IO`.
     */
    public val io: CoroutineDispatcher
}

/**
 * The production [DispatcherProvider], backed by `kotlinx.coroutines` dispatchers.
 *
 * [io] is provided per platform through [ioDispatcher], because the standard
 * library only exposes `Dispatchers.IO` on the JVM.
 */
public object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = ioDispatcher()
}

/**
 * The platform's IO dispatcher: `Dispatchers.IO` on the JVM/Android,
 * `Dispatchers.Default` on Kotlin/Native, where a dedicated IO pool does not
 * exist and is not needed.
 */
internal expect fun ioDispatcher(): CoroutineDispatcher
