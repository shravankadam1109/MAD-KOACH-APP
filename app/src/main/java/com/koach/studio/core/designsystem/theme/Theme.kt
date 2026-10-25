package com.koach.studio.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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

/** KOACH Studio is dark-only by design; the brand has no light variant. */
@Composable
fun KoachTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKoachColors provides KoachColors(),
        LocalKoachSpacing provides KoachSpacing()
    ) {
        MaterialTheme(
            colorScheme = KoachColorScheme,
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
