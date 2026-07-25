package com.d1onix.dishlab.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

val LocalMiseColors: ProvidableCompositionLocal<MiseColors> =
    staticCompositionLocalOf { MiseColors() }

val LocalMiseTypography: ProvidableCompositionLocal<MiseTypography> =
    staticCompositionLocalOf { error("MiseTypography not provided — wrap the tree in MiseTheme") }

/** Entry point for every screen in the app. */
@Composable
fun MiseTheme(content: @Composable () -> Unit) {
    val colors = MiseColors()
    val typography = miseTypography()

    CompositionLocalProvider(
        LocalMiseColors provides colors,
        LocalMiseTypography provides typography,
        LocalContentColor provides colors.text,
    ) {
        // Material3 is still underneath (ripples, text selection, dialogs), so
        // its scheme is mapped onto the Mise palette rather than left at default.
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colors.lime,
                onPrimary = colors.onLime,
                secondary = colors.violet,
                onSecondary = colors.text,
                tertiary = colors.cyan,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.surface,
                onSurface = colors.text,
                error = colors.red,
                outline = colors.border,
            ),
        ) {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                content()
            }
        }
    }
}

/** Token access: `MiseTheme.colors.lime`, `MiseTheme.typography.monoLabel`. */
object MiseTheme {
    val colors: MiseColors
        @Composable @ReadOnlyComposable get() = LocalMiseColors.current

    val typography: MiseTypography
        @Composable @ReadOnlyComposable get() = LocalMiseTypography.current
}
