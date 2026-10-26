package com.koach.studio.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Raw KOACH palette. Screens should read colours through [KoachTheme.colors], not these values. */
object KoachPalette {
    /** KOACH signature lime-green accent (#41BA00). Used deliberately as an accent, not a full-screen wash. */
    val Green = Color(0xFF41BA00)
    val GreenBright = Color(0xFF5CE019)
    val GreenDeep = Color(0xFF2E8500)
    val GreenShadow = Color(0xFF1A4B00)
    val GreenContainer = Color(0xFF11210A)
    val GreenOutline = Color(0xFF234517)

    /** Deep studio blacks and layered charcoal surfaces. */
    val Ink = Color(0xFF07080A)
    val Background = Color(0xFF0B0C0E)
    val Surface = Color(0xFF131519)
    val SurfaceRaised = Color(0xFF1A1D23)
    val SurfaceHigh = Color(0xFF22262E)
    val Outline = Color(0xFF2B303B)
    val OutlineSubtle = Color(0xFF1E222A)

    /** Clean white and neutral grey typography tokens. */
    val White = Color(0xFFFFFFFF)
    val Grey = Color(0xFFA5ABB8)
    /** Tertiary text; kept at 4.5:1 or better against Background. */
    val GreyDim = Color(0xFF7D8494)

    /** Light mode neutral surfaces and ink tokens. */
    val LightBackground = Color(0xFFF7F8FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceRaised = Color(0xFFF0F2F6)
    val LightSurfaceHigh = Color(0xFFE4E7EE)
    val LightOutline = Color(0xFFC5CAD6)
    val LightOutlineSubtle = Color(0xFFE1E4EC)
    val LightTextSecondary = Color(0xFF43495B)
    val LightTextTertiary = Color(0xFF687082)

    val Amber = Color(0xFFF5B83D)
    val Red = Color(0xFFF26B6B)
    val Blue = Color(0xFF5AA9F2)
    val Violet = Color(0xFFA48CF7)
}

/** Semantic colours that Material's ColorScheme has no slot for. */
@Immutable
data class KoachColors(
    val background: Color = KoachPalette.Background,
    val surface: Color = KoachPalette.Surface,
    val surfaceRaised: Color = KoachPalette.SurfaceRaised,
    val surfaceHigh: Color = KoachPalette.SurfaceHigh,
    val outline: Color = KoachPalette.Outline,
    val outlineSubtle: Color = KoachPalette.OutlineSubtle,
    val brand: Color = KoachPalette.Green,
    val brandBright: Color = KoachPalette.GreenBright,
    val brandDeep: Color = KoachPalette.GreenDeep,
    val brandContainer: Color = KoachPalette.GreenContainer,
    val brandOutline: Color = KoachPalette.GreenOutline,
    val onBrand: Color = KoachPalette.Ink,
    val textPrimary: Color = KoachPalette.White,
    val textSecondary: Color = KoachPalette.Grey,
    val textTertiary: Color = KoachPalette.GreyDim,
    val success: Color = KoachPalette.Green,
    val warning: Color = KoachPalette.Amber,
    val danger: Color = KoachPalette.Red,
    val info: Color = KoachPalette.Blue,
    val review: Color = KoachPalette.Violet
)
