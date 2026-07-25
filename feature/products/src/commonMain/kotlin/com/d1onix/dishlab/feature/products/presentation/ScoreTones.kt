package com.d1onix.dishlab.feature.products.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.designsystem.theme.ScoreTone
import com.d1onix.dishlab.designsystem.theme.forTone
import com.d1onix.dishlab.domain.model.ScoreVerdict

/** The domain verdict rendered in the design system's vocabulary. */
fun ScoreVerdict.tone(): ScoreTone = when (this) {
    ScoreVerdict.Buy -> ScoreTone.Positive
    ScoreVerdict.Maybe -> ScoreTone.Caution
    ScoreVerdict.Skip -> ScoreTone.Negative
}

@Composable
fun scoreColor(verdict: ScoreVerdict): Color = MiseTheme.colors.forTone(verdict.tone())
