package com.koach.studio.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

/**
 * Editorial type scale: heavy, tightly tracked display sizes for headlines and
 * calm, open body text. Uses the platform sans so no font files ship with the app.
 */
val KoachTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1.8).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-1.0).sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp)
)
