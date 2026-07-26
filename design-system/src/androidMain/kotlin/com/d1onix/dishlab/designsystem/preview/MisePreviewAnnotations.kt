package com.d1onix.dishlab.designsystem.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/** The app's dark canvas, as the ARGB int `@Preview` wants. */
const val MISE_BACKGROUND_ARGB: Long = 0xFF07080C

/**
 * Every full-screen preview in the app. These live in `androidMain` because
 * only Android Studio renders them, and only Android has the device catalogue,
 * system UI chrome and font-scale setting they configure.
 *
 * The three variants are the ones that actually catch bugs in this app: a
 * current phone, the smallest phone still supported (where the fixed-height
 * cards start to collide), and a 150% font scale (where the mono labels wrap).
 * `showSystemUi` is on because every screen uses `safeDrawingPadding()` —
 * without the status and navigation bars the padding is invisible.
 */
@Preview(name = "Phone", device = Devices.PIXEL_7, showSystemUi = true)
@Preview(name = "Small phone", device = Devices.PIXEL_4A, showSystemUi = true)
@Preview(name = "Font 150%", device = Devices.PIXEL_7, showSystemUi = true, fontScale = 1.5f)
annotation class MiseScreenPreviews

/**
 * A single component, sized to its content on the app background. No device and
 * no system UI — a button does not need a status bar to be reviewed.
 */
@Preview(showBackground = true, backgroundColor = MISE_BACKGROUND_ARGB)
annotation class MiseComponentPreview

/**
 * A component whose layout depends on the width it is given: the two widths are
 * the narrowest and widest a phone hands it.
 */
@Preview(name = "Narrow", widthDp = 320, showBackground = true, backgroundColor = MISE_BACKGROUND_ARGB)
@Preview(name = "Wide", widthDp = 411, showBackground = true, backgroundColor = MISE_BACKGROUND_ARGB)
annotation class MiseWidthPreviews
