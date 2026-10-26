package com.koach.studio.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class KoachSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 44.dp,
    /** Horizontal gutter for screen content, balanced for compact and standard displays. */
    val gutter: Dp = 16.dp
)

/**
 * Standardised corner radii. Clean, modern geometry without oversized capsule distortion on cards.
 */
object KoachRadius {
    val xs = 8.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 22.dp
    val pill = 50.dp
}

val KoachShapes = Shapes(
    extraSmall = RoundedCornerShape(KoachRadius.xs),
    small = RoundedCornerShape(KoachRadius.sm),
    medium = RoundedCornerShape(KoachRadius.md),
    large = RoundedCornerShape(KoachRadius.lg),
    extraLarge = RoundedCornerShape(KoachRadius.xl)
)

private val LocalKoachColors = staticCompositionLocalOf { KoachColors() }
private val LocalKoachSpacing = staticCompositionLocalOf { KoachSpacing() }
private val LocalKoachIsDark = staticCompositionLocalOf { true }

private val KoachColorScheme = darkColorScheme(
    primary = KoachPalette.Green,
    onPrimary = KoachPalette.Ink,
    primaryContainer = KoachPalette.GreenContainer,
    onPrimaryContainer = KoachPalette.GreenBright,
    secondary = KoachPalette.Grey,
    onSecondary = KoachPalette.Ink,
    secondaryContainer = KoachPalette.SurfaceHigh,
    onSecondaryContainer = KoachPalette.White,
    tertiary = KoachPalette.Violet,
    background = KoachPalette.Background,
    onBackground = KoachPalette.White,
    surface = KoachPalette.Surface,
    onSurface = KoachPalette.White,
    surfaceVariant = KoachPalette.SurfaceRaised,
    onSurfaceVariant = KoachPalette.Grey,
    surfaceContainerLowest = KoachPalette.Ink,
    surfaceContainerLow = KoachPalette.Surface,
    surfaceContainer = KoachPalette.Surface,
    surfaceContainerHigh = KoachPalette.SurfaceRaised,
    surfaceContainerHighest = KoachPalette.SurfaceHigh,
    outline = KoachPalette.Outline,
    outlineVariant = KoachPalette.OutlineSubtle,
    error = KoachPalette.Red,
    onError = KoachPalette.Ink,
    inverseSurface = KoachPalette.White,
    inverseOnSurface = KoachPalette.Ink,
    inversePrimary = KoachPalette.GreenDeep,
    scrim = KoachPalette.Ink
)

/** Light semantic colours — keeps brand identity coherent with dark mode. */
private val KoachColorsLight = KoachColors(
    background = KoachPalette.LightBackground,
    surface = KoachPalette.LightSurface,
    surfaceRaised = KoachPalette.LightSurfaceRaised,
    surfaceHigh = KoachPalette.LightSurfaceHigh,
    outline = KoachPalette.LightOutline,
    outlineSubtle = KoachPalette.LightOutlineSubtle,
    brand = KoachPalette.GreenDeep,
    brandBright = KoachPalette.Green,
    brandDeep = Color(0xFF1C5100),
    brandContainer = Color(0xFFE3F6D5),
    brandOutline = Color(0xFF9ED47E),
    onBrand = KoachPalette.White,
    textPrimary = KoachPalette.Ink,
    textSecondary = KoachPalette.LightTextSecondary,
    textTertiary = KoachPalette.LightTextTertiary,
    success = KoachPalette.GreenDeep,
    warning = Color(0xFFA87000),
    danger = Color(0xFFB82020),
    info = Color(0xFF1C6EA8),
    review = Color(0xFF6B46E0),
    glass = Color(0xD9FFFFFF),
    glassRaised = Color(0xF2FFFFFF),
    glassBorder = Color.Black.copy(alpha = 0.07f),
    glassHighlight = Color.White.copy(alpha = 0.6f),
    scrim = KoachPalette.Ink,
    glow = KoachPalette.Green
)

private val KoachLightColorScheme = lightColorScheme(
    primary = KoachPalette.GreenDeep,
    onPrimary = KoachPalette.White,
    primaryContainer = Color(0xFFE3F6D5),
    onPrimaryContainer = Color(0xFF1C5100),
    secondary = KoachPalette.LightTextSecondary,
    onSecondary = KoachPalette.White,
    secondaryContainer = KoachPalette.LightSurfaceHigh,
    onSecondaryContainer = KoachPalette.Ink,
    tertiary = Color(0xFF6B46E0),
    background = KoachPalette.LightBackground,
    onBackground = KoachPalette.Ink,
    surface = KoachPalette.LightSurface,
    onSurface = KoachPalette.Ink,
    surfaceVariant = KoachPalette.LightSurfaceRaised,
    onSurfaceVariant = KoachPalette.LightTextSecondary,
    surfaceContainerLowest = KoachPalette.White,
    surfaceContainerLow = KoachPalette.LightSurface,
    surfaceContainer = KoachPalette.LightSurfaceRaised,
    surfaceContainerHigh = KoachPalette.LightSurfaceHigh,
    surfaceContainerHighest = KoachPalette.LightOutline,
    outline = KoachPalette.LightOutline,
    outlineVariant = KoachPalette.LightOutlineSubtle,
    error = Color(0xFFB82020),
    onError = KoachPalette.White,
    inverseSurface = KoachPalette.Ink,
    inverseOnSurface = KoachPalette.White,
    inversePrimary = KoachPalette.GreenBright,
    scrim = KoachPalette.Ink
)

/**
 * KOACH Studio theme. Supports Dark (brand default), Light, and System preference modes.
 */
@Composable
fun KoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalKoachColors provides if (darkTheme) KoachColors() else KoachColorsLight,
        LocalKoachSpacing provides KoachSpacing(),
        LocalKoachIsDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) KoachColorScheme else KoachLightColorScheme,
            typography = KoachTypography,
            shapes = KoachShapes,
            content = content
        )
    }
}

object KoachTheme {
    val colors: KoachColors
        @Composable @ReadOnlyComposable get() = LocalKoachColors.current
    val spacing: KoachSpacing
        @Composable @ReadOnlyComposable get() = LocalKoachSpacing.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalKoachIsDark.current
}
