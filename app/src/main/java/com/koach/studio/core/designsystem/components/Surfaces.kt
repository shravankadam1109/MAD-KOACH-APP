package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

/**
 * The standard KOACH card: charcoal surface, hairline outline, generous radius.
 * [highlighted] swaps the outline for a green gradient edge to draw the eye.
 */
@Composable
fun KoachCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.lg)
    val border = if (highlighted) {
        BorderStroke(1.dp, Brush.linearGradient(listOf(colors.brand, colors.brandOutline, colors.outline)))
    } else {
        BorderStroke(1.dp, colors.outlineSubtle)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (highlighted) {
                    Brush.verticalGradient(listOf(colors.brandContainer, colors.surface))
                } else {
                    Brush.verticalGradient(listOf(colors.surface, colors.surface))
                }
            )
            .border(border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Soft green radial glow behind hero areas. Keep it subtle: it is light, not paint. */
fun Modifier.koachGlow(color: Color, intensity: Float = 0.22f): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = intensity), Color.Transparent),
            center = Offset(size.width * 0.85f, 0f),
            radius = size.maxDimension * 0.75f
        )
    )
}

/** Full-screen background with the brand glow in the top corner. */
@Composable
fun KoachBackground(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = KoachTheme.colors
    Box(
        modifier = modifier
            .background(colors.background)
            .then(if (glow) Modifier.koachGlow(colors.brand, 0.14f) else Modifier)
    ) {
        content()
    }
}
