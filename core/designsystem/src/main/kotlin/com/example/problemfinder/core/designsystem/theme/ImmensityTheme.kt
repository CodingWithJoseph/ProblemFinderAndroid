package com.example.problemfinder.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightScheme = lightColorScheme(
    primary = ImmensityColors.Ink,
    onPrimary = Color.White,
    primaryContainer = ImmensityColors.DarkSurface,
    onPrimaryContainer = ImmensityColors.OnDark,
    inversePrimary = ImmensityColors.OnDark,

    secondary = Color(0xFFB84700),
    onSecondary = Color.White,
    secondaryContainer = ImmensityColors.AccentSoft,
    onSecondaryContainer = Color(0xFF5C2400),

    tertiary = Color(0xFF73533D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DDC9),
    onTertiaryContainer = Color(0xFF3B2618),

    background = ImmensityColors.Canvas,
    onBackground = ImmensityColors.Ink,
    surface = ImmensityColors.Cream,
    onSurface = ImmensityColors.Ink,
    surfaceVariant = Color(0xFFEEE7DE),
    onSurfaceVariant = ImmensityColors.Muted,
    surfaceTint = ImmensityColors.Ink,
    inverseSurface = ImmensityColors.DarkSurface,
    inverseOnSurface = ImmensityColors.OnDark,

    error = ImmensityColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E5),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF8A8178),
    outlineVariant = ImmensityColors.Border,
    scrim = Color.Black,

    surfaceBright = ImmensityColors.Cream,
    surfaceDim = Color(0xFFE9E1D7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCF9F3),
    surfaceContainer = Color(0xFFF6F1E9),
    surfaceContainerHigh = Color(0xFFEEE7DE),
    surfaceContainerHighest = Color(0xFFE6DDD1)
).withImmensityFixedRoles()

private val DarkScheme = darkColorScheme(
    primary = ImmensityColors.Orange,
    onPrimary = ImmensityColors.Ink,
    primaryContainer = Color(0xFF4B443D),
    onPrimaryContainer = ImmensityColors.OnDark,
    inversePrimary = Color(0xFF51463D),

    secondary = Color(0xFFFF9A55),
    onSecondary = Color(0xFF3B1700),
    secondaryContainer = Color(0xFF683308),
    onSecondaryContainer = Color(0xFFFFE0C5),

    tertiary = Color(0xFFE4C1A3),
    onTertiary = Color(0xFF3A2416),
    tertiaryContainer = Color(0xFF5D3F2B),
    onTertiaryContainer = Color(0xFFF8DCC3),

    background = Color(0xFF171614),
    onBackground = ImmensityColors.OnDark,
    surface = Color(0xFF211F1C),
    onSurface = ImmensityColors.OnDark,
    surfaceVariant = Color(0xFF433D37),
    onSurfaceVariant = Color(0xFFD0C5B8),
    surfaceTint = ImmensityColors.OnDark,
    inverseSurface = Color(0xFFEAE2D8),
    inverseOnSurface = ImmensityColors.DarkSurface,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA79B8F),
    outlineVariant = Color(0xFF5A524B),
    scrim = Color.Black,

    surfaceBright = Color(0xFF49423B),
    surfaceDim = Color(0xFF171614),
    surfaceContainerLowest = Color(0xFF1B1916),
    surfaceContainerLow = Color(0xFF272420),
    surfaceContainer = ImmensityColors.DarkSurface,
    surfaceContainerHigh = Color(0xFF38332D),
    surfaceContainerHighest = Color(0xFF433C35)
).withImmensityFixedRoles()

// Fixed roles deliberately retain the same colors in both schemes, including tab selection colors.
private fun ColorScheme.withImmensityFixedRoles(): ColorScheme = copy(
    primaryFixed = Color(0xFFEAE2D8),
    primaryFixedDim = Color(0xFFCFC5B8),
    onPrimaryFixed = ImmensityColors.Ink,
    onPrimaryFixedVariant = Color(0xFF514A43),

    secondaryFixed = Color(0xFFFFE0C5),
    secondaryFixedDim = Color(0xFFFFB57E),
    onSecondaryFixed = Color(0xFF3B1700),
    onSecondaryFixedVariant = Color(0xFF7B3600),

    tertiaryFixed = Color(0xFFF1DCC8),
    tertiaryFixedDim = Color(0xFFD7BFA8),
    onTertiaryFixed = Color(0xFF341E12),
    onTertiaryFixedVariant = Color(0xFF614733)
)

/** Applies the app's light/dark Material colors and shared Inter typography to all feature content. */
@Composable
fun ImmensityTheme(content: @Composable () -> Unit) {

    val immensityScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = immensityScheme,
        typography = ImmensityTypography,
        content = content,
    )
}
