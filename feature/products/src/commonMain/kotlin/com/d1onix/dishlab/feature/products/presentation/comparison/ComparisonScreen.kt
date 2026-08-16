package com.d1onix.dishlab.feature.products.presentation.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.ScoreBadge
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.feature.products.resources.Res
import com.d1onix.dishlab.feature.products.resources.comparison_add
import com.d1onix.dishlab.feature.products.resources.comparison_add_selected
import com.d1onix.dishlab.feature.products.resources.comparison_best
import com.d1onix.dishlab.feature.products.resources.comparison_clear
import com.d1onix.dishlab.feature.products.resources.comparison_empty
import com.d1onix.dishlab.feature.products.resources.comparison_error
import com.d1onix.dishlab.feature.products.resources.comparison_loading
import com.d1onix.dishlab.feature.products.resources.comparison_nutrition
import com.d1onix.dishlab.feature.products.resources.comparison_remove
import com.d1onix.dishlab.feature.products.resources.comparison_select_hint
import com.d1onix.dishlab.feature.products.resources.comparison_select_all
import com.d1onix.dishlab.feature.products.resources.comparison_clear_selection
import com.d1onix.dishlab.feature.products.resources.comparison_title
import com.d1onix.dishlab.feature.products.presentation.scoreColor
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ComparisonContent(state, viewModel::onAction)
}

