package com.d1onyx.core.essentials.collections

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionMapExtensionsTest {

    @Test
    fun `mapAsync preserves the original order`() = runTest {
        val result = listOf(1, 2, 3).mapAsync { it * 2 }

        assertEquals(listOf(2, 4, 6), result)
    }

    @Test
    fun `mapNotNullAsync drops nulls`() = runTest {
        val result = listOf(1, 2, 3, 4).mapNotNullAsync { value ->
            value.takeIf { it % 2 == 0 }
        }

        assertEquals(listOf(2, 4), result)
    }
}
