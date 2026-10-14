package com.squishey.stoolsentry.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.squishey.stoolsentry.R

val Playfair = FontFamily(
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold),
    Font(R.font.playfair_display_italic, FontWeight.Normal, FontStyle.Italic),
)

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

private fun serif(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = Playfair,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val ThroneTypography = Typography(
    displayLarge = serif(48, 54),
    displayMedium = serif(40, 46),
    displaySmall = serif(34, 40),
    headlineLarge = serif(30, 36),
    headlineMedium = serif(26, 32),
    headlineSmall = serif(22, 28),
    titleLarge = serif(21, 27),
    titleMedium = sans(16, 22, FontWeight.SemiBold),
    titleSmall = sans(14, 20, FontWeight.SemiBold),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 21),
    bodySmall = sans(12, 17),
    labelLarge = sans(14, 20, FontWeight.SemiBold, 0.01f),
    labelMedium = sans(12, 16, FontWeight.SemiBold, 0.02f),
    labelSmall = sans(11, 14, FontWeight.Bold, 0.14f),
)

/** Small gold caps used above sections. */
val EyebrowStyle = TextStyle(
    fontFamily = Manrope,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.16.em,
)

val QuoteStyle = TextStyle(
    fontFamily = Playfair,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 15.sp,
    lineHeight = 22.sp,
)
