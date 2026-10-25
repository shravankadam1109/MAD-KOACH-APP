package com.koach.studio.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

/**
 * Editorial type scale. Big headlines use a medium weight with gentle tracking
 * for a calm, confident look; figures and titles step up to semibold for
 * hierarchy. Body text stays open and readable. Uses the platform sans, so no
 * font files ship with the app.
 */
val KoachTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.9).sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp)
)
