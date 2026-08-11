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
import androidx.compose.ui.layout.ContentScale
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
import com.d1onix.dishlab.feature.products.resources.product_allergens
import com.d1onix.dishlab.feature.products.resources.product_brand
import com.d1onix.dishlab.feature.products.resources.product_categories
import com.d1onix.dishlab.feature.products.resources.product_cook
import com.d1onix.dishlab.feature.products.resources.product_device_fallback
import com.d1onix.dishlab.feature.products.resources.product_eco_score
import com.d1onix.dishlab.feature.products.resources.product_image
import com.d1onix.dishlab.feature.products.resources.product_incomplete_data
import com.d1onix.dishlab.feature.products.resources.product_ingredients
import com.d1onix.dishlab.feature.products.resources.product_labels
import com.d1onix.dishlab.feature.products.resources.product_not_in_database
import com.d1onix.dishlab.feature.products.resources.product_nova
import com.d1onix.dishlab.feature.products.resources.product_nutrients_per_100g
import com.d1onix.dishlab.feature.products.resources.product_nutri_score
import com.d1onix.dishlab.feature.products.resources.product_quantity
import com.d1onix.dishlab.feature.products.resources.product_remove
import com.d1onix.dishlab.feature.products.resources.product_serving_size
import org.jetbrains.compose.resources.stringResource
import coil3.compose.AsyncImage

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
    val card = product.toDetailCardModel()

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

        if (card.isDeviceFallback) {
            Spacer(Modifier.height(8.dp))
            DetailTile(text = stringResource(Res.string.product_device_fallback), accent = colors.amber)
        }

        Spacer(Modifier.height(16.dp))
        product.summary.takeIf(String::isNotBlank)?.let { DetailTile(text = it) }
        card.imageUrl?.let { imageUrl ->
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .background(colors.panel, RoundedCornerShape(14.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(10.dp),
            ) {
                Text(stringResource(Res.string.product_image).uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
                Spacer(Modifier.height(6.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(160.dp).background(colors.border, RoundedCornerShape(10.dp)),
                )
            }
        }
        card.facts.forEach { fact ->
            DetailTile(title = fact.kind.label(), text = fact.value)
        }
        card.ingredients?.let { DetailTile(title = stringResource(Res.string.product_ingredients), text = it) }
        card.allergens.takeIf(List<String>::isNotEmpty)?.let {
            DetailTile(title = stringResource(Res.string.product_allergens), text = it.joinToString())
        }
        card.categories.takeIf(List<String>::isNotEmpty)?.let {
            DetailTile(title = stringResource(Res.string.product_categories), text = it.joinToString())
        }
        card.labels.takeIf(List<String>::isNotEmpty)?.let {
            DetailTile(title = stringResource(Res.string.product_labels), text = it.joinToString())
        }

        if (card.notInDatabase) {
            DetailTile(text = stringResource(Res.string.product_not_in_database), accent = colors.amber)
        } else {
            Spacer(Modifier.height(12.dp))
            SectionLabel(text = stringResource(Res.string.product_nutrients_per_100g), color = colors.textMuted)
            Spacer(Modifier.height(8.dp))
        }
        card.nutrients.chunked(3).forEach { row ->
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

@Composable
private fun DetailTile(title: String? = null, text: String, accent: Color? = null) {
    val colors = MiseTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.panel, RoundedCornerShape(14.dp))
            .border(1.dp, accent?.copy(alpha = 0.35f) ?: colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        title?.let {
            Text(it.uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
            Spacer(Modifier.height(3.dp))
        }
        Text(text = text, style = MiseTheme.typography.bodySmall, color = accent ?: colors.text)
    }
}

@Composable
private fun ProductDetailFactKind.label(): String = when (this) {
    ProductDetailFactKind.Brand -> stringResource(Res.string.product_brand)
    ProductDetailFactKind.Quantity -> stringResource(Res.string.product_quantity)
    ProductDetailFactKind.ServingSize -> stringResource(Res.string.product_serving_size)
    ProductDetailFactKind.NutriScore -> stringResource(Res.string.product_nutri_score)
    ProductDetailFactKind.Nova -> stringResource(Res.string.product_nova)
    ProductDetailFactKind.EcoScore -> stringResource(Res.string.product_eco_score)
}
