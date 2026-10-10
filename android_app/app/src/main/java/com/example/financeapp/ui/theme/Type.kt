package com.example.financeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// shortcut: system sans instead of bundled Manrope/Inter; add res/font when T1 owns the theme
private val HeadingFont = FontFamily.SansSerif
private val BodyFont = FontFamily.SansSerif

private const val Tabular = "tnum"

val NumberXL = TextStyle(
    fontFamily = HeadingFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 36.sp,
    lineHeight = 40.sp,
    fontFeatureSettings = Tabular,
)
val HeadlineL = TextStyle(
    fontFamily = HeadingFont,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
)
val HeadlineM = TextStyle(
    fontFamily = HeadingFont,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
)
val Title = TextStyle(
    fontFamily = HeadingFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
)
val BodyL = TextStyle(
    fontFamily = BodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)
val BodyM = TextStyle(
    fontFamily = BodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)
val LabelL = TextStyle(
    fontFamily = BodyFont,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)
val LabelS = TextStyle(
    fontFamily = BodyFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
)

val Typography = Typography(
    headlineMedium = HeadlineL,
    headlineSmall = HeadlineM,
    titleLarge = Title,
    bodyLarge = BodyL,
    bodyMedium = BodyM,
    labelLarge = LabelL,
    labelSmall = LabelS,
)
