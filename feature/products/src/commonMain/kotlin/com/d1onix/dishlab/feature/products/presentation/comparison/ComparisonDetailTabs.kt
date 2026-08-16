package com.d1onix.dishlab.feature.products.presentation.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.component.MiseTabPager
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.feature.products.presentation.graph.components.ProductDetailCardModel
import com.d1onix.dishlab.feature.products.presentation.graph.components.novaExplanation
import com.d1onix.dishlab.feature.products.presentation.graph.components.summary
import com.d1onix.dishlab.feature.products.presentation.graph.components.toDetailCardModel
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.comparison_nutrients_per
import com.d1onix.dishlab.feature.products.resources.comparison_score
import com.d1onix.dishlab.feature.products.resources.comparison_summary
import com.d1onix.dishlab.feature.products.resources.product_additives
import com.d1onix.dishlab.feature.products.resources.product_allergens
import com.d1onix.dishlab.feature.products.resources.product_alternatives
import com.d1onix.dishlab.feature.products.resources.product_brand
import com.d1onix.dishlab.feature.products.resources.product_categories
import com.d1onix.dishlab.feature.products.resources.product_compared_to_category
import com.d1onix.dishlab.feature.products.resources.product_countries
import com.d1onix.dishlab.feature.products.resources.product_eco_score
import com.d1onix.dishlab.feature.products.resources.product_expiration_date
import com.d1onix.dishlab.feature.products.resources.product_food_classification
import com.d1onix.dishlab.feature.products.resources.product_ingredients
import com.d1onix.dishlab.feature.products.resources.product_ingredients_detailed
import com.d1onix.dishlab.feature.products.resources.product_labels
import com.d1onix.dishlab.feature.products.resources.product_manufacturing_places
import com.d1onix.dishlab.feature.products.resources.product_not_set
import com.d1onix.dishlab.feature.products.resources.product_nova
import com.d1onix.dishlab.feature.products.resources.product_nutri_score
import com.d1onix.dishlab.feature.products.resources.product_nutri_score_version
import com.d1onix.dishlab.feature.products.resources.product_origins
import com.d1onix.dishlab.feature.products.resources.product_packaging
import com.d1onix.dishlab.feature.products.resources.product_purchase_places
import com.d1onix.dishlab.feature.products.resources.product_quantity
import com.d1onix.dishlab.feature.products.resources.product_serving_size
import com.d1onix.dishlab.feature.products.resources.product_stores
import com.d1onix.dishlab.feature.products.resources.product_tab_composition
import com.d1onix.dishlab.feature.products.resources.product_tab_nutrition
import com.d1onix.dishlab.feature.products.resources.product_tab_overview
import com.d1onix.dishlab.feature.products.resources.product_traces
import org.jetbrains.compose.resources.stringResource

private const val LABEL_WEIGHT = 0.34f
private const val VALUES_WEIGHT = 0.66f
private val LOWER_IS_BETTER = listOf("sugar", "salt", "sodium", "saturated", "fat", "cholesterol", "energy", "calor")
private val HIGHER_IS_BETTER = listOf("protein", "fiber", "fibre")

private data class ComparedDetail(val product: Product, val card: ProductDetailCardModel)

private data class ComparisonFact(
    val label: String,
    val value: (ComparedDetail) -> String,
)

