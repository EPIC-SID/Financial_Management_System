package com.example.financeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.financeapp.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)
val HeadlineSerif = FontFamily(Font(R.font.libre_baskerville_regular))
val DevanagariSerif = FontFamily(Font(R.font.eczar_regular))
val DevanagariSans = FontFamily(Font(R.font.noto_sans_devanagari_regular))

val NumberHero = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 40.sp,
    lineHeight = 48.sp,
    fontFeatureSettings = "tnum, lnum"
)
val NumberXL = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 32.sp,
    lineHeight = 40.sp,
    fontFeatureSettings = "tnum"
)
val NumberCompact = TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = "tnum"
)
val DevanagariHeadline = TextStyle(
    fontFamily = DevanagariSerif,
    fontSize = 32.sp,
    lineHeight = 48.sp
)
val DevanagariBody = TextStyle(
    fontFamily = DevanagariSans,
    fontSize = 16.sp,
    lineHeight = 28.sp
)

private val BaseTypography = Typography(
    displayLarge = TextStyle(fontFamily = HeadlineSerif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 52.sp),
    displayMedium = TextStyle(fontFamily = HeadlineSerif, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = HeadlineSerif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = HeadlineSerif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

val Typography = BaseTypography.run {
    fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum, lnum")
    copy(
        headlineMedium = headlineMedium.tabular(), headlineSmall = headlineSmall.tabular(),
        titleLarge = titleLarge.tabular(), titleMedium = titleMedium.tabular(), titleSmall = titleSmall.tabular(),
        bodyLarge = bodyLarge.tabular(), bodyMedium = bodyMedium.tabular(), bodySmall = bodySmall.tabular(),
        labelLarge = labelLarge.tabular(), labelMedium = labelMedium.tabular(), labelSmall = labelSmall.tabular()
    )
}

// Aliases for backward compatibility with existing components
val HeadlineL = Typography.headlineMedium
val HeadlineM = Typography.headlineSmall
val Title = Typography.titleLarge
val BodyL = Typography.bodyLarge
val BodyM = Typography.bodyMedium
val LabelL = Typography.labelLarge
val LabelS = Typography.labelSmall
