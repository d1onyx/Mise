package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/** Displays a continuous A–E score bar and highlights the supplied grade. */
@Composable
fun MiseGradeScoreScale(
    selectedGrade: String,
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val grades = listOf("A", "B", "C", "D", "E")
    val gradeColors = listOf(
        colors.lime,
        colors.cyan,
        Color(0xFFFFC107),
        Color(0xFFFF8C00),
        colors.red,
    )
    val selected = selectedGrade.trim().uppercase()

    Column(modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MiseTheme.typography.monoTiny,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(9.dp)),
        ) {
            Row(Modifier.fillMaxSize()) {
                grades.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(gradeColors[index]),
                    )
                }
            }
            Row(Modifier.fillMaxSize()) {
                grades.forEachIndexed { index, grade ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (grade == selected) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .background(colors.backgroundDeep, CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = grade,
                                    style = MiseTheme.typography.monoSmall,
                                    color = gradeColors[index],
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            grades.forEachIndexed { index, grade ->
                Text(
                    text = grade,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    style = MiseTheme.typography.monoTiny,
                    color = gradeColors[index],
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = hint,
            style = MiseTheme.typography.monoTiny,
            color = colors.textMuted,
        )
    }
}
