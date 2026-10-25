package com.koach.studio.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Shared gradients so every screen uses the same light and direction. */
object KoachBrushes {
    /** Bright KOACH-green hero fill (top-left light falling to deep green). Pair with dark text. */
    val hero: Brush = Brush.linearGradient(
        colors = listOf(KoachPalette.GreenBright, KoachPalette.Green, KoachPalette.GreenDeep, KoachPalette.GreenShadow),
        start = Offset(0f, 0f),
        end = Offset(900f, 1400f)
    )

}
