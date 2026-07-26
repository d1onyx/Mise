package com.d1onix.dishlab.feature.home.presentation

/**
 * The states every platform's previews render.
 *
 * The preview *declarations* are platform-specific — `androidMain` has the
 * `@Preview` functions Studio renders, `iosMain` has the `UIViewController`s
 * Xcode hosts — but they feed off this one list, so anything that looks
 * different between the two platforms is a rendering difference and never a
 * difference in the input.
 */
internal object HomePreviewStates {

    val Default = HomeUiState(savedCount = 3, historyCount = 5)

    /** First launch: nothing saved, nothing scanned. */
    val Empty = HomeUiState()

    val Toast = HomeUiState(savedCount = 1, historyCount = 1, showProfileHint = true)
}
