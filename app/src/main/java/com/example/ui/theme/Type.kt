package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

/** Tracking used by the launch wordmark. */
val WordmarkTracking = 4.sp

private fun style(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Float = 0f
) = TextStyle(
    fontFamily = Rajdhani,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

/**
 * Material 3 roles mapped to seven type sizes. Rajdhani carries the RORAX
 * display personality while keeping one family across the app.
 */
val AppTypography = Typography(
    displayLarge = style(FontWeight.Bold, 45, 52),
    displayMedium = style(FontWeight.Bold, 32, 38),
    displaySmall = style(FontWeight.Bold, 32, 38),
    headlineLarge = style(FontWeight.Bold, 32, 38),
    headlineMedium = style(FontWeight.Bold, 24, 30),
    headlineSmall = style(FontWeight.Bold, 24, 30),
    titleLarge = style(FontWeight.Bold, 20, 26),
    titleMedium = style(FontWeight.Bold, 16, 22),
    titleSmall = style(FontWeight.SemiBold, 16, 22),
    bodyLarge = style(FontWeight.SemiBold, 16, 24),
    bodyMedium = style(FontWeight.SemiBold, 14, 20),
    bodySmall = style(FontWeight.SemiBold, 12, 16),
    labelLarge = style(FontWeight.Bold, 14, 20, 0.5f),
    labelMedium = style(FontWeight.SemiBold, 12, 16, 0.5f),
    labelSmall = style(FontWeight.SemiBold, 12, 16)
)
