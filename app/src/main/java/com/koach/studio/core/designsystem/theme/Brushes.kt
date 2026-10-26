package com.koach.studio.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Shared surface brushes. Preference is given to solid charcoal surfaces and subtle
 * tonal depth rather than harsh full-surface green gradients.
 */
object KoachBrushes {
    /** Refined dark charcoal surface brush for hero cards, avoiding harsh neon green fills. */
    val hero: Brush = Brush.linearGradient(
        colors = listOf(
            KoachPalette.SurfaceRaised,
            KoachPalette.Surface
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 1200f)
    )
}
