package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrightGold,
    onPrimary = MidnightTealDark,
    primaryContainer = WarmGoldContainerDark,
    onPrimaryContainer = BrightGold,
    secondary = Color(0xFF8FC1D4),
    onSecondary = MidnightTealDark,
    secondaryContainer = MidnightTealElevatedDark,
    onSecondaryContainer = Color(0xFFE0F0F6),
    tertiary = NativityBurgundyLight,
    onTertiary = Color(0xFF3E0E12),
    tertiaryContainer = NativityBurgundyContainerDark,
    onTertiaryContainer = Color(0xFFFFDADE),
    background = MidnightTealSurfaceDark,
    onBackground = Color(0xFFF7F2E7),
    surface = MidnightTealCardDark,
    onSurface = Color(0xFFF7F2E7),
    surfaceVariant = MidnightTealElevatedDark,
    onSurfaceVariant = Color(0xFFB9CBD2),
    outline = Color(0xFF33505E),
    outlineVariant = Color(0xFF233B46)
)

private val LightColorScheme = lightColorScheme(
    primary = MidnightTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E6ED),
    onPrimaryContainer = MidnightTealDark,
    secondary = WarmGold,
    onSecondary = Color.White,
    secondaryContainer = WarmGoldContainer,
    onSecondaryContainer = Color(0xFF473308),
    tertiary = NativityBurgundy,
    onTertiary = Color.White,
    tertiaryContainer = NativityBurgundyContainer,
    onTertiaryContainer = Color(0xFF471116),
    background = WarmParchment,
    onBackground = DeepInk,
    surface = PureIvoryCard,
    onSurface = DeepInk,
    surfaceVariant = SoftParchmentVariant,
    onSurfaceVariant = MutedInk,
    outline = OutlineParchment,
    outlineVariant = Color(0xFFE8DFCE)
)

@Composable
fun NativityFastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
