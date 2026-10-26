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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

/**
 * Standard KOACH card: a solid charcoal (or light neutral) surface with a crisp 1dp border
 * and subtle tonal separation. [highlighted] uses an elevated surface and restrained green border.
 */
@Composable
fun KoachCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.lg)
    val bg = if (highlighted) colors.surfaceRaised else colors.surface
    val edge = if (highlighted) colors.brand.copy(alpha = 0.45f) else colors.outlineSubtle
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
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
 * Elevated feature card for the primary summary on a screen.
 * Uses a solid raised charcoal surface with a restrained #41BA00 accent border and top bar,
 * avoiding harsh full-card green gradients.
 */
@Composable
fun KoachHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.xl)
    val brandAccent = colors.brand
    CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
        Column(
            modifier = modifier
                .clip(shape)
                .background(colors.surfaceRaised)
                .border(BorderStroke(1.dp, colors.brand.copy(alpha = 0.4f)), shape)
                .drawBehind {
                    // Subtle 3dp brand accent line along the top edge.
                    drawRect(
                        color = brandAccent,
                        topLeft = Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                    )
                }
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(contentPadding),
            content = content
        )
    }
}

/** Solid nested sub-panel for use inside [KoachHeroCard] or dashboard summaries. */
@Composable
fun KoachGlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    dark: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (dark) colors.surfaceHigh else colors.surface)
            .border(BorderStroke(1.dp, colors.outline), shape)
            .then(if (onClick != null) Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Kept as a no-op modifier so screens prefer solid surfaces over radial green washes. */
@Suppress("UnusedReceiverParameter")
fun Modifier.cornerGlow(color: Color, intensity: Float = 0f): Modifier = this

/** Kept as a no-op modifier so screens prefer solid surfaces over radial green washes. */
@Suppress("UnusedReceiverParameter")
fun Modifier.koachGlow(color: Color, intensity: Float = 0f): Modifier = this

/**
 * Full-screen background: clean solid studio background in both dark and light modes.
 */
@Composable
fun KoachBackground(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = KoachTheme.colors
    Box(
        modifier = modifier.background(colors.background)
    ) {
        content()
    }
}
