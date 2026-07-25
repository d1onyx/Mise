package com.d1onyx.core.datastore

import com.d1onyx.core.essentials.logger.DefaultLogger
import com.d1onyx.core.essentials.logger.RecordingLogSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Exercises the real DataStore implementation against a temporary file, so the
 * platform-key mapping and persistence are covered — not just the fake.
 */
class DataStoreKeyValueStorageTest {

    private val path: Path =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "core-kmp-test-${Random.nextLong()}.preferences_pb"

    private val sink = RecordingLogSink()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun storage(): DataStoreKeyValueStorage {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scopes += scope
        return DataStoreKeyValueStorage(
            dataStore = createPreferencesDataStore(scope = scope) { path.toString() },
            logger = DefaultLogger(sink),
        )
    }

    @AfterTest
    fun tearDown() {
        scopes.forEach { it.cancel() }
        FileSystem.SYSTEM.delete(path, mustExist = false)
    }

    private val darkTheme = PreferenceKey.BooleanKey("dark_theme")
    private val lastSync = PreferenceKey.LongKey("last_sync_millis")
    private val userName = PreferenceKey.StringKey("user_name")
    private val launchCount = PreferenceKey.IntKey("launch_count")
    private val ratio = PreferenceKey.DoubleKey("ratio")
    private val tags = PreferenceKey.StringSetKey("tags")

    @Test
    fun `returns null for an absent key`() = runTest {
        assertNull(storage().get(darkTheme))
    }

    @Test
    fun `round-trips every supported key type`() = runTest {
        val storage = storage()

        storage.put(userName, "Ada")
        storage.put(launchCount, 7)
        storage.put(lastSync, 1_700_000_000_000L)
        storage.put(darkTheme, true)
        storage.put(ratio, 0.5)
        storage.put(tags, setOf("a", "b"))

        assertEquals("Ada", storage.get(userName))
        assertEquals(7, storage.get(launchCount))
        assertEquals(1_700_000_000_000L, storage.get(lastSync))
        assertEquals(true, storage.get(darkTheme))
        assertEquals(0.5, storage.get(ratio))
        assertEquals(setOf("a", "b"), storage.get(tags))
    }

    @Test
    fun `observe reflects a later write`() = runTest {
        val storage = storage()
        storage.put(launchCount, 1)

        assertEquals(1, storage.observe(launchCount).first())

        storage.put(launchCount, 2)

        assertEquals(2, storage.observe(launchCount).first())
    }

    @Test
    fun `remove drops only the requested key`() = runTest {
        val storage = storage()
        storage.put(userName, "Ada")
        storage.put(launchCount, 7)

        storage.remove(userName)

        assertNull(storage.get(userName))
        assertEquals(7, storage.get(launchCount))
    }

    @Test
    fun `clear drops everything`() = runTest {
        val storage = storage()
        storage.put(userName, "Ada")
        storage.put(launchCount, 7)

        storage.clear()

        assertNull(storage.get(userName))
        assertNull(storage.get(launchCount))
    }

    @Test
    fun `persists to disk once the owning scope is released`() = runTest {
        val firstScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        DataStoreKeyValueStorage(
            createPreferencesDataStore(scope = firstScope) { path.toString() },
            DefaultLogger(sink),
        ).put(userName, "Ada")

        // DataStore forbids two live instances over one file, so the first must
        // be released before a second can legitimately read the file back.
        firstScope.cancel()

        assertEquals("Ada", storage().get(userName))
    }
}
