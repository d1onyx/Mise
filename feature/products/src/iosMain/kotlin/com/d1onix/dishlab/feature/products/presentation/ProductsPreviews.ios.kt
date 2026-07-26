package com.d1onix.dishlab.feature.products.presentation

import androidx.compose.ui.window.ComposeUIViewController
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.products.presentation.connections.ConnectionOverviewContent
import com.d1onix.dishlab.feature.products.presentation.graph.GraphContent
import com.d1onix.dishlab.feature.products.presentation.history.HistoryContent
import platform.UIKit.UIViewController

fun graphPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.Default, onAction = {})
    }
}

/** The sheet is the screen's only bottom-anchored surface, so it meets the home indicator. */
fun graphSheetPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        GraphContent(state = GraphPreviewStates.SheetOpen, onAction = {})
    }
}

fun historyPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        HistoryContent(state = HistoryPreviewStates.Default, onAction = {})
    }
}

fun connectionOverviewPreviewController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        ConnectionOverviewContent(
            state = ConnectionOverviewPreviewStates.Default,
            onAction = {},
        )
    }
}
