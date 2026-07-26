package com.d1onix.dishlab.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.ScoreRing
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.VerdictBadge
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/**
 * A preview that lives in the Android application module.
 *
 * It is here as a control: this module is a plain `com.android.application`, so
 * if this renders while the previews in the feature modules do not, the problem
 * is the KMP module type, not the SDK or the theme.
 */
@Preview(showBackground = true, backgroundColor = 0xFF07080C, widthDp = 360)
@Composable
private fun MiseComponentsPreview() {
    MiseTheme {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Scan · Understand · Cook")
            MisePrimaryButton(text = "Capture scan", onClick = {}, modifier = Modifier.fillMaxWidth())
            MiseGhostButton(text = "Enter barcode manually", onClick = {}, modifier = Modifier.fillMaxWidth())
            VerdictBadge(label = "Buy", color = MiseTheme.colors.lime)
            ScoreRing(score = 82, color = MiseTheme.colors.lime)
        }
    }
}
