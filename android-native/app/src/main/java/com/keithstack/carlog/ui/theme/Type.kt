@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.keithstack.carlog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.keithstack.carlog.R

// Both files are variable fonts (wght axis) decompressed from the original
// app's bundled woff2 resources — see android-native/app/src/main/res/font.
val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond, weight = FontWeight.Normal),
    Font(
        R.font.cormorant_garamond,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

val Lora = FontFamily(
    Font(R.font.lora, weight = FontWeight.Normal),
    Font(
        R.font.lora,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

val CarLogTypography = Typography(
    headlineLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Normal, fontSize = 11.sp),
)
