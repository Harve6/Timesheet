package com.example.timesheet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp
)

// Deliberately larger than Material's defaults, and nothing smaller than 15sp anywhere.
val Typography = Typography(
    displayLarge = style(57, 64, FontWeight.Bold),
    displayMedium = style(48, 56, FontWeight.Bold),
    displaySmall = style(40, 48, FontWeight.Bold),
    headlineLarge = style(36, 44, FontWeight.Bold),
    headlineMedium = style(32, 40, FontWeight.Bold),
    headlineSmall = style(28, 36, FontWeight.Bold),
    titleLarge = style(26, 32, FontWeight.Bold),
    titleMedium = style(21, 28, FontWeight.SemiBold),
    titleSmall = style(18, 24, FontWeight.SemiBold),
    bodyLarge = style(20, 28),
    bodyMedium = style(18, 26),
    bodySmall = style(16, 22),
    labelLarge = style(18, 24, FontWeight.SemiBold),
    labelMedium = style(16, 22, FontWeight.SemiBold),
    labelSmall = style(15, 20, FontWeight.SemiBold)
)
