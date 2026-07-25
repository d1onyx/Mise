package com.d1onix.dishlab.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Mise palette. Dark only — the product is a scanner used in shops, and the
 * neon-on-black look is the brand, not a preference.
 */
@Immutable
data class MiseColors(
    val background: Color = Color(0xFF07080C),
    /** Slightly deeper background used behind the camera viewfinder. */
    val backgroundDeep: Color = Color(0xFF040507),
    /** Filled surface of avatars and nodes, opaque so glows read against it. */
    val surface: Color = Color(0xFF0C0E14),
    val panel: Color = Color.White.copy(alpha = 0.05f),
    val panelStrong: Color = Color.White.copy(alpha = 0.08f),
    val border: Color = Color.White.copy(alpha = 0.10f),
    val borderStrong: Color = Color.White.copy(alpha = 0.14f),
    val lime: Color = Color(0xFFC8FF4D),
    val limeDeep: Color = Color(0xFFA6E02E),
    val onLime: Color = Color(0xFF0A1500),
    val violet: Color = Color(0xFF8B7BFF),
    val cyan: Color = Color(0xFF4EE9E0),
    val onCyan: Color = Color(0xFF04121A),
    val amber: Color = Color(0xFFFFC24E),
    val red: Color = Color(0xFFFF6B5E),
    val text: Color = Color(0xFFEEF1F4),
    val textMuted: Color = Color.White.copy(alpha = 0.55f),
    val textFaint: Color = Color.White.copy(alpha = 0.40f),
    val textDim: Color = Color.White.copy(alpha = 0.35f),
)

/** How a rated value reads at a glance. Callers map their domain verdict onto this. */
enum class ScoreTone { Positive, Caution, Negative }

fun MiseColors.forTone(tone: ScoreTone): Color = when (tone) {
    ScoreTone.Positive -> lime
    ScoreTone.Caution -> amber
    ScoreTone.Negative -> red
}
