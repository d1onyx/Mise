package com.d1onix.dishlab.feature.recipes.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseTag
import com.d1onix.dishlab.designsystem.component.ProductAvatar
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.feature.recipes.presentation.components.difficultyColor
import com.d1onix.dishlab.feature.recipes.presentation.difficultyLabel
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.recipe_back
import com.d1onix.dishlab.feature.recipes.resources.recipe_loading
import com.d1onix.dishlab.feature.recipes.resources.recipe_unavailable
import com.d1onix.dishlab.feature.recipes.resources.recipe_retry
import com.d1onix.dishlab.feature.recipes.resources.recipe_ingredients
import com.d1onix.dishlab.feature.recipes.resources.recipe_save
import com.d1onix.dishlab.feature.recipes.resources.recipe_start_cooking
import com.d1onix.dishlab.feature.recipes.resources.recipe_steps
import com.d1onix.dishlab.feature.recipes.resources.recipe_timer_minutes
import com.d1onix.dishlab.feature.recipes.resources.recipe_timer_seconds
import com.d1onix.dishlab.feature.recipes.resources.recipe_unsave
import com.d1onix.dishlab.feature.recipes.resources.recipes_minutes
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecipeDetailScreen(viewModel: RecipeDetailViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecipeDetailContent(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun RecipeDetailContent(
    state: RecipeDetailUiState,
    onAction: (RecipeDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val recipe = state.recipe
    if (recipe == null) {
        RecipeDetailUnavailableContent(state, onAction, modifier)
        return
    }

    Box(modifier.fillMaxSize().screenIn()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                (state.products.firstOrNull()?.let { Color(it.accentColor) }
                                    ?: colors.violet).copy(alpha = 0.2f),
                                colors.violet.copy(alpha = 0.2f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    state.products.forEachIndexed { index, product ->
                        ProductAvatar(
                            initial = product.initial,
                            accent = Color(product.accentColor),
                            size = 60,
                            fontSize = 22,
                            modifier = Modifier.offset(x = if (index == 0) 0.dp else (-14 * index).dp),
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiseIconCircleButton(
                        icon = MiseIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.recipe_back),
                        onClick = { onAction(RecipeDetailAction.BackClicked) },
                        background = Color.Black.copy(alpha = 0.35f),
                        borderColor = Color.Transparent,
                        tint = Color.White,
                    )
                    MiseIconCircleButton(
                        icon = MiseIcons.Bookmark,
                        contentDescription = if (state.isSaved) {
                            stringResource(Res.string.recipe_unsave)
                        } else {
                            stringResource(Res.string.recipe_save)
                        },
                        onClick = { onAction(RecipeDetailAction.SaveClicked) },
                        iconSize = 18,
                        tint = colors.lime,
                        background = if (state.isSaved) {
                            colors.lime.copy(alpha = 0.25f)
                        } else {
                            Color.Black.copy(alpha = 0.35f)
                        },
                        borderColor = Color.Transparent,
                    )
                }
            }

            Column(Modifier.padding(horizontal = 22.dp).padding(top = 20.dp, bottom = 120.dp)) {
                Text(recipe.name, style = MiseTheme.typography.headline, color = colors.text)

                Row(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiseTag(
                        text = stringResource(Res.string.recipes_minutes, recipe.minutes),
                        background = colors.panel,
                    )
                    val tint = difficultyColor(recipe.difficulty)
                    MiseTag(
                        text = difficultyLabel(recipe.difficulty),
                        color = tint,
                        background = tint.copy(alpha = 0.14f),
                    )
                    recipe.categories.take(1).forEach { MiseTag(text = it) }
                }

                Text(
                    text = recipe.description,
                    style = MiseTheme.typography.body,
                    color = colors.text.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 16.dp),
                )

                SectionLabel(
                    text = stringResource(Res.string.recipe_ingredients),
                    modifier = Modifier.padding(top = 24.dp),
                )
                Column(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                ) {
                    recipe.ingredients.forEachIndexed { index, ingredient ->
                        RecipeIngredientRow(index + 1, ingredient.name, ingredient.quantity)
                    }
                }

                SectionLabel(
                    text = stringResource(Res.string.recipe_steps, recipe.steps.size),
                    modifier = Modifier.padding(top = 24.dp),
                )
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    recipe.steps.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .border(1.dp, colors.border, RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MiseTheme.typography.mono,
                                    color = colors.lime,
                                )
                            }
                            Column {
                                Text(
                                    step.title,
                                    style = MiseTheme.typography.titleSmall,
                                    color = colors.text,
                                )
                                Text(
                                    step.description,
                                    style = MiseTheme.typography.bodySmall,
                                    color = colors.text.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                                val timer = step.timerSeconds
                                if (timer != null) {
                                    Text(
                                        text = if (timer >= 60) {
                                            stringResource(Res.string.recipe_timer_minutes, timer / 60)
                                        } else {
                                            stringResource(Res.string.recipe_timer_seconds, timer)
                                        },
                                        style = MiseTheme.typography.monoSmall,
                                        color = colors.cyan,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, colors.background, colors.background),
                    )
                )
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            MisePrimaryButton(
                text = stringResource(Res.string.recipe_start_cooking),
                onClick = { onAction(RecipeDetailAction.StartCookingClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RecipeIngredientRow(number: Int, name: String, quantity: String) {
    val colors = MiseTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.panel)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(colors.background, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MiseTheme.typography.monoTiny, color = colors.lime)
        }
        Column(Modifier.weight(1f)) {
            Text(text = name, style = MiseTheme.typography.body, color = colors.text)
            if (quantity.isNotBlank()) {
                Text(
                    text = quantity,
                    style = MiseTheme.typography.mono,
                    color = colors.lime,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .background(colors.background, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun RecipeDetailUnavailableContent(
    state: RecipeDetailUiState,
    onAction: (RecipeDetailAction) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        MiseScreenHeader(
            title = "",
            onBackClick = { onAction(RecipeDetailAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        EmptyState(
            text = stringResource(
                if (state.isLoading) Res.string.recipe_loading else Res.string.recipe_unavailable,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.loadError) {
            MisePrimaryButton(
                text = stringResource(Res.string.recipe_retry),
                onClick = { onAction(RecipeDetailAction.RetryClicked) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
    }
}