/** Full product details in the same Overview, Nutrition and Composition groups as one product. */
@Composable
internal fun ComparisonDetailTabs(products: List<Product>, modifier: Modifier = Modifier) {
    val details = products.map { ComparedDetail(it, it.toDetailCardModel()) }
    val tabs = listOf(
        stringResource(Res.string.product_tab_overview),
        stringResource(Res.string.product_tab_nutrition),
        stringResource(Res.string.product_tab_composition),
    )

    MiseTabPager(tabs = tabs, modifier = modifier) { page ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (page) {
                0 -> OverviewComparison(details)
                1 -> NutritionComparison(details)
                else -> CompositionComparison(details)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverviewComparison(details: List<ComparedDetail>) {
    val placeholder = stringResource(Res.string.product_not_set)
    ComparisonFactsPanel(
        details = details,
        placeholder = placeholder,
        facts = listOf(
            ComparisonFact(stringResource(Res.string.comparison_score)) { it.product.score.toString() },
            ComparisonFact(stringResource(Res.string.comparison_summary)) { it.product.summary },
            ComparisonFact(stringResource(Res.string.product_nutri_score)) { it.card.nutriScoreGrade },
            ComparisonFact(stringResource(Res.string.product_eco_score)) { it.card.ecoScoreGrade },
            ComparisonFact(stringResource(Res.string.product_brand)) { it.card.brand },
            ComparisonFact(stringResource(Res.string.product_quantity)) { it.card.quantity },
            ComparisonFact(stringResource(Res.string.product_serving_size)) { it.card.servingSize },
            ComparisonFact(stringResource(Res.string.product_expiration_date)) { it.card.expirationDate },
            ComparisonFact(stringResource(Res.string.product_ingredients)) { it.card.ingredients },
            ComparisonFact(stringResource(Res.string.product_allergens)) { it.card.allergens.joinToString() },
            ComparisonFact(stringResource(Res.string.product_alternatives)) {
                it.product.alternatives.joinToString { alternative -> "${alternative.name} (${alternative.score})" }
            },
        ),
        bestProductId = details.maxByOrNull { it.product.score }?.product?.id,
        bestLabel = stringResource(Res.string.comparison_score),
    )
}

@Composable
private fun NutritionComparison(details: List<ComparedDetail>) {
    val placeholder = stringResource(Res.string.product_not_set)
    val levelNames = details.flatMap { it.card.nutrientLevels.map(Pair<String, String>::first) }.distinct()
    ComparisonFactsPanel(
        details = details,
        placeholder = placeholder,
        facts = listOf(
            ComparisonFact(stringResource(Res.string.product_nova)) {
                it.card.novaGroup?.let { group -> "$group · ${group.novaExplanation()}" }.orEmpty()
            },
            ComparisonFact(stringResource(Res.string.product_nutri_score_version)) { it.card.nutriScoreVersion },
            ComparisonFact(stringResource(Res.string.product_compared_to_category)) { it.card.comparedToCategory },
            ComparisonFact(stringResource(Res.string.comparison_nutrients_per)) { it.card.nutrientsPer },
        ) + levelNames.map { name ->
            ComparisonFact(name) { detail -> detail.card.nutrientLevels.firstOrNull { it.first == name }?.second.orEmpty() }
        },
    )
    Spacer(Modifier.height(16.dp))
    SectionLabel(text = stringResource(Res.string.product_tab_nutrition), color = MiseTheme.colors.textMuted)
    Spacer(Modifier.height(8.dp))
    NutrientComparisonPanel(details, placeholder)
}

@Composable
private fun CompositionComparison(details: List<ComparedDetail>) {
    val placeholder = stringResource(Res.string.product_not_set)
    ComparisonFactsPanel(
        details = details,
        placeholder = placeholder,
        facts = listOf(
            ComparisonFact(stringResource(Res.string.product_ingredients_detailed)) { it.card.ingredientsBreakdown },
            ComparisonFact(stringResource(Res.string.product_traces)) { it.card.traces.joinToString() },
            ComparisonFact(stringResource(Res.string.product_additives)) { it.card.additives.joinToString() },
            ComparisonFact(stringResource(Res.string.product_categories)) { it.card.categories.joinToString() },
            ComparisonFact(stringResource(Res.string.product_labels)) { it.card.labels.joinToString() },
            ComparisonFact(stringResource(Res.string.product_food_classification)) {
                listOf(it.card.pnnsGroup, it.card.pnnsSubgroup).filter(String::isNotBlank).joinToString(" · ")
                    .ifBlank { it.card.foodGroups.joinToString() }
            },
            ComparisonFact(stringResource(Res.string.product_countries)) { it.card.countries.joinToString() },
            ComparisonFact(stringResource(Res.string.product_origins)) { it.card.origins.joinToString().ifBlank { it.card.originNote } },
            ComparisonFact(stringResource(Res.string.product_manufacturing_places)) { it.card.manufacturingPlaces.joinToString() },
            ComparisonFact(stringResource(Res.string.product_purchase_places)) { it.card.purchasePlaces.joinToString() },
            ComparisonFact(stringResource(Res.string.product_stores)) { it.card.stores.joinToString() },
            ComparisonFact(stringResource(Res.string.product_packaging)) { it.card.packaging.joinToString { component -> component.summary() } },
        ),
    )
}

/** A compact, aligned table: every field remains visible for every compared product. */
@Composable
private fun ComparisonFactsPanel(
    details: List<ComparedDetail>,
    facts: List<ComparisonFact>,
    placeholder: String,
    bestProductId: ProductId? = null,
    bestLabel: String? = null,
) {
    val colors = MiseTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.panel, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
    ) {
        ComparisonHeaderRow(details)
        Divider()
        facts.forEachIndexed { index, fact ->
            if (index > 0) Divider()
            ComparisonFactRow(
                fact = fact,
                details = details,
                placeholder = placeholder,
                highlightProductId = if (fact.label == bestLabel) bestProductId else null,
            )
        }
    }
}

@Composable
private fun NutrientComparisonPanel(details: List<ComparedDetail>, placeholder: String) {
    val nutrients = details.flatMap { it.card.nutrients.map(Nutrient::name) }.distinct()
    val colors = MiseTheme.colors
    if (nutrients.isEmpty()) {
        Text(placeholder, style = MiseTheme.typography.bodySmall, color = colors.textMuted)
        return
    }
    Column(
        Modifier.fillMaxWidth().background(colors.panel, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
    ) {
        ComparisonHeaderRow(details)
        Divider()
        nutrients.forEachIndexed { index, name ->
            if (index > 0) Divider()
            ComparisonNutrientRow(name, details, placeholder)
        }
    }
}

@Composable
private fun ComparisonHeaderRow(details: List<ComparedDetail>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Spacer(Modifier.weight(LABEL_WEIGHT))
        Row(Modifier.weight(VALUES_WEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            details.forEach { detail ->
                Text(
                    text = detail.product.initial,
                    style = MiseTheme.typography.monoTiny,
                    color = Color(detail.product.accentColor),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ComparisonFactRow(
    fact: ComparisonFact,
    details: List<ComparedDetail>,
    placeholder: String,
    highlightProductId: ProductId?,
) {
    val colors = MiseTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Text(fact.label, style = MiseTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.weight(LABEL_WEIGHT))
        Row(Modifier.weight(VALUES_WEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            details.forEach { detail ->
                Text(
                    text = fact.value(detail).ifBlank { placeholder },
                    style = MiseTheme.typography.monoTiny,
                    color = if (detail.product.id == highlightProductId) colors.lime else colors.text,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ComparisonNutrientRow(name: String, details: List<ComparedDetail>, placeholder: String) {
    val colors = MiseTheme.colors
    val bestProductId = bestProductIdForNutrient(name, details)
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.Top) {
        Text(name, style = MiseTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.weight(LABEL_WEIGHT))
        Row(Modifier.weight(VALUES_WEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            details.forEach { detail ->
                val nutrient = detail.card.nutrients.firstOrNull { it.name == name }
                Text(
                    text = nutrient?.let { "${it.amount} ${it.unit}".trim() } ?: placeholder,
                    style = MiseTheme.typography.monoTiny,
                    color = if (detail.product.id == bestProductId) colors.lime else colors.text,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(MiseTheme.colors.border))

private fun bestProductIdForNutrient(name: String, details: List<ComparedDetail>): ProductId? {
    val lowerName = name.lowercase()
    val lowerIsBetter = when {
        LOWER_IS_BETTER.any(lowerName::contains) -> true
        HIGHER_IS_BETTER.any(lowerName::contains) -> false
        else -> return null
    }
    val values = details.mapNotNull { detail ->
        detail.card.nutrients.firstOrNull { it.name == name }?.amount?.toDoubleOrNull()?.let { detail.product.id to it }
    }
    if (values.size < 2) return null
    return if (lowerIsBetter) values.minBy { it.second }.first else values.maxBy { it.second }.first
}
