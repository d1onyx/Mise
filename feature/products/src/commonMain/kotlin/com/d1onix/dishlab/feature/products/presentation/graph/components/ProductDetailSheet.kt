package com.d1onix.dishlab.feature.products.presentation.graph.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MiseGradeScoreScale
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseTabPager
import com.d1onix.dishlab.designsystem.component.ScoreRing
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.VerdictBadge
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductPackagingComponent
import com.d1onix.dishlab.feature.products.presentation.graph.GraphAction
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.product_alternatives
import com.d1onix.dishlab.feature.products.resources.product_allergens
import com.d1onix.dishlab.feature.products.resources.product_brand
import com.d1onix.dishlab.feature.products.resources.product_categories
import com.d1onix.dishlab.feature.products.resources.product_cook
import com.d1onix.dishlab.feature.products.resources.product_eco_score
import com.d1onix.dishlab.feature.products.resources.product_eco_score_scale_hint
import com.d1onix.dishlab.feature.products.resources.product_image
import com.d1onix.dishlab.feature.products.resources.product_incomplete_data
import com.d1onix.dishlab.feature.products.resources.product_additives
import com.d1onix.dishlab.feature.products.resources.product_countries
import com.d1onix.dishlab.feature.products.resources.product_food_groups
import com.d1onix.dishlab.feature.products.resources.product_ingredients
import com.d1onix.dishlab.feature.products.resources.product_ingredients_detailed
import com.d1onix.dishlab.feature.products.resources.product_ingredients_photo
import com.d1onix.dishlab.feature.products.resources.product_labels
import com.d1onix.dishlab.feature.products.resources.product_manufacturing_places
import com.d1onix.dishlab.feature.products.resources.product_no_extra_photos
import com.d1onix.dishlab.feature.products.resources.product_nova
import com.d1onix.dishlab.feature.products.resources.product_nutrient_levels
import com.d1onix.dishlab.feature.products.resources.product_nutrients_per_100g
import com.d1onix.dishlab.feature.products.resources.product_nutri_score
import com.d1onix.dishlab.feature.products.resources.product_nutri_score_scale_hint
import com.d1onix.dishlab.feature.products.resources.product_nutrition_photo
import com.d1onix.dishlab.feature.products.resources.product_origins
import com.d1onix.dishlab.feature.products.resources.product_packaging
import com.d1onix.dishlab.feature.products.resources.product_packaging_photo
import com.d1onix.dishlab.feature.products.resources.product_purchase_places
import com.d1onix.dishlab.feature.products.resources.product_quantity
import com.d1onix.dishlab.feature.products.resources.product_remove
import com.d1onix.dishlab.feature.products.resources.product_serving_size
import com.d1onix.dishlab.feature.products.resources.product_stores
import com.d1onix.dishlab.feature.products.resources.product_tab_composition
import com.d1onix.dishlab.feature.products.resources.product_tab_nutrition
import com.d1onix.dishlab.feature.products.resources.product_tab_overview
import com.d1onix.dishlab.feature.products.resources.product_tab_photos
import com.d1onix.dishlab.feature.products.resources.product_traces
import org.jetbrains.compose.resources.stringResource
import coil3.compose.AsyncImage

