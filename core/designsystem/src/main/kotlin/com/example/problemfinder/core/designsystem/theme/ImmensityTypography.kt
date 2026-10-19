package com.example.problemfinder.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.problemfinder.core.designsystem.R

// Official Inter static fonts. Source version: gradle/libs.versions.toml.
// The SIL Open Font License is bundled in res/raw/inter_font_license.txt.
private val InterFamily = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_bold, weight = FontWeight.Bold),
)

private val BaseTypography = Typography()

val ImmensityTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = InterFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = InterFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = InterFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = InterFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = InterFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = InterFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = InterFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = InterFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = InterFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = InterFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = InterFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = InterFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = InterFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = InterFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = InterFamily),
)
