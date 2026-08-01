package com.d1onix.dishlab.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The prototype's inline SVGs as [ImageVector]s. They are drawn with a stroke
 * and no fill, and every icon is tinted at the call site, so `Icon(..., tint =)`
 * recolours the whole path.
 */
object MiseIcons {

    val ChevronLeft: ImageVector by lazy {
        strokeIcon("chevron_left", 16f, "M10 3L5 8l5 5")
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("chevron_right", 16f, "M6 3l5 5-5 5")
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("chevron_down", 10f, "M2 3.5L5 6.5l3-3", strokeWidth = 1.5f)
    }

    val Plus: ImageVector by lazy {
        strokeIcon("plus", 16f, "M8 3v10M3 8h10", strokeWidth = 1.9f)
    }

    val Close: ImageVector by lazy {
        strokeIcon("close", 20f, "M5 5l10 10M15 5L5 15", strokeWidth = 1.6f)
    }

    val Check: ImageVector by lazy {
        strokeIcon("check", 16f, "M3 8l3 3 7-7", strokeWidth = 1.9f)
    }

    val Scissors: ImageVector by lazy {
        strokeIcon(
            "scissors",
            20f,
            "M7 7a3 3 0 11-6 0 3 3 0 016 0zM7 13a3 3 0 11-6 0 3 3 0 016 0zM6.5 8.5L18 3M6.5 11.5L18 17",
            strokeWidth = 1.6f,
        )
    }

    val Search: ImageVector by lazy {
        strokeIcon("search", 20f, "M15 9a6 6 0 10-12 0 6 6 0 0012 0zM14 14l4 4", strokeWidth = 1.6f)
    }

    val Bookmark: ImageVector by lazy {
        strokeIcon("bookmark", 24f, "M6 4h12a1 1 0 011 1v15l-7-4-7 4V5a1 1 0 011-1z")
    }

    val NoResult: ImageVector by lazy {
        strokeIcon("no_result", 24f, "M4 4l16 16M9 9a5 5 0 106.5 6.5")
    }

    val Timer: ImageVector by lazy {
        strokeIcon("timer", 24f, "M12 4a8 8 0 108 8M12 7v5l3 2", strokeWidth = 1.6f)
    }

    /** Torch on — the bolt is closed so it reads as a solid shape when tinted lime. */
    val Flash: ImageVector by lazy {
        strokeIcon("flash", 20f, "M11.6 1.8L4.8 11.4h4.1l-.5 6.8 6.8-9.6h-4.1l.5-6.8z", strokeWidth = 1.6f)
    }

    /** Torch off — the same bolt struck through, so the two states share a silhouette. */
    val FlashOff: ImageVector by lazy {
        strokeIcon(
            "flash_off",
            20f,
            "M11.6 1.8L4.8 11.4h4.1l-.5 6.8 6.8-9.6h-4.1l.5-6.8z M2.6 2.6l14.8 14.8",
            strokeWidth = 1.6f,
        )
    }

    /** Swap between the back and front lens. */
    val CameraFlip: ImageVector by lazy {
        strokeIcon(
            "camera_flip",
            20f,
            "M3.4 10a6.6 6.6 0 0111.1-4.8M16.6 10a6.6 6.6 0 01-11.1 4.8" +
                "M14.6 2.1v3.4h-3.4M5.4 17.9v-3.4h3.4",
            strokeWidth = 1.6f,
        )
    }

    /** The barcode glyph on the home screen — filled bars, not a stroke. */
    val Barcode: ImageVector by lazy {
        val bars = listOf(3f to 1.6f, 6f to 1f, 8.5f to 2f, 12f to 1f, 14.5f to 2.4f, 18.5f to 1.2f)
        ImageVector.Builder(
            name = "barcode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            bars.forEach { (x, width) ->
                addPath(
                    pathData = addPathNodes("M$x 6h${width}v12h-${width}z"),
                    fill = SolidColor(Color.Black),
                )
            }
        }.build()
    }

    private fun strokeIcon(
        name: String,
        viewport: Float,
        pathData: String,
        strokeWidth: Float = 1.8f,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = viewport.dp,
        defaultHeight = viewport.dp,
        viewportWidth = viewport,
        viewportHeight = viewport,
    ).apply {
        addPath(
            pathData = addPathNodes(pathData),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
