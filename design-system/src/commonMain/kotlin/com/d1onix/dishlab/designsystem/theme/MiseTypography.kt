package com.d1onix.dishlab.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.d1onix.dishlab.designsystem.resources.Res
import com.d1onix.dishlab.designsystem.resources.ibm_plex_mono_medium
import com.d1onix.dishlab.designsystem.resources.ibm_plex_mono_regular
import com.d1onix.dishlab.designsystem.resources.ibm_plex_mono_semibold
import com.d1onix.dishlab.designsystem.resources.space_grotesk
import org.jetbrains.compose.resources.Font

/**
 * Space Grotesk ships as a single variable font (wght 300–700), so every weight
 * comes from one file through [FontVariation]. Weight axes need API 26 on
 * Android; below that the regular master is used, which is a cosmetic
 * difference only.
 */
@Composable
private fun spaceGrotesk(): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            resource = Res.font.space_grotesk,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    }
)

@Composable
private fun plexMono(): FontFamily = FontFamily(
    Font(Res.font.ibm_plex_mono_regular, weight = FontWeight.Normal),
    Font(Res.font.ibm_plex_mono_medium, weight = FontWeight.Medium),
    Font(Res.font.ibm_plex_mono_semibold, weight = FontWeight.SemiBold),
)

/**
 * Two families: Space Grotesk carries the content, IBM Plex Mono every number,
 * tag and technical caption — the split is what makes the UI look instrumented.
 */
@Immutable
data class MiseTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val monoLabel: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val monoTiny: TextStyle,
    val monoDisplay: TextStyle,
)

@Composable
internal fun miseTypography(): MiseTypography {
    val sans = spaceGrotesk()
    val mono = plexMono()
    return MiseTypography(
        display = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 36.sp,
            letterSpacing = (-1).sp,
        ),
        headline = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.6).sp,
        ),
        title = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.5).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.3).sp,
        ),
        bodyLarge = TextStyle(fontFamily = sans, fontSize = 17.sp, lineHeight = 26.sp),
        body = TextStyle(fontFamily = sans, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = sans, fontSize = 13.sp, lineHeight = 20.sp),
        monoLabel = TextStyle(
            fontFamily = mono,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
        ),
        mono = TextStyle(fontFamily = mono, fontSize = 13.sp),
        monoSmall = TextStyle(fontFamily = mono, fontSize = 12.sp),
        monoTiny = TextStyle(fontFamily = mono, fontSize = 10.5.sp),
        monoDisplay = TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
        ),
    )
}
