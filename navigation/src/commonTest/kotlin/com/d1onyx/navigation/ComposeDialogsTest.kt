@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.d1onyx.navigation

import com.d1onyx.core.essentials.dialogs.DialogConfig
import com.d1onyx.navigation.dialogs.ComposeDialogs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the suspend/queue behaviour. Rendering is Compose UI and is not
 * exercised here.
 */
class ComposeDialogsTest {

    private val dialogs = ComposeDialogs()

    private fun config(title: String = "Delete chat?") = DialogConfig.Default(
        title = title,
        message = "This cannot be undone.",
        positiveButton = "Delete",
        negativeButton = "Cancel",
    )

    @Test
    fun `suspends until the dialog is answered`() = runTest {
        val answer = async { dialogs.showAlertDialog(config()) }
        runCurrent()

        assertTrue(answer.isActive, "the call must not return before the user answers")
        assertEquals(1, dialogs.pending.size)

        // runTest waits for every child coroutine, and this one is waiting on a
        // user who will never answer.
        answer.cancel()
    }

    @Test
    fun `queues several dialogs in request order`() = runTest {
        val first = async { dialogs.showAlertDialog(config("first")) }
        val second = async { dialogs.showAlertDialog(config("second")) }
        runCurrent()

        assertEquals(listOf("first", "second"), dialogs.pending.map { it.title })

        first.cancel()
        second.cancel()
    }

    @Test
    fun `cancelling the caller dismisses its dialog`() = runTest {
        val answer = async { dialogs.showAlertDialog(config()) }
        runCurrent()

        answer.cancel()
        runCurrent()

        assertTrue(dialogs.pending.isEmpty(), "a cancelled request must not leave a dialog on screen")
    }

    @Test
    fun `cancelling one dialog leaves the others pending`() = runTest {
        val first = async { dialogs.showAlertDialog(config("first")) }
        val second = async { dialogs.showAlertDialog(config("second")) }
        runCurrent()

        first.cancel()
        runCurrent()

        assertEquals(listOf("second"), dialogs.pending.map { it.title })
        second.cancel()
    }

    @Test
    fun `starts with nothing pending`() = runTest {
        assertTrue(dialogs.pending.isEmpty())
    }
}
