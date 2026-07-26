package com.d1onix.dishlab.designsystem.preview

import com.d1onix.dishlab.designsystem.component.FilterGroup
import com.d1onix.dishlab.designsystem.component.FilterOption

/**
 * Fixtures the previews render, shared by every platform on purpose.
 *
 * The preview *declarations* are platform-specific — `androidMain` renders them
 * in the Studio panel, `iosMain` hands them to Xcode — but the data behind them
 * is common, so a difference between two platforms is always a rendering
 * difference and never a difference in what was fed in.
 */
internal val previewFilterGroups: List<FilterGroup> = listOf(
    FilterGroup(
        id = "difficulty",
        name = "Difficulty",
        options = listOf(
            FilterOption("easy", "Easy"),
            FilterOption("medium", "Medium"),
            FilterOption("hard", "Hard"),
        ),
        selected = setOf("easy"),
    ),
    FilterGroup(
        id = "category",
        name = "Category",
        options = listOf(FilterOption("breakfast", "Breakfast")),
        selected = emptySet(),
    ),
)

internal const val PREVIEW_TOAST_TEXT: String = "No more products in the catalogue"

internal const val PREVIEW_SEARCH_PLACEHOLDER: String = "Search recipes"