@Composable
internal fun ComparisonContent(
    state: ComparisonUiState,
    onAction: (ComparisonAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().safeDrawingPadding().screenIn()) {
        MiseScreenHeader(
            title = stringResource(Res.string.comparison_title),
            onBackClick = { onAction(ComparisonAction.BackClicked) },
            trailing = {
                if (state.products.isNotEmpty()) {
                    MiseTextAction(
                        text = stringResource(Res.string.comparison_clear),
                        onClick = { onAction(ComparisonAction.ClearClicked) },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        if (state.isLoading) {
            EmptyState(stringResource(Res.string.comparison_loading), Modifier.weight(1f).padding(24.dp))
            return
        }
        if (state.hasError) {
            EmptyState(stringResource(Res.string.comparison_error), Modifier.weight(1f).padding(24.dp))
            return
        }
        if (state.products.isEmpty()) {
            EmptyState(
                text = stringResource(Res.string.comparison_empty),
                modifier = Modifier.weight(1f).padding(24.dp),
            )
            MisePrimaryButton(
                text = stringResource(Res.string.comparison_add),
                onClick = { onAction(ComparisonAction.AddProductClicked) },
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.comparison_select_hint),
                style = MiseTheme.typography.bodySmall,
                color = MiseTheme.colors.textMuted,
                modifier = Modifier.weight(1f),
            )
            MiseTextAction(
                text = stringResource(
                    if (state.allSelected) {
                        Res.string.comparison_clear_selection
                    } else {
                        Res.string.comparison_select_all
                    },
                ),
                onClick = { onAction(ComparisonAction.SelectAllToggled) },
                color = MiseTheme.colors.lime,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.products, key = { it.id.value }) { product ->
                ComparedProductCard(
                    product = product,
                    selected = product.id in state.selectedForGraph,
                    best = product.id == state.bestProductId,
                    onToggle = { onAction(ComparisonAction.ProductSelectionToggled(product.id)) },
                    onRemove = { onAction(ComparisonAction.ProductRemoved(product.id)) },
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SectionLabel(stringResource(Res.string.comparison_nutrition))
            Spacer(Modifier.height(10.dp))
            NutrientComparisonPanel(state.products)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.canAddProduct) {
                MiseGhostButton(
                    text = stringResource(Res.string.comparison_add),
                    onClick = { onAction(ComparisonAction.AddProductClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
            MisePrimaryButton(
                text = stringResource(
                    Res.string.comparison_add_selected,
                    state.selectedForGraph.size,
                ),
                onClick = { onAction(ComparisonAction.AddSelectedToGraphClicked) },
                enabled = state.selectedForGraph.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ComparedProductCard(
    product: Product,
    selected: Boolean,
    best: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = Modifier.width(144.dp),
        cornerRadius = 8,
        background = if (selected) colors.lime.copy(alpha = 0.08f) else colors.panel,
        borderColor = if (selected) colors.lime else colors.border,
        contentPadding = PaddingValues(12.dp),
        onClick = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreBadge(product.score, scoreColor(product.verdict))
                if (best) Text(
                    stringResource(Res.string.comparison_best),
                    style = MiseTheme.typography.monoTiny,
                    color = colors.lime,
                )
            }
            Text(product.name, style = MiseTheme.typography.titleSmall, color = colors.text)
            Text(product.category, style = MiseTheme.typography.monoTiny, color = colors.textMuted)
            MiseTextAction(
                text = stringResource(Res.string.comparison_remove),
                onClick = onRemove,
                color = colors.red,
            )
        }
    }
}

/**
 * Nutrients side by side as one continuous divided table (the [MiseFactList]
 * pattern) instead of a separate bordered card per nutrient — the previous
 * layout turned into a wall of identical cards once three or more products
 * were compared.
 */
@Composable
private fun NutrientComparisonPanel(products: List<Product>, modifier: Modifier = Modifier) {
    val colors = MiseTheme.colors
    val names = nutrientNames(products)
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
    ) {
        NutrientHeaderRow(products)
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        names.forEachIndexed { index, name ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            NutrientValuesRow(name, products)
        }
    }
}

@Composable
private fun NutrientHeaderRow(products: List<Product>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Spacer(Modifier.weight(NUTRIENT_LABEL_WEIGHT))
        Row(Modifier.weight(NUTRIENT_VALUES_WEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            products.forEach { product ->
                Text(
                    text = product.initial,
                    style = MiseTheme.typography.monoTiny,
                    color = Color(product.accentColor),
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
private fun NutrientValuesRow(name: String, products: List<Product>) {
    val colors = MiseTheme.colors
    val bestId = bestProductIdForNutrient(name, products)
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Text(
            text = name,
            style = MiseTheme.typography.bodySmall,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(NUTRIENT_LABEL_WEIGHT),
        )
        Row(Modifier.weight(NUTRIENT_VALUES_WEIGHT), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            products.forEach { product ->
                val nutrient = product.nutrients.firstOrNull { it.name == name }
                val isBest = nutrient != null && product.id == bestId
                Text(
                    text = nutrient?.let { "${it.amount}${it.unit}" } ?: "—",
                    style = MiseTheme.typography.monoTiny,
                    color = if (isBest) colors.lime else colors.text,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private const val NUTRIENT_LABEL_WEIGHT = 0.34f
private const val NUTRIENT_VALUES_WEIGHT = 0.66f

/** Substrings of nutrient names DishLab's data sources use, lower-cased. */
private val LOWER_IS_BETTER = listOf("sugar", "salt", "sodium", "saturated", "fat", "cholesterol", "energy", "calor")
private val HIGHER_IS_BETTER = listOf("protein", "fiber", "fibre")

/**
 * Which product wins a nutrient row — the actual "compare intelligently" part
 * of this screen. Sugar/salt/fat/sodium/energy are better lower, protein and
 * fiber are better higher; anything else (unrecognised nutrient names) is left
 * unhighlighted rather than guessed at.
 */
private fun bestProductIdForNutrient(name: String, products: List<Product>): ProductId? {
    val lowerName = name.lowercase()
    val directionIsLower = when {
        LOWER_IS_BETTER.any { lowerName.contains(it) } -> true
        HIGHER_IS_BETTER.any { lowerName.contains(it) } -> false
        else -> return null
    }
    val values = products.mapNotNull { product ->
        product.nutrients.firstOrNull { it.name == name }?.amount?.toDoubleOrNull()?.let { product.id to it }
    }
    if (values.size < 2) return null
    return if (directionIsLower) values.minBy { it.second }.first else values.maxBy { it.second }.first
}

private fun nutrientNames(products: List<Product>): List<String> =
    products.flatMap { product -> product.nutrients.map { it.name } }.distinct().take(12)
