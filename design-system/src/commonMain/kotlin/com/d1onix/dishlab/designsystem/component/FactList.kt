package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/** One row of [MiseFactList]: a label and its value (blank when the source has none). */
data class MiseFact(val label: String, val value: String)

/**
 * A panel of label/value rows separated by hairline dividers — the layout a
 * nutrition-facts label or a settings screen uses, one continuous table, not
 * a stack of individually bordered cards.
 *
 * A blank [MiseFact.value] renders as [placeholder] instead of being skipped,
 * so a field OFF hasn't been filled in for this product still has a visible
 * slot — the list has the same shape whether the data exists or not.
 */
@Composable
fun MiseFactList(facts: List<MiseFact>, placeholder: String, modifier: Modifier = Modifier) {
    val colors = MiseTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
    ) {
        facts.forEachIndexed { index, fact ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            MiseFactRow(label = fact.label, value = fact.value, placeholder = placeholder)
        }
    }
}

@Composable
private fun MiseFactRow(label: String, value: String, placeholder: String) {
    val colors = MiseTheme.colors
    val hasValue = value.isNotBlank()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MiseTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = if (hasValue) value else placeholder,
            style = MiseTheme.typography.bodySmall,
            color = if (hasValue) colors.text else colors.textFaint,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f),
        )
    }
}
