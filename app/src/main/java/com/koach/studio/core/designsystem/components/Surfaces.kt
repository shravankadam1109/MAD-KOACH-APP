package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachBrushes
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

/**
 * The standard KOACH card: a layered charcoal surface with a large radius and a
 * barely-there edge, separated from the background by tone rather than lines.
 * [highlighted] adds a soft green wash from the top-right corner and a faint
 * green edge to draw the eye without shouting.
 */
@Composable
fun KoachCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.lg)
    val edge = if (highlighted) colors.brand.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .then(if (highlighted) Modifier.cornerGlow(colors.brand, 0.22f) else Modifier)
            .border(BorderStroke(1.dp, edge), shape)
            .then(
                if (onClick != null) {
                    Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Bright KOACH-green feature card for the single most important item on a
 * screen. Content is dark for contrast; use [KoachTheme.colors.onBrand] for text.
 */
@Composable
fun KoachHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(KoachRadius.xl)
    CompositionLocalProvider(LocalContentColor provides KoachTheme.colors.onBrand) {
        Column(
            modifier = modifier
                .clip(shape)
                .background(KoachBrushes.hero)
                .drawBehind {
                    // A soft highlight in the top-right gives the fill some depth.
                    val center = Offset(size.width * 0.9f, size.height * 0.05f)
                    val radius = size.minDimension * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = center, radius = radius),
                        radius = radius,
                        center = center
                    )
                }
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
            content = content
        )
    }
}

/** Translucent panel for use on top of [KoachHeroCard] or glow backgrounds. */
@Composable
fun KoachGlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    dark: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(KoachRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (dark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f))
            .then(if (onClick != null) Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Soft green wash from the top-right corner, sized to whatever it decorates. */
fun Modifier.cornerGlow(color: Color, intensity: Float = 0.2f): Modifier = drawBehind {
    val center = Offset(size.width, 0f)
    val radius = size.maxDimension * 0.85f
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = intensity), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center
    )
}

/** Soft green radial glow behind hero areas. Keep it subtle: it is light, not paint. */
fun Modifier.koachGlow(color: Color, intensity: Float = 0.22f): Modifier = drawBehind {
    val center = Offset(size.width * 0.85f, 0f)
    val radius = size.maxDimension * 0.75f
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color.copy(alpha = intensity), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center
    )
}

/**
 * Full-screen background: near-black with two blurred light pools in KOACH
 * green, a strong one top-right and a faint one lower-left, like light leaking
 * across a dark studio.
 */
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
            .then(
                if (glow) {
                    Modifier.drawBehind {
                        val top = Offset(size.width * 0.95f, size.height * 0.02f)
                        val topRadius = size.width * 1.05f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(colors.brand.copy(alpha = 0.28f), colors.brand.copy(alpha = 0.06f), Color.Transparent),
                                center = top,
                                radius = topRadius
                            ),
                            radius = topRadius,
                            center = top
                        )
                        val low = Offset(-size.width * 0.1f, size.height * 0.62f)
                        val lowRadius = size.width * 0.8f
                        drawCircle(
                            brush = Brush.radialGradient(listOf(colors.brand.copy(alpha = 0.09f), Color.Transparent), center = low, radius = lowRadius),
                            radius = lowRadius,
                            center = low
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}
