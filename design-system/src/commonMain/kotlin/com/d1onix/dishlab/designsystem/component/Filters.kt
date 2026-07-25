package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Search box used on the Recipes and Saved screens. */
@Composable
fun MiseSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Row(
        modifier = modifier
            .background(colors.panel, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(MiseIcons.Search, null, Modifier.size(17.dp), tint = colors.textMuted)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MiseTheme.typography.body, color = colors.textMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(MiseTheme.typography.body).copy(color = colors.text),
                cursorBrush = SolidColor(colors.lime),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * One filter group: a chip that expands into its options.
 *
 * [id] is what the callbacks report — stable and translation-independent, so the
 * caller matches on it instead of on the displayed [name].
 */
data class FilterGroup(
    val id: String,
    val name: String,
    val options: List<FilterOption>,
    /** Ids of the selected options. */
    val selected: Set<String>,
)

/** One option: [id] identifies it, [label] is what the user reads. */
data class FilterOption(
    val id: String,
    val label: String,
)

/**
 * The horizontally scrollable chip bar with an expandable option panel — the
 * prototype's `chipBar`. Selection state is hoisted; this only renders it.
 */
@Composable
fun FilterChipBar(
    groups: List<FilterGroup>,
    expandedGroupId: String?,
    onGroupClick: (groupId: String) -> Unit,
    onOptionClick: (groupId: String, optionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            groups.forEach { group ->
                val active = group.selected.isNotEmpty()
                val expanded = expandedGroupId == group.id
                Row(
                    modifier = Modifier
                        .background(
                            if (active) colors.lime.copy(alpha = 0.14f) else colors.panel,
                            RoundedCornerShape(14.dp),
                        )
                        .border(
                            1.dp,
                            if (active) colors.lime.copy(alpha = 0.4f) else colors.border,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onGroupClick(group.id) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = group.name,
                        style = MiseTheme.typography.bodySmall,
                        color = if (active) colors.lime else colors.text,
                    )
                    if (active) {
                        Text(
                            text = "(${group.selected.size})",
                            style = MiseTheme.typography.monoSmall,
                            color = colors.lime.copy(alpha = 0.8f),
                        )
                    }
                    Icon(
                        imageVector = MiseIcons.ChevronDown,
                        contentDescription = null,
                        tint = if (active) colors.lime else colors.textMuted,
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                    )
                }
            }
        }

        val group = groups.firstOrNull { it.id == expandedGroupId }
        if (group != null) {
            FlowRow(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .background(colors.panel, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.options.forEach { option ->
                    val on = option.id in group.selected
                    Text(
                        text = option.label,
                        style = MiseTheme.typography.bodySmall,
                        color = if (on) colors.onLime else colors.text,
                        modifier = Modifier
                            .background(
                                if (on) colors.lime else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .border(
                                1.dp,
                                if (on) colors.lime else colors.border,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onOptionClick(group.id, option.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MiseSearchFieldPreview() {
    MiseTheme {
        Column {
            MiseSearchField(value = "", onValueChange = {}, placeholder = "Search recipes")
            MiseSearchField(value = "oat", onValueChange = {}, placeholder = "Search recipes")
        }
    }
}

@Preview
@Composable
private fun FilterChipBarPreview() {
    MiseTheme {
        FilterChipBar(
            groups = listOf(
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
            ),
            expandedGroupId = "difficulty",
            onGroupClick = {},
            onOptionClick = { _, _ -> },
        )
    }
}
