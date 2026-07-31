package com.d1onix.dishlab.feature.home.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.MiseScreenHeader
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.home.resources.Res
import com.d1onix.dishlab.feature.home.resources.profile_auto_connect
import com.d1onix.dishlab.feature.home.resources.profile_auto_connect_body
import com.d1onix.dishlab.feature.home.resources.profile_graph_count
import com.d1onix.dishlab.feature.home.resources.profile_graph_settings
import com.d1onix.dishlab.feature.home.resources.profile_name
import com.d1onix.dishlab.feature.home.resources.profile_name_placeholder
import com.d1onix.dishlab.feature.home.resources.profile_personal
import com.d1onix.dishlab.feature.home.resources.profile_reduce_motion
import com.d1onix.dishlab.feature.home.resources.profile_reduce_motion_body
import com.d1onix.dishlab.feature.home.resources.profile_recipe_preferences
import com.d1onix.dishlab.feature.home.resources.profile_recipe_preferences_body
import com.d1onix.dishlab.feature.home.resources.profile_save
import com.d1onix.dishlab.feature.home.resources.profile_sign_out
import com.d1onix.dishlab.feature.home.resources.profile_show_scores
import com.d1onix.dishlab.feature.home.resources.profile_show_scores_body
import com.d1onix.dishlab.feature.home.resources.profile_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .screenIn(),
    ) {
        MiseScreenHeader(
            title = stringResource(Res.string.profile_title),
            onBackClick = { onAction(ProfileAction.BackClicked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .size(72.dp)
                            .background(colors.violet.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, colors.violet, CircleShape),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            state.initials,
                            style = MiseTheme.typography.title,
                            color = colors.violet,
                        )
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(
                            state.savedDisplayName,
                            style = MiseTheme.typography.title,
                            color = colors.text,
                        )
                        Text(
                            stringResource(
                                Res.string.profile_graph_count,
                                state.graphProductCount,
                                10,
                            ),
                            style = MiseTheme.typography.monoSmall,
                            color = colors.lime,
                        )
                    }
                }
            }

            item { SectionLabel(stringResource(Res.string.profile_personal)) }
            item {
                ProfileNameField(
                    value = state.displayName,
                    onValueChange = { onAction(ProfileAction.DisplayNameChanged(it)) },
                    onSave = { onAction(ProfileAction.DisplayNameSaved) },
                    canSave = state.canSaveName,
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                SectionLabel(stringResource(Res.string.profile_graph_settings))
            }
            item {
                SettingsToggleRow(
                    title = stringResource(Res.string.profile_auto_connect),
                    description = stringResource(Res.string.profile_auto_connect_body),
                    checked = state.autoConnectNewProducts,
                    onCheckedChange = { onAction(ProfileAction.AutoConnectChanged(it)) },
                )
            }
            item {
                SettingsToggleRow(
                    title = stringResource(Res.string.profile_reduce_motion),
                    description = stringResource(Res.string.profile_reduce_motion_body),
                    checked = state.reduceGraphMotion,
                    onCheckedChange = { onAction(ProfileAction.ReduceMotionChanged(it)) },
                )
            }
            item {
                SettingsToggleRow(
                    title = stringResource(Res.string.profile_show_scores),
                    description = stringResource(Res.string.profile_show_scores_body),
                    checked = state.showProductScores,
                    onCheckedChange = { onAction(ProfileAction.ShowScoresChanged(it)) },
                )
            }
            item {
                MisePanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 8,
                    onClick = { onAction(ProfileAction.RecipePreferencesClicked) },
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(Res.string.profile_recipe_preferences),
                            style = MiseTheme.typography.body,
                            color = colors.text,
                        )
                        Text(
                            stringResource(Res.string.profile_recipe_preferences_body),
                            style = MiseTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
            item {
                MiseTextAction(
                    text = stringResource(Res.string.profile_sign_out),
                    onClick = { onAction(ProfileAction.SignOutClicked) },
                    color = colors.red,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.profile_name),
                style = MiseTheme.typography.monoTiny,
                color = colors.textMuted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current
                        .merge(MiseTheme.typography.body)
                        .copy(color = colors.text),
                    cursorBrush = SolidColor(colors.lime),
                    decorationBox = { input ->
                        if (value.isEmpty()) {
                            Text(
                                stringResource(Res.string.profile_name_placeholder),
                                style = MiseTheme.typography.body,
                                color = colors.textMuted,
                            )
                        }
                        input()
                    },
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
                if (canSave) {
                    MiseTextAction(
                        text = stringResource(Res.string.profile_save),
                        onClick = onSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8,
        background = if (checked) colors.lime.copy(alpha = 0.06f) else colors.panel,
        borderColor = if (checked) colors.lime.copy(alpha = 0.28f) else colors.border,
        contentPadding = PaddingValues(14.dp),
        onClick = { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MiseTheme.typography.body, color = colors.text)
                Text(
                    description,
                    style = MiseTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onLime,
                    checkedTrackColor = colors.lime,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.panelStrong,
                    uncheckedBorderColor = colors.borderStrong,
                ),
            )
        }
    }
}
