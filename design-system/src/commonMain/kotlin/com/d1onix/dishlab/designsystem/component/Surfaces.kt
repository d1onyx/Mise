package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme

/** The translucent card every list item and info block sits in. */
@Composable
fun MisePanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    background: Color = MiseTheme.colors.panel,
    borderColor: Color = MiseTheme.colors.border,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val debouncedClick = onClick?.let { rememberDebouncedClick(onClick = it) }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .let {
                if (debouncedClick != null) {
                    it.clickable(enabled = debouncedClick.enabled, onClick = debouncedClick)
                } else {
                    it
                }
            }
            .padding(contentPadding),
        content = content,
    )
}

/** Mono, uppercase, wide-tracked caption: «Ingredients», «Scan · Understand · Cook». */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MiseTheme.colors.violet,
) {
    Text(
        text = text.uppercase(),
        style = MiseTheme.typography.monoLabel,
        color = color,
        modifier = modifier,
    )
}

/** The circular product token: initial in the product's accent colour. */
@Composable
fun ProductAvatar(
    initial: String,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Int = 44,
    fontSize: Int = 16,
    glow: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MiseTheme.colors.surface, CircleShape)
            .border(2.dp, accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (glow) {
            Box(
                Modifier
                    .size((size - 4).dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape)
            )
        }
        Text(
            text = initial,
            style = MiseTheme.typography.mono.copy(fontSize = fontSize.sp),
            color = accent,
        )
    }
}

/** Small pill used for time, difficulty and category tags. */
@Composable
fun MiseTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MiseTheme.colors.textMuted,
    background: Color = Color.White.copy(alpha = 0.06f),
) {
    Text(
        text = text,
        style = MiseTheme.typography.monoTiny,
        color = color,
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Centred message for «No scans yet.» and empty filter results. */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(horizontal = 20.dp, vertical = 50.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MiseTheme.typography.body,
            color = MiseTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Screen header: optional back button, title, optional trailing slot. */
@Composable
fun MiseScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            MiseIconCircleButton(
                icon = MiseIcons.ChevronLeft,
                contentDescription = "Back",
                onClick = onBackClick,
            )
        }
        Column(Modifier.padding(start = if (onBackClick != null) 14.dp else 0.dp)) {
            Text(title, style = MiseTheme.typography.headline, color = MiseTheme.colors.text)
        }
        Box(Modifier.weight(1f))
        trailing?.invoke()
    }
}
