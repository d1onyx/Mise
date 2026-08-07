package com.d1onix.dishlab.feature.home.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val savedCount: Int = 0,
    val historyCount: Int = 0,
    val profileInitials: String = "AK",
    val isAuthenticated: Boolean = false,
)

sealed interface HomeAction {
    data object ScanClicked : HomeAction
    data object SavedClicked : HomeAction
    data object HistoryClicked : HomeAction
    data object ProfileClicked : HomeAction
    data object CompareClicked : HomeAction
    data object RecipesClicked : HomeAction
}
