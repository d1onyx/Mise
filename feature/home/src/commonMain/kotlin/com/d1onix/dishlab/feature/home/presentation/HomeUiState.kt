package com.d1onix.dishlab.feature.home.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val savedCount: Int = 0,
    val historyCount: Int = 0,
    /** Set while a transient message is on screen; the text itself is a resource. */
    val showProfileHint: Boolean = false,
)

sealed interface HomeAction {
    data object ScanClicked : HomeAction
    data object SavedClicked : HomeAction
    data object HistoryClicked : HomeAction
    data object ProfileClicked : HomeAction
    data object MessageShown : HomeAction
}
