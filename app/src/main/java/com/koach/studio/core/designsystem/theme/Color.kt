package com.koach.studio.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Raw KOACH palette. Screens should read colours through [KoachTheme.colors], not these values. */
object KoachPalette {
    val Green = Color(0xFF41BA00)
    val GreenBright = Color(0xFF62DB1F)
    val GreenDeep = Color(0xFF2C7F00)
    val GreenContainer = Color(0xFF12230B)
    val GreenOutline = Color(0xFF24461A)

    val Ink = Color(0xFF08090B)
    val Background = Color(0xFF0D0E11)
    val Surface = Color(0xFF16181D)
    val SurfaceRaised = Color(0xFF1C1F25)
    val SurfaceHigh = Color(0xFF242832)
    val Outline = Color(0xFF2A2E38)
    val OutlineSubtle = Color(0xFF1F2229)

    val White = Color(0xFFFFFFFF)
    val Grey = Color(0xFFA1A6B3)
    val GreyDim = Color(0xFF6C7180)

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
