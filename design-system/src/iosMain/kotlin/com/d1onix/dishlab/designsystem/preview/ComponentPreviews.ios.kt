package com.d1onix.dishlab.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.d1onix.dishlab.designsystem.component.AmbientConstellation
import com.d1onix.dishlab.designsystem.component.FilterChipBar
import com.d1onix.dishlab.designsystem.component.MiseSearchField
import com.d1onix.dishlab.designsystem.component.MiseToast
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import platform.UIKit.UIViewController

/**
 * iOS previews are `UIViewController`s, not annotations: there is no preview
 * renderer for Compose on iOS, so Xcode's SwiftUI `#Preview` canvas hosts the
 * real Compose controller instead (see `iosApp/iosApp/Previews.swift`).
 *
 * That changes what a preview is good for. Android gets one preview per state
 * because re-rendering is cheap; here every preview builds and boots the
 * framework, so iOS gets one gallery that answers the questions only a real
 * device can — how the fonts rasterise, and whether `safeDrawingPadding()`
 * clears the notch and the home indicator.
 */
fun MiseComponentGalleryController(): UIViewController = ComposeUIViewController {
    MiseTheme {
        Box(Modifier.fillMaxSize()) {
            AmbientConstellation(Modifier.fillMaxSize())
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MiseSearchField(
                    value = "",
                    onValueChange = {},
                    placeholder = PREVIEW_SEARCH_PLACEHOLDER,
                )
                MiseSearchField(
                    value = "oat",
                    onValueChange = {},
                    placeholder = PREVIEW_SEARCH_PLACEHOLDER,
                )
                FilterChipBar(
                    groups = previewFilterGroups,
                    expandedGroupId = "difficulty",
                    onGroupClick = {},
                    onOptionClick = { _, _ -> },
                )
            }
            MiseToast(text = PREVIEW_TOAST_TEXT)
        }
    }
}
