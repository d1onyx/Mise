package com.d1onix.dishlab.feature.recipes.presentation.cooking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.feature.recipes.presentation.previewBowl
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.cooking_back
import com.d1onix.dishlab.feature.recipes.resources.cooking_exit
import com.d1onix.dishlab.feature.recipes.resources.cooking_finish
import com.d1onix.dishlab.feature.recipes.resources.cooking_next
import com.d1onix.dishlab.feature.recipes.resources.cooking_step_counter
import com.d1onix.dishlab.feature.recipes.resources.cooking_timer_pause
import com.d1onix.dishlab.feature.recipes.resources.cooking_timer_resume
import com.d1onix.dishlab.feature.recipes.resources.cooking_timer_start
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CookingScreen(viewModel: CookingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CookingContent(state = state, onAction = viewModel::onAction)
}

/** Step-by-step cooking with the per-step countdown. */
@Composable
private fun CookingContent(
    state: CookingUiState,
    onAction: (CookingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val step = state.step ?: return

    Column(
        modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .screenIn(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.cooking_step_counter,
                    state.stepIndex + 1,
                    state.stepCount,
                ),
                style = MiseTheme.typography.monoSmall,
                color = colors.textMuted,
            )
            MiseIconCircleButton(
                icon = MiseIcons.Close,
                contentDescription = stringResource(Res.string.cooking_exit),
                onClick = { onAction(CookingAction.CloseClicked) },
                iconSize = 18,
                tint = colors.textMuted,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(state.stepCount) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index <= state.stepIndex) colors.lime else colors.border,
                            RoundedCornerShape(2.dp),
                        )
                )
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = (state.stepIndex + 1).toString().padStart(2, '0'),
                style = MiseTheme.typography.mono,
                color = colors.violet,
            )
            Text(
                text = step.title,
                style = MiseTheme.typography.display,
                color = colors.text,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = step.description,
                style = MiseTheme.typography.bodyLarge,
                color = colors.text.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 16.dp),
            )

            if (state.timerSeconds != null) {
                Row(
                    Modifier
                        .padding(top = 26.dp)
                        .fillMaxWidth()
                        .background(colors.panel, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = state.timerLabel,
                        style = MiseTheme.typography.monoDisplay,
                        color = if (state.isTimerRunning) colors.cyan else colors.text,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(
                        text = stringResource(
                            when (state.timerButton) {
                                TimerButton.Start -> Res.string.cooking_timer_start
                                TimerButton.Pause -> Res.string.cooking_timer_pause
                                TimerButton.Resume -> Res.string.cooking_timer_resume
                            }
                        ),
                        style = MiseTheme.typography.body,
                        color = if (state.isTimerRunning) colors.red else colors.onCyan,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(
                                if (state.isTimerRunning) {
                                    colors.red.copy(alpha = 0.15f)
                                } else {
                                    colors.cyan
                                },
                                RoundedCornerShape(13.dp),
                            )
                            .clickable { onAction(CookingAction.TimerClicked) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.stepIndex > 0) {
                MiseGhostButton(
                    text = stringResource(Res.string.cooking_back),
                    onClick = { onAction(CookingAction.PreviousClicked) },
                )
            }
            MisePrimaryButton(
                text = if (state.isLastStep) {
                    stringResource(Res.string.cooking_finish)
                } else {
                    stringResource(Res.string.cooking_next)
                },
                onClick = { onAction(CookingAction.NextClicked) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Preview
@Composable
private fun CookingContentPreview() {
    MiseTheme {
        CookingContent(state = CookingUiState(recipe = previewBowl), onAction = {})
    }
}

/** Timer counting down — the state that only exists a few seconds in the app. */
@Preview
@Composable
private fun CookingContentRunningTimerPreview() {
    MiseTheme {
        CookingContent(
            state = CookingUiState(
                recipe = previewBowl,
                remainingSeconds = 77,
                isTimerRunning = true,
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun CookingContentLastStepPreview() {
    MiseTheme {
        CookingContent(
            state = CookingUiState(recipe = previewBowl, stepIndex = previewBowl.steps.lastIndex),
            onAction = {},
        )
    }
}
