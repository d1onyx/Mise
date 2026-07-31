package com.d1onix.dishlab.feature.home.presentation

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.rememberPulse
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.AmbientConstellation
import com.d1onix.dishlab.designsystem.component.MiseCircleButton
import com.d1onix.dishlab.designsystem.component.MisePanel
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.home.resources.Res
import com.d1onix.dishlab.feature.home.resources.home_headline
import com.d1onix.dishlab.feature.home.resources.home_compare_subtitle
import com.d1onix.dishlab.feature.home.resources.home_compare_title
import com.d1onix.dishlab.feature.home.resources.home_discover_subtitle
import com.d1onix.dishlab.feature.home.resources.home_discover_title
import com.d1onix.dishlab.feature.home.resources.home_history_count
import com.d1onix.dishlab.feature.home.resources.home_history_title
import com.d1onix.dishlab.feature.home.resources.home_profile_action
import com.d1onix.dishlab.feature.home.resources.home_saved_empty
import com.d1onix.dishlab.feature.home.resources.home_saved_many
import com.d1onix.dishlab.feature.home.resources.home_saved_one
import com.d1onix.dishlab.feature.home.resources.home_saved_title
import com.d1onix.dishlab.feature.home.resources.home_scan_subtitle
import com.d1onix.dishlab.feature.home.resources.home_scan_title
import com.d1onix.dishlab.feature.home.resources.home_tagline
import com.d1onix.dishlab.feature.home.resources.home_wordmark
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors

    Box(modifier.fillMaxSize()) {
        AmbientConstellation()

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .screenIn(),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.home_wordmark),
                        style = MiseTheme.typography.headline,
                        color = colors.text,
                    )
                    Spacer(Modifier.size(8.dp))
                    GlowDot()
                }
                MiseCircleButton(
                    onClick = { onAction(HomeAction.ProfileClicked) },
                    size = 40,
                ) {
                    Text(
                        text = state.profileInitials,
                        style = MiseTheme.typography.mono,
                        color = colors.textMuted,
                    )
                }
            }

            Spacer(Modifier.height(34.dp))
            SectionLabel(stringResource(Res.string.home_tagline))
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.home_headline),
                style = MiseTheme.typography.display,
                color = colors.text,
            )

            Spacer(Modifier.height(38.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ScanCard(onClick = { onAction(HomeAction.ScanClicked) })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomeFeatureCard(
                        title = stringResource(Res.string.home_compare_title),
                        subtitle = stringResource(Res.string.home_compare_subtitle),
                        icon = MiseIcons.Barcode,
                        tint = colors.amber,
                        onClick = { onAction(HomeAction.CompareClicked) },
                        modifier = Modifier.weight(1f),
                    )
                    HomeFeatureCard(
                        title = stringResource(Res.string.home_discover_title),
                        subtitle = stringResource(Res.string.home_discover_subtitle),
                        icon = MiseIcons.Search,
                        tint = colors.violet,
                        onClick = { onAction(HomeAction.DiscoverRecipesClicked) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (state.isAuthenticated) MisePanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26,
                    onClick = { onAction(HomeAction.SavedClicked) },
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
                ) {
                    CardRow(
                        title = stringResource(Res.string.home_saved_title),
                        subtitle = savedLabel(state.savedCount),
                        icon = MiseIcons.Bookmark,
                        iconTint = colors.violet,
                        iconBackground = colors.violet.copy(alpha = 0.16f),
                    )
                }

                if (state.isAuthenticated) MisePanel(
                    modifier = Modifier.fillMaxWidth(),
                    background = Color.Transparent,
                    borderColor = colors.border,
                    onClick = { onAction(HomeAction.HistoryClicked) },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.home_history_title),
                            style = MiseTheme.typography.mono,
                            color = colors.textMuted,
                        )
                        Text(
                            text = stringResource(Res.string.home_history_count, state.historyCount),
                            style = MiseTheme.typography.monoSmall,
                            color = colors.lime,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = modifier,
        cornerRadius = 8,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
            Text(title, style = MiseTheme.typography.titleSmall, color = colors.text)
            Text(subtitle, style = MiseTheme.typography.monoTiny, color = colors.textMuted)
        }
    }
}

@Composable
private fun savedLabel(count: Int): String = when (count) {
    0 -> stringResource(Res.string.home_saved_empty)
    1 -> stringResource(Res.string.home_saved_one)
    else -> stringResource(Res.string.home_saved_many, count)
}

@Composable
private fun ScanCard(onClick: () -> Unit) {
    val colors = MiseTheme.colors
    MisePanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26,
        background = Color.Transparent,
        borderColor = Color.Transparent,
        onClick = onClick,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(colors.lime, colors.limeDeep)),
                    RoundedCornerShape(26.dp),
                )
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.home_scan_title),
                        style = MiseTheme.typography.title,
                        color = colors.onLime,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.home_scan_subtitle),
                        style = MiseTheme.typography.monoSmall,
                        color = colors.onLime.copy(alpha = 0.65f),
                    )
                }
                Box(
                    Modifier
                        .size(46.dp)
                        .background(colors.onLime.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiseIcons.Barcode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.onLime,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(title, style = MiseTheme.typography.title, color = MiseTheme.colors.text)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MiseTheme.typography.monoSmall, color = MiseTheme.colors.textMuted)
        }
        Box(
            Modifier.size(46.dp).background(iconBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
        }
    }
}

/** The pulsing lime dot next to the wordmark. */
@Composable
private fun GlowDot() {
    val colors = MiseTheme.colors
    val pulse = rememberPulse(durationMillis = 2400, from = 0.5f, to = 1f, label = "logo")
    Box(
        Modifier
            .size(6.dp)
            .drawBehind {
                drawCircle(color = colors.lime, alpha = pulse.value)
                drawCircle(color = colors.lime, alpha = pulse.value * 0.35f, radius = size.minDimension)
            }
    )
}
