package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/** The lime call-to-action: «Scan a product», «Find recipes», «Start cooking mode». */
@Composable
fun MisePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleUse: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 17.dp, horizontal = 20.dp),
) {
    val colors = MiseTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val singleUseClick = rememberSingleUseClick(onClick = onClick)
    val isEnabled = enabled && (!singleUse || singleUseClick.enabled)
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.45f)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = colors.lime,
                spotColor = colors.lime,
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(listOf(colors.lime, colors.limeDeep)),
                shape = shape,
            )
            .clickable(enabled = isEnabled, onClick = if (singleUse) singleUseClick else onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiseTheme.typography.titleSmall,
            color = colors.onLime,
            textAlign = TextAlign.Center,
        )
    }
}

/** Outlined counterpart used for secondary actions and «+ Add». */
@Composable
fun MiseGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    singleUse: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 15.dp, horizontal = 18.dp),
) {
    val colors = MiseTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val singleUseClick = rememberSingleUseClick(onClick = onClick)
    Box(
        modifier = modifier
            .alpha(if (!singleUse || singleUseClick.enabled) 1f else 0.45f)
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .clickable(
                enabled = !singleUse || singleUseClick.enabled,
                onClick = if (singleUse) singleUseClick else onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MiseTheme.typography.body, color = colors.text)
    }
}

/** The mono, low-contrast text action («back to home», «simulate not found»). */
@Composable
fun MiseTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MiseTheme.colors.textFaint,
    singleUse: Boolean = true,
) {
    val singleUseClick = rememberSingleUseClick(onClick = onClick)
    Text(
        text = text,
        style = MiseTheme.typography.monoSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .alpha(if (!singleUse || singleUseClick.enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = !singleUse || singleUseClick.enabled,
                onClick = if (singleUse) singleUseClick else onClick,
            )
            .padding(vertical = 6.dp),
    )
}

/** The circular 38–40dp button used for back, «+» and the profile initials. */
@Composable
fun MiseCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 38,
    borderColor: Color = MiseTheme.colors.borderStrong,
    background: Color = Color.Transparent,
    singleUse: Boolean = true,
    content: @Composable () -> Unit,
) {
    val singleUseClick = rememberSingleUseClick(onClick = onClick)
    val isEnabled = !singleUse || singleUseClick.enabled
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else 0.45f)
            .size(size.dp)
            .clip(CircleShape)
            .background(background, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = isEnabled, onClick = if (singleUse) singleUseClick else onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
fun MiseIconCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 38,
    iconSize: Int = 15,
    tint: Color = MiseTheme.colors.text,
    borderColor: Color = MiseTheme.colors.borderStrong,
    background: Color = Color.Transparent,
    singleUse: Boolean = true,
) {
    MiseCircleButton(onClick, modifier, size, borderColor, background, singleUse) {
        Icon(icon, contentDescription, Modifier.size(iconSize.dp), tint = tint)
    }
}

/** Row of actions pinned to the bottom of a screen. */
@Composable
fun MiseActionRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}
