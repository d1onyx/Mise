package com.d1onix.dishlab.feature.recipes.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.component.MiseTag
import com.d1onix.dishlab.designsystem.component.ProductAvatar
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.feature.recipes.presentation.difficultyLabel
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.recipes_minutes
import org.jetbrains.compose.resources.stringResource

/** Colour of a difficulty tag — the recipe equivalent of a product's verdict. */
@Composable
fun difficultyColor(difficulty: RecipeDifficulty): Color = when (difficulty) {
    RecipeDifficulty.Easy -> MiseTheme.colors.lime
    RecipeDifficulty.Medium -> MiseTheme.colors.amber
    RecipeDifficulty.Hard -> MiseTheme.colors.red
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    products: List<Product>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val accent = products.firstOrNull()?.let { Color(it.accentColor) } ?: colors.violet

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.panel)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.13f), colors.violet.copy(alpha = 0.13f)),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row {
                products.forEachIndexed { index, product ->
                    ProductAvatar(
                        initial = product.initial,
                        accent = Color(product.accentColor),
                        size = 44,
                        modifier = Modifier.offset(x = if (index == 0) 0.dp else (-12 * index).dp),
                    )
                }
            }
            Text(
                text = stringResource(Res.string.recipes_minutes, recipe.minutes),
                style = MiseTheme.typography.monoSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }

        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(recipe.name, style = MiseTheme.typography.titleSmall, color = colors.text)
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val difficultyTint = difficultyColor(recipe.difficulty)
                MiseTag(
                    text = difficultyLabel(recipe.difficulty),
                    color = difficultyTint,
                    background = difficultyTint.copy(alpha = 0.14f),
                )
                recipe.categories.forEach { category -> MiseTag(text = category) }
            }
        }
    }
}
