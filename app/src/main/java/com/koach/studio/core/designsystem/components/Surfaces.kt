package com.koach.studio.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.pressScale
import com.koach.studio.core.designsystem.theme.rememberReducedMotion

/**
 * Glass surface treatment shared by every card: a translucent fill that lets the
 * ambient background breathe through, a faint top sheen and a hairline edge that
 * reads as reflected light. No live blur, so it stays cheap on any device.
 */
@Composable
private fun Modifier.glass(shape: Shape, raised: Boolean = false, edge: Color? = null): Modifier {
    val c = KoachTheme.colors
    val top = if (raised) c.glassRaised else c.glass
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, c.glass)))
        .drawBehind {
            drawRect(
                Brush.verticalGradient(listOf(c.glassHighlight, Color.Transparent), endY = size.height * 0.35f)
            )
        }
        .border(
            BorderStroke(1.dp, Brush.verticalGradient(listOf(edge ?: c.glassHighlight.copy(alpha = 0.12f), edge?.copy(alpha = 0.08f) ?: c.glassBorder))),
            shape
        )
}

/** Clickable surfaces settle under the finger; static ones skip the interaction plumbing. */
@Composable
private fun Modifier.tappable(onClick: (() -> Unit)?, minSize: Boolean = true): Modifier {
    if (onClick == null) return this
    val interaction = remember { MutableInteractionSource() }
    return this
        .pressScale(interaction, 0.985f)
        .then(if (minSize) Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp) else Modifier)
        .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
}

/**
 * Standard KOACH card: a glass panel over the studio background.
 * [highlighted] raises it and picks out its edge in brand green.
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
    Column(
        modifier = modifier
            .tappable(onClick)
            .glass(shape, raised = highlighted, edge = if (highlighted) colors.brand.copy(alpha = 0.4f) else null)
            .padding(contentPadding),
        content = content
    )
}

/**
 * The one feature surface on a screen: raised glass lit from its top corner by a
 * soft bloom of brand green, so it reads as the focal point without a green fill.
 */
@Composable
fun KoachHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.xl)
    CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
        Column(
            modifier = modifier
                .tappable(onClick, minSize = false)
                .glass(shape, raised = true, edge = colors.brand.copy(alpha = 0.32f))
                .cornerGlow(colors.glow, 0.22f)
                .padding(contentPadding),
            content = content
        )
    }
}

/** Nested sub-panel inside a hero card or summary: a quieter, more transparent glass. */
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
            .tappable(onClick)
            .clip(shape)
            .background(if (dark) Color.White.copy(alpha = if (KoachTheme.isDark) 0.04f else 0.5f) else colors.glass)
            .border(BorderStroke(1.dp, colors.glassBorder), shape)
            .padding(contentPadding),
        content = content
    )
}

/** Soft radial bloom of [color] from the top-end corner. Keep [intensity] low (≤ 0.3). */
fun Modifier.cornerGlow(color: Color, intensity: Float = 0.18f): Modifier = drawBehind {
    if (intensity <= 0f) return@drawBehind
    drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = intensity), Color.Transparent),
            center = Offset(size.width * 0.92f, 0f),
            radius = size.maxDimension * 0.75f
        )
    )
}

/** Centred bloom behind an icon or headline group. Keep [intensity] low (≤ 0.35). */
fun Modifier.koachGlow(color: Color, intensity: Float = 0.18f): Modifier = drawBehind {
    if (intensity <= 0f) return@drawBehind
    drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = intensity * 0.8f), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.9f
        )
    )
}

/**
 * Full-screen studio background: near-black with two slow-drifting pools of
 * brand-green light. The drift is read only in the draw phase, so it never
 * recomposes content; it pauses with the frame clock when the app is hidden
 * and holds still when the system asks for reduced motion.
 */
@Composable
fun KoachBackground(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = KoachTheme.colors
    val strength = if (KoachTheme.isDark) 1f else 0.45f
    val reduced = rememberReducedMotion()
    val drift = if (glow && !reduced) {
        val transition = rememberInfiniteTransition(label = "ambient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(KoachMotion.Ambient, easing = LinearEasing), RepeatMode.Reverse),
            label = "drift"
        )
    } else null
    Box(
        modifier = modifier
            .background(colors.background)
            .then(
                if (!glow) Modifier else Modifier.drawBehind {
                    val t = drift?.value ?: 0.5f
                    drawRect(
                        Brush.radialGradient(
                            listOf(colors.glow.copy(alpha = 0.16f * strength), Color.Transparent),
                            center = Offset(size.width * (0.78f + 0.14f * t), size.height * (0.02f + 0.06f * t)),
                            radius = size.width * 0.95f
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(colors.glow.copy(alpha = 0.07f * strength), Color.Transparent),
                            center = Offset(size.width * (0.1f + 0.12f * (1f - t)), size.height * 0.88f),
                            radius = size.width * 0.8f
                        )
                    )
                }
            )
    ) {
        content()
    }
}
