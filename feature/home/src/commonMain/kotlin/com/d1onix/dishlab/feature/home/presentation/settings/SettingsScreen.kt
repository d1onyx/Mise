package com.d1onix.dishlab.feature.home.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.home.resources.Res
import com.d1onix.dishlab.feature.home.resources.profile_auto_connect
import com.d1onix.dishlab.feature.home.resources.profile_auto_connect_body
import com.d1onix.dishlab.feature.home.resources.profile_graph_settings
import com.d1onix.dishlab.feature.home.resources.profile_recipe_preferences
import com.d1onix.dishlab.feature.home.resources.profile_recipe_preferences_body
import com.d1onix.dishlab.feature.home.resources.profile_reduce_motion
import com.d1onix.dishlab.feature.home.resources.profile_reduce_motion_body
import com.d1onix.dishlab.feature.home.resources.profile_show_scores
import com.d1onix.dishlab.feature.home.resources.profile_show_scores_body
import com.d1onix.dishlab.feature.home.resources.settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(state, viewModel::onAction)
}

@Composable
internal fun SettingsContent(state: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().screenIn()) {
        MiseScreenHeader(
            title = stringResource(Res.string.settings_title),
            onBackClick = { onAction(SettingsAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionLabel(stringResource(Res.string.profile_graph_settings)) }
            item { SettingsToggle(stringResource(Res.string.profile_auto_connect), stringResource(Res.string.profile_auto_connect_body), state.autoConnectNewProducts) { onAction(SettingsAction.AutoConnectChanged(it)) } }
            item { SettingsToggle(stringResource(Res.string.profile_reduce_motion), stringResource(Res.string.profile_reduce_motion_body), state.reduceGraphMotion) { onAction(SettingsAction.ReduceMotionChanged(it)) } }
            item { SettingsToggle(stringResource(Res.string.profile_show_scores), stringResource(Res.string.profile_show_scores_body), state.showProductScores) { onAction(SettingsAction.ShowScoresChanged(it)) } }
            item {
                MisePanel(Modifier.fillMaxWidth(), cornerRadius = 8, onClick = { onAction(SettingsAction.RecipePreferencesClicked) }, contentPadding = PaddingValues(14.dp)) {
                    Column { Text(stringResource(Res.string.profile_recipe_preferences), style = MiseTheme.typography.body, color = MiseTheme.colors.text); Text(stringResource(Res.string.profile_recipe_preferences_body), style = MiseTheme.typography.bodySmall, color = MiseTheme.colors.textMuted) }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MiseTheme.colors
    MisePanel(Modifier.fillMaxWidth(), cornerRadius = 8, onClick = { onCheckedChange(!checked) }, singleUse = false, contentPadding = PaddingValues(14.dp)) {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { Text(title, style = MiseTheme.typography.body, color = colors.text); Text(body, style = MiseTheme.typography.bodySmall, color = colors.textMuted) }
            Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedThumbColor = colors.onLime, checkedTrackColor = colors.lime))
        }
    }
}
