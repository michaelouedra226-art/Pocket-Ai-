package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepSeekHermesColorScheme = darkColorScheme(
    primary = DeepSeekCyan,
    onPrimary = VoidBlack,
    primaryContainer = DeepSeekCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = StrikeEmerald,
    onSecondary = VoidBlack,
    secondaryContainer = StrikeEmeraldDim,
    onSecondaryContainer = TextPrimary,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force consistent cyber-tactical theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DeepSeekHermesColorScheme,
        typography = Typography,
        content = content
    )
}
