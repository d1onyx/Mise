package com.d1onix.dishlab.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import com.d1onix.dishlab.designsystem.component.AmbientConstellation
import com.d1onix.dishlab.designsystem.component.FilterChipBar
import com.d1onix.dishlab.designsystem.component.MiseSearchField
import com.d1onix.dishlab.designsystem.component.MiseTabPager
import com.d1onix.dishlab.designsystem.component.MiseToast
import com.d1onix.dishlab.designsystem.theme.MiseTheme

@MiseComponentPreview
@Composable
private fun MiseToastPreview() {
    MiseTheme {
        MiseToast(text = PREVIEW_TOAST_TEXT)
    }
}

/** The drifting background needs the whole canvas to show anything. */
@MiseScreenPreviews
@Composable
private fun AmbientConstellationPreview() {
    MiseTheme {
        AmbientConstellation(Modifier.fillMaxSize())
    }
}

/** Empty and filled, because the placeholder and the value use different colours. */
@MiseWidthPreviews
@Composable
private fun MiseSearchFieldPreview() {
    MiseTheme {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
        }
    }
}

/** Collapsed: only the chips are visible. */
@MiseWidthPreviews
@Composable
private fun FilterChipBarPreview() {
    MiseTheme {
        FilterChipBar(
            groups = previewFilterGroups,
            expandedGroupId = null,
            onGroupClick = {},
            onOptionClick = { _, _ -> },
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

/** Expanded: the option panel drops down and is the part that clips when narrow. */
@MiseWidthPreviews
@Composable
private fun FilterChipBarExpandedPreview() {
    MiseTheme {
        FilterChipBar(
            groups = previewFilterGroups,
            expandedGroupId = "difficulty",
            onGroupClick = {},
            onOptionClick = { _, _ -> },
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

/** Fixed height here only because a preview has no surrounding sheet to bound it. */
@MiseWidthPreviews
@Composable
private fun MiseTabPagerPreview() {
    MiseTheme {
        MiseTabPager(
            tabs = listOf("Overview", "Nutrition", "Sourcing"),
            modifier = Modifier.height(160.dp).padding(vertical = 12.dp),
        ) { index ->
            Text("Page $index", style = MiseTheme.typography.body, color = MiseTheme.colors.text)
        }
    }
}
