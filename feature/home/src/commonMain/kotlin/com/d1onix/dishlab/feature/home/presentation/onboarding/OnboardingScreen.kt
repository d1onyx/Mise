package com.d1onix.dishlab.feature.home.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.rememberSingleUseClick
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.AllergenPreference
import com.d1onix.dishlab.domain.model.DietPreference
import com.d1onix.dishlab.domain.model.KitchenEquipment
import com.d1onix.dishlab.domain.model.TastePreference
import com.d1onix.dishlab.feature.home.resources.Res
import com.d1onix.dishlab.feature.home.resources.onboarding_allergens
import com.d1onix.dishlab.feature.home.resources.onboarding_diets
import com.d1onix.dishlab.feature.home.resources.onboarding_done
import com.d1onix.dishlab.feature.home.resources.onboarding_equipment
import com.d1onix.dishlab.feature.home.resources.onboarding_intro_body
import com.d1onix.dishlab.feature.home.resources.onboarding_intro_title
import com.d1onix.dishlab.feature.home.resources.onboarding_next
import com.d1onix.dishlab.feature.home.resources.onboarding_not_now
import com.d1onix.dishlab.feature.home.resources.onboarding_optional
import com.d1onix.dishlab.feature.home.resources.onboarding_skip
import com.d1onix.dishlab.feature.home.resources.onboarding_start
import com.d1onix.dishlab.feature.home.resources.onboarding_tastes
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingContent(state, viewModel::onAction)
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showIntro) {
        OnboardingIntro(onAction, modifier)
        return
    }

    val options = sectionOptions(state)
    Column(
        modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp).screenIn(),
    ) {
        MiseScreenHeader(
            title = sectionTitle(state.section),
            onBackClick = { onAction(OnboardingAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        LinearProgressIndicator(
            progress = { state.step.toFloat() / state.totalSteps },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = MiseTheme.colors.lime,
            trackColor = MiseTheme.colors.panelStrong,
        )
        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(Res.string.onboarding_optional))
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { option ->
                PreferenceChip(
                    text = option.label,
                    selected = option.selected,
                    onClick = { onAction(OnboardingAction.OptionToggled(option.id)) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        MisePrimaryButton(
            text = stringResource(
                if (state.section == OnboardingSection.Equipment) {
                    Res.string.onboarding_done
                } else {
                    Res.string.onboarding_next
                },
            ),
            onClick = { onAction(OnboardingAction.NextClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
        MiseTextAction(
            text = stringResource(Res.string.onboarding_skip),
            onClick = { onAction(OnboardingAction.SkipClicked) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun OnboardingIntro(
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).screenIn(),
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(Res.string.onboarding_intro_title),
            style = MiseTheme.typography.display,
            color = MiseTheme.colors.text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.onboarding_intro_body),
            style = MiseTheme.typography.bodyLarge,
            color = MiseTheme.colors.textMuted,
        )
        Spacer(Modifier.weight(1f))
        MisePrimaryButton(
            text = stringResource(Res.string.onboarding_start),
            onClick = { onAction(OnboardingAction.StartClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
        MiseGhostButton(
            text = stringResource(Res.string.onboarding_not_now),
            onClick = { onAction(OnboardingAction.SkipClicked) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

private data class PreferenceOption(val id: String, val label: String, val selected: Boolean)

@Composable
private fun sectionOptions(state: OnboardingUiState): List<PreferenceOption> = when (state.section) {
    OnboardingSection.Diets -> DietPreference.entries.map {
        PreferenceOption(it.name, enumLabel(it.name), it in state.preferences.diets)
    }
    OnboardingSection.Allergens -> AllergenPreference.entries.map {
        PreferenceOption(it.name, enumLabel(it.name), it in state.preferences.allergens)
    }
    OnboardingSection.Tastes -> TastePreference.entries.map {
        PreferenceOption(it.name, enumLabel(it.name), it in state.preferences.tastes)
    }
    OnboardingSection.Equipment -> KitchenEquipment.entries.map {
        PreferenceOption(it.name, enumLabel(it.name), it in state.preferences.equipment)
    }
}

private fun enumLabel(value: String): String = value.replace(Regex("([a-z])([A-Z])"), "$1 $2")

@Composable
private fun sectionTitle(section: OnboardingSection): String = stringResource(
    when (section) {
        OnboardingSection.Diets -> Res.string.onboarding_diets
        OnboardingSection.Allergens -> Res.string.onboarding_allergens
        OnboardingSection.Tastes -> Res.string.onboarding_tastes
        OnboardingSection.Equipment -> Res.string.onboarding_equipment
    },
)

@Composable
private fun PreferenceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiseTheme.colors
    val shape = RoundedCornerShape(8.dp)
    val singleUseClick = rememberSingleUseClick(onClick = onClick)
    Text(
        text = text,
        style = MiseTheme.typography.body,
        color = if (selected) colors.onLime else colors.text,
        modifier = Modifier
            .alpha(if (singleUseClick.enabled) 1f else 0.45f)
            .clip(shape)
            .background(if (selected) colors.lime else colors.panel, shape)
            .border(1.dp, if (selected) colors.lime else colors.border, shape)
            .clickable(enabled = singleUseClick.enabled, onClick = singleUseClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    )
}
