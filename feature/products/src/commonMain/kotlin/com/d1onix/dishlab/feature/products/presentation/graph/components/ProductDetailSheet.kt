package com.d1onix.dishlab.feature.products.presentation.graph.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.ScoreRing
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.VerdictBadge
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.feature.products.presentation.graph.GraphAction
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.product_alternatives
import com.d1onix.dishlab.feature.products.resources.product_cook
import com.d1onix.dishlab.feature.products.resources.product_incomplete_data
import com.d1onix.dishlab.feature.products.resources.product_remove
import org.jetbrains.compose.resources.stringResource

/**
 * The product sheet: score, verdict, nutrients and alternatives.
 * Its visual surface stays separate from the Material modal container, which
 * owns swipe-to-dismiss, nested scrolling and accessibility semantics.
 */
@Composable
fun ProductDetailSheet(
    product: Product,
    onAction: (GraphAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val accent = scoreColor(product.verdict)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFF0C0E14).copy(alpha = 0.94f),
                RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            )
            .border(
                width = 1.dp,
                color = colors.border,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            )
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 26.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(score = product.score, color = accent, size = 64, strokeWidth = 6) {
                Text(
                    text = product.score.toString(),
                    style = MiseTheme.typography.mono,
                    color = accent,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MiseTheme.typography.title, color = colors.text)
                Text(
                    product.category,
                    style = MiseTheme.typography.monoSmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            VerdictBadge(label = product.verdict.label, color = accent)
            Spacer(Modifier.width(8.dp))
            MiseGhostButton(
                text = stringResource(Res.string.product_remove),
                onClick = { onAction(GraphAction.RemoveClicked(product.id)) },
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
            )
        }

        if (!product.hasCompleteData) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.product_incomplete_data),
                style = MiseTheme.typography.monoSmall,
                color = colors.amber,
                modifier = Modifier
                    .background(colors.amber.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                    .border(1.dp, colors.amber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = product.summary,
            style = MiseTheme.typography.bodySmall,
            color = colors.text.copy(alpha = 0.8f),
        )

        Spacer(Modifier.height(16.dp))
        product.nutrients.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { nutrient ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(colors.panel, RoundedCornerShape(14.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                    ) {
                        Text(
                            nutrient.name.uppercase(),
                            style = MiseTheme.typography.monoTiny,
                            color = colors.textMuted,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                nutrient.amount,
                                style = MiseTheme.typography.titleSmall,
                                color = colors.text,
                            )
                            Text(
                                nutrient.unit,
                                style = MiseTheme.typography.monoTiny,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                            )
                        }
                    }
                }
                // Keep the last row aligned with a full grid.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        if (product.alternatives.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionLabel(
                text = stringResource(Res.string.product_alternatives),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            product.alternatives.forEach { alternative ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(colors.lime.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                        .border(1.dp, colors.lime.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(alternative.name, style = MiseTheme.typography.body, color = colors.text)
                    Text(
                        alternative.score.toString(),
                        style = MiseTheme.typography.mono,
                        color = scoreColor(alternative.verdict),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MisePrimaryButton(
                text = stringResource(Res.string.product_cook),
                onClick = { onAction(GraphAction.FindRecipesClicked) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
