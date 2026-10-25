package com.koach.studio.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.isSystemInDarkTheme
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
    val xxxl: Dp = 48.dp,
    /** Horizontal gutter for screen content. */
    val gutter: Dp = 20.dp
)

/**
 * Corner radii. Large, soft corners on cards and fully rounded controls give the
 * layered, tactile feel of the reference designs.
 */
object KoachRadius {
    val sm = 12.dp
    val md = 18.dp
    val lg = 26.dp
    val xl = 32.dp
}

val KoachShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(KoachRadius.sm),
    medium = RoundedCornerShape(KoachRadius.md),
    large = RoundedCornerShape(KoachRadius.lg),
    extraLarge = RoundedCornerShape(KoachRadius.xl)
)

private val LocalKoachColors = staticCompositionLocalOf { KoachColors() }
private val LocalKoachSpacing = staticCompositionLocalOf { KoachSpacing() }

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
    surface = KoachPalette.Background,
    onSurface = KoachPalette.White,
    surfaceVariant = KoachPalette.Surface,
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

/** Light semantic colours — used when the system theme is set to light mode. */
private val KoachColorsLight = KoachColors(
    background = KoachPalette.White,
    surface = Color(0xFFF3F4F6),
    surfaceRaised = Color(0xFFE8EAF0),
    surfaceHigh = Color(0xFFDCDFE8),
    outline = Color(0xFFBFC3CF),
    outlineSubtle = Color(0xFFD8DBE5),
    brand = KoachPalette.GreenDeep,
    brandBright = KoachPalette.Green,
    brandDeep = Color(0xFF1C5100),
    brandContainer = Color(0xFFDCF4CC),
    brandOutline = Color(0xFFABDC90),
    onBrand = KoachPalette.White,
    textPrimary = KoachPalette.Ink,
    textSecondary = Color(0xFF3C4155),
    textTertiary = KoachPalette.GreyDim,
    success = KoachPalette.GreenDeep,
    warning = Color(0xFFA87000),
    danger = Color(0xFFB82020),
    info = Color(0xFF1C6EA8),
    review = Color(0xFF6B46E0)
)

private val KoachLightColorScheme = lightColorScheme(
    primary = KoachPalette.GreenDeep,
    onPrimary = KoachPalette.White,
    primaryContainer = Color(0xFFDCF4CC),
    onPrimaryContainer = Color(0xFF1C5100),
    secondary = Color(0xFF3C4155),
    onSecondary = KoachPalette.White,
    secondaryContainer = Color(0xFFDCDFE8),
    onSecondaryContainer = KoachPalette.Ink,
    tertiary = Color(0xFF6B46E0),
    background = KoachPalette.White,
    onBackground = KoachPalette.Ink,
    surface = Color(0xFFF3F4F6),
    onSurface = KoachPalette.Ink,
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF3C4155),
    surfaceContainerLowest = KoachPalette.White,
    surfaceContainerLow = Color(0xFFF3F4F6),
    surfaceContainer = Color(0xFFE8EAF0),
    surfaceContainerHigh = Color(0xFFDCDFE8),
    surfaceContainerHighest = Color(0xFFBFC3CF),
    outline = Color(0xFFBFC3CF),
    outlineVariant = Color(0xFFD8DBE5),
    error = Color(0xFFB82020),
    onError = KoachPalette.White,
    inverseSurface = KoachPalette.Ink,
    inverseOnSurface = KoachPalette.White,
    inversePrimary = KoachPalette.GreenBright,
    scrim = KoachPalette.Ink
)

/** KOACH Studio is dark-first; light mode is supported for system-preference accessibility. */
@Composable
fun KoachTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalKoachColors provides if (isDark) KoachColors() else KoachColorsLight,
        LocalKoachSpacing provides KoachSpacing()
    ) {
        MaterialTheme(
            colorScheme = if (isDark) KoachColorScheme else KoachLightColorScheme,
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
}
