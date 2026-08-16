package com.d1onix.dishlab.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SingleUseClickTest {

    @Test
    fun `invokes the action only once while the click handler is retained`() {
        var invocationCount = 0
        val click = SingleUseClick { invocationCount++ }

        click()
        click()

        assertEquals(1, invocationCount)
        assertFalse(click.enabled)
    }
}
