package com.d1onix.dishlab.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class AppBackNavigationTest {
    @Test
    fun `back exits when navigation has only its start destination`() {
        var exited = 0

        handleAppBack(popBackStack = { false }, onExit = { exited++ })

        assertEquals(1, exited)
    }

    @Test
    fun `back pops a deeper destination without exiting`() {
        var exited = 0

        handleAppBack(popBackStack = { true }, onExit = { exited++ })

        assertEquals(0, exited)
    }
}
