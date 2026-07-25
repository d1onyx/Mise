package com.d1onyx.navigation.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.d1onyx.core.essentials.dialogs.DialogConfig
import com.d1onyx.core.essentials.dialogs.Dialogs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * [Dialogs] rendered with Compose `AlertDialog`.
 *
 * Lets non-UI code ask the user a question and await the answer:
 *
 * ```
 * // in a view-model or use case — no Compose in sight
 * val confirmed = dialogs.showAlertDialog(
 *     DialogConfig.Default(
 *         title = "Delete chat?",
 *         message = "This cannot be undone.",
 *         positiveButton = "Delete",
 *         negativeButton = "Cancel",
 *     )
 * )
 * if (confirmed) repository.deleteChat(chatId)
 * ```
 *
 * Render it once, above the navigation host:
 *
 * ```
 * val dialogs = remember { ComposeDialogs() }
 * NavHost(...)
 * dialogs.Render()
 * ```
 *
 * Several dialogs may be pending at once; they stack in request order.
 * Cancelling the calling coroutine dismisses its dialog.
 */
public class ComposeDialogs : Dialogs {

    private val records = SnapshotStateList<DialogRecord>()
    private var idSeq: Long = 0L

    /**
     * Configs currently awaiting an answer, oldest first. Exposed for tests.
     */
    public val pending: List<DialogConfig> get() = records.map { it.config }

    override suspend fun showAlertDialog(config: DialogConfig): Boolean =
        suspendCancellableCoroutine { continuation ->
            val id = ++idSeq
            val dismiss = { records.removeAll { it.id == id } }
            records += DialogRecord(
                id = id,
                config = config,
                onConfirm = {
                    dismiss()
                    continuation.resume(true)
                },
                onDismiss = {
                    dismiss()
                    continuation.resume(false)
                },
            )
            continuation.invokeOnCancellation { dismiss() }
        }

    /**
     * Render every pending dialog. Call once, near the root of the UI.
     */
    @Composable
    public fun Render() {
        records.forEach { record -> record.Render() }
    }

    @Composable
    private fun DialogRecord.Render() {
        val negative = config.negativeButton
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(config.title) },
            text = { Text(config.message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(config.positiveButton) }
            },
            dismissButton = if (!negative.isNullOrBlank()) {
                { TextButton(onClick = onDismiss) { Text(negative) } }
            } else {
                null
            },
        )
    }

    private class DialogRecord(
        val id: Long,
        val config: DialogConfig,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    )
}
