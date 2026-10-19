package com.example.problemfinder.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Spotlight's text roles are separate from the app's general Material type scale.
object DiscoveryTypography {
    val dashboardEyebrow
        @Composable get() = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp, lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp,
        )
    val dashboardTitle
        @Composable get() = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 42.sp, lineHeight = 45.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp,
        )
    val featuredTitle
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 30.sp, lineHeight = 33.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp,
        )
    val featuredDescription
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.sp,
        )
    val featuredBadge
        @Composable get() = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp, lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
    val collectionTitle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(
            fontSize = 20.sp, lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
        )
    val subtitle
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
        )
    val cardTitle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(
            fontSize = 19.sp, lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
        )
    val description
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp, lineHeight = 19.sp, letterSpacing = 0.sp,
        )
    val categoryLabel
        @Composable get() = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp, lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
    val metadata
        @Composable get() = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.sp,
        )
    val activityDate
        @Composable get() = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp,
        )
    val action
        @Composable get() = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
        )
    val seeAll
        @Composable get() = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.sp,
        )
}

// Featured placement has its own emphasis treatment; topic colors remain on collection cards.
object DiscoverySpotlightColors {
    val surface
        @Composable get() = if (isSystemInDarkTheme())
            Color(0xFF313C28)
        else
            Color(0xFF252A22)
    val foreground = Color(0xFFF3F5E9)
    val supportingText = Color(0xFFE0E5D4)
    val accent = Color(0xFFD8ED9A)
    val ring = accent.copy(alpha = 0.33f)
}

data class OpportunityPalette (
    val surface: Color,
    val foreground: Color,
    val secondaryText: Color,
    val border: Color,
    val actionContainer: Color,
    val actionContent: Color,
    val removeContainer: Color,
    val removeContent: Color,
)

private val LightOpportunityPalette = OpportunityPalette(
    surface = Color.White,
    foreground = Color(0xFF242621),
    secondaryText = Color(0xFF64675D),
    border = Color(0xFFE4E5DC),
    actionContainer = Color(0xFF242621),
    actionContent = Color(0xFFF7F6F2),
    removeContainer = Color(0xFFFCE8DC),
    removeContent = Color(0xFFB54A1E),
)

private val DarkOpportunityPalette = OpportunityPalette(
    surface = Color(0xFF252622),
    foreground = Color(0xFFF4F4EC),
    secondaryText = Color(0xFFBCBFB0),
    border = Color(0xFF3D4035),
    actionContainer = Color(0xFFF4F4EC),
    actionContent = Color(0xFF171815),
    removeContainer = Color(0xFF553323),
    removeContent = Color(0xFFFFAC83),
)

private val LightFeaturePalette = OpportunityPalette(
    surface = Color(0xFF252A22),
    foreground = Color(0xFFF3F5E9),
    secondaryText = Color(0xFFE0E5D4),
    border = Color(0x55D8ED9A),
    actionContainer = Color(0xFFD8ED9A),
    actionContent = Color(0xFF252A22),
    removeContainer = Color(0xFFFCE8DC),
    removeContent = Color(0xFFB54A1E),
)

private val DarkFeaturePalette = OpportunityPalette(
    surface = Color(0xFF313C28),
    foreground = Color(0xFFF3F5E9),
    secondaryText = Color(0xFFE0E5D4),
    border = Color(0x55D8ED9A),
    actionContainer = Color(0xFFD8ED9A),
    actionContent = Color(0xFF313C28),
    removeContainer = Color(0xFF553323),
    removeContent = Color(0xFFFFAC83),
)

/** Shared neutral card/action colors chosen from the current system theme. */
@Composable
fun opportunityPalette(): OpportunityPalette =
    if (isSystemInDarkTheme()) DarkOpportunityPalette else LightOpportunityPalette

/** Featured-card treatment uses the same color roles as regular cards with stronger emphasis. */
@Composable
fun featurePalette(): OpportunityPalette =
    if (isSystemInDarkTheme()) DarkFeaturePalette else LightFeaturePalette