/**
 * The product sheet: score, verdict, and everything OFF reports about the
 * product, organized into swipeable tabs — Overview stays a glance, the rest
 * is one tap or one swipe away for whoever wants it.
 *
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
    val tabs = listOf(
        stringResource(Res.string.product_tab_overview),
        stringResource(Res.string.product_tab_nutrition),
        stringResource(Res.string.product_tab_composition),
        stringResource(Res.string.product_tab_photos),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .background(
                Color(0xFF0C0E14).copy(alpha = 0.94f),
                RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            )
            .border(
                width = 1.dp,
                color = colors.border,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            ),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScoreRing(score = product.score, color = accent, size = 64, strokeWidth = 6) {
                        Text(
                            text = product.score.toString(),
                            style = MiseTheme.typography.mono,
                            color = accent,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    VerdictBadge(label = product.verdict.label, color = accent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    Text(
                        text = product.name,
                        style = MiseTheme.typography.title,
                        color = colors.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        product.category,
                        style = MiseTheme.typography.monoSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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

            MiseTabPager(tabs = tabs, modifier = Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (page) {
                        0 -> OverviewTab(product, card)
                        1 -> NutritionTab(card)
                        2 -> CompositionTab(card)
                        else -> PhotosTab(product, card)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 42.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MisePrimaryButton(
                text = stringResource(Res.string.product_cook),
                onClick = { onAction(GraphAction.FindRecipesClicked) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
            )
            MiseGhostButton(
                text = stringResource(Res.string.product_remove),
                onClick = { onAction(GraphAction.RemoveClicked(product.id)) },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
            )
        }
    }
}

@Composable
private fun OverviewTab(product: Product, card: ProductDetailCardModel) {
    val colors = MiseTheme.colors

    product.summary.takeIf(String::isNotBlank)?.let { DetailTile(text = it) }
    card.imageUrl?.let { imageUrl ->
        val imageShape = RoundedCornerShape(18.dp)
        Column(
            Modifier.fillMaxWidth().padding(bottom = 8.dp)
                .background(colors.panel, RoundedCornerShape(14.dp))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(10.dp),
        ) {
            Text(stringResource(Res.string.product_image).uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(204.dp)
                    .clip(imageShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            }
        }
    }
    card.facts.forEach { fact ->
        when (fact.kind) {
            ProductDetailFactKind.NutriScore -> GradeScoreTile(
                selectedGrade = fact.value,
                label = stringResource(Res.string.product_nutri_score),
                hint = stringResource(Res.string.product_nutri_score_scale_hint),
            )
            ProductDetailFactKind.EcoScore -> GradeScoreTile(
                selectedGrade = fact.value,
                label = stringResource(Res.string.product_eco_score),
                hint = stringResource(Res.string.product_eco_score_scale_hint),
            )
            ProductDetailFactKind.Nova -> Unit // shown on the Nutrition tab instead
            else -> DetailTile(title = fact.kind.label(), text = fact.value)
        }
    }
    card.ingredients?.let { DetailTile(title = stringResource(Res.string.product_ingredients), text = it) }
    card.allergens.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_allergens), text = it.joinToString())
    }

    if (product.alternatives.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        SectionLabel(text = stringResource(Res.string.product_alternatives), color = colors.textMuted)
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
}

@Composable
private fun NutritionTab(card: ProductDetailCardModel) {
    val colors = MiseTheme.colors

    card.facts.firstOrNull { it.kind == ProductDetailFactKind.Nova }?.let { fact ->
        DetailTile(title = "NOVA ${fact.value}", text = fact.value.novaExplanation())
    }
    card.nutrientLevels.takeIf(List<*>::isNotEmpty)?.let { levels ->
        DetailTile(
            title = stringResource(Res.string.product_nutrient_levels),
            text = levels.joinToString { (name, level) -> "$name: $level" },
        )
    }
    if (card.nutrients.isNotEmpty()) {
        SectionLabel(text = stringResource(Res.string.product_nutrients_per_100g), color = colors.textMuted)
        Spacer(Modifier.height(8.dp))
        NutrientGrid(card.nutrients)
    }
}

@Composable
private fun CompositionTab(card: ProductDetailCardModel) {
    card.ingredientsBreakdown?.let {
        DetailTile(title = stringResource(Res.string.product_ingredients_detailed), text = it)
    }
    card.traces.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_traces), text = it.joinToString())
    }
    card.additives.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_additives), text = it.joinToString())
    }
    card.categories.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_categories), text = it.joinToString())
    }
    card.labels.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_labels), text = it.joinToString())
    }
    card.foodGroups.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_food_groups), text = it.joinToString())
    }
    card.countries.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_countries), text = it.joinToString())
    }
    card.origins.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_origins), text = it.joinToString())
    }
    card.manufacturingPlaces.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_manufacturing_places), text = it.joinToString())
    }
    card.purchasePlaces.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_purchase_places), text = it.joinToString())
    }
    card.stores.takeIf(List<String>::isNotEmpty)?.let {
        DetailTile(title = stringResource(Res.string.product_stores), text = it.joinToString())
    }
    card.packaging.takeIf(List<*>::isNotEmpty)?.let { components ->
        DetailTile(
            title = stringResource(Res.string.product_packaging),
            text = components.joinToString { it.summary() },
        )
    }
}

@Composable
private fun PhotosTab(product: Product, card: ProductDetailCardModel) {
    val photos = listOfNotNull(
        card.ingredientsImageUrl?.let { Res.string.product_ingredients_photo to it },
        card.nutritionImageUrl?.let { Res.string.product_nutrition_photo to it },
        card.packagingImageUrl?.let { Res.string.product_packaging_photo to it },
    )
    if (photos.isEmpty()) {
        EmptyState(text = stringResource(Res.string.product_no_extra_photos))
        return
    }
    photos.forEach { (label, url) ->
        ProductPhotoTile(title = stringResource(label), imageUrl = url, contentDescription = product.name)
    }
}

@Composable
private fun NutrientGrid(nutrients: List<Nutrient>) {
    val colors = MiseTheme.colors
    nutrients.chunked(3).forEach { row ->
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
}

@Composable
private fun GradeScoreTile(selectedGrade: String, label: String, hint: String) {
    val colors = MiseTheme.colors

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.panel, RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        MiseGradeScoreScale(
            selectedGrade = selectedGrade,
            label = label,
            hint = hint,
        )
    }
}

private fun ProductPackagingComponent.summary(): String = buildString {
    numberOfUnits?.takeIf { it > 0 }?.let { append(it).append("× ") }
    append(listOf(shape, material, recycling).filter(String::isNotBlank).joinToString(" · "))
    quantityPerUnit.takeIf(String::isNotBlank)?.let { append(" (").append(it).append(')') }
}.trim()

private fun String.novaExplanation(): String = when (this) {
    "1" -> "Unprocessed or minimally processed food"
    "2" -> "Processed culinary ingredient"
    "3" -> "Processed food"
    "4" -> "Ultra-processed food"
    else -> ""
}

@Composable
private fun ProductPhotoTile(title: String, imageUrl: String, contentDescription: String) {
    val colors = MiseTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .background(colors.panel, RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Text(title.uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(12.dp),
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
