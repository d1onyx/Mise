package com.d1onyx.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryKeyValueStorageTest {

    private val darkTheme = PreferenceKey.BooleanKey("dark_theme")
    private val lastSync = PreferenceKey.LongKey("last_sync_millis")
    private val tags = PreferenceKey.StringSetKey("tags")

    @Test
    fun `returns null for an absent key`() = runTest {
        val storage = InMemoryKeyValueStorage()

        assertNull(storage.get(darkTheme))
    }

    @Test
    fun `stores and reads back a value`() = runTest {
        val storage = InMemoryKeyValueStorage()

        storage.put(darkTheme, true)

        assertEquals(true, storage.get(darkTheme))
    }

    @Test
    fun `keeps keys of different types apart`() = runTest {
        val storage = InMemoryKeyValueStorage()

        storage.put(darkTheme, true)
        storage.put(lastSync, 42L)
        storage.put(tags, setOf("a", "b"))

        assertEquals(true, storage.get(darkTheme))
        assertEquals(42L, storage.get(lastSync))
        assertEquals(setOf("a", "b"), storage.get(tags))
    }

    @Test
    fun `observe emits the updated value`() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.put(lastSync, 1L)

        assertEquals(1L, storage.observe(lastSync).first())

        storage.put(lastSync, 2L)

        assertEquals(2L, storage.observe(lastSync).first())
    }

    @Test
    fun `remove drops only the requested key`() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.put(darkTheme, true)
        storage.put(lastSync, 42L)

        storage.remove(darkTheme)

        assertNull(storage.get(darkTheme))
        assertEquals(42L, storage.get(lastSync))
    }

    @Test
    fun `clear drops everything`() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.put(darkTheme, true)
        storage.put(lastSync, 42L)

        storage.clear()

        assertNull(storage.get(darkTheme))
        assertNull(storage.get(lastSync))
    }

    @Test
    fun `honours seeded initial values`() = runTest {
        val storage = InMemoryKeyValueStorage(mapOf(darkTheme to true))

        assertEquals(true, storage.get(darkTheme))
    }

    @Test
    fun `getOrDefault falls back only when the key is absent`() = runTest {
        val storage = InMemoryKeyValueStorage()

        assertEquals(false, storage.getOrDefault(darkTheme, defaultValue = false))

        storage.put(darkTheme, true)

        assertEquals(true, storage.getOrDefault(darkTheme, defaultValue = false))
    }
}
