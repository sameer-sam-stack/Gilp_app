package com.glip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GlipGreenPrimary,
    onPrimary = Color(0xFF18181B),
    primaryContainer = GlipGreenLight,
    onPrimaryContainer = Color(0xFF18181B),
    secondary = GlipOrangeAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = GlipBlue,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = ErrorRed,
    errorContainer = ErrorRedLight,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GlipGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A2B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GlipOrangeAccent,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF383838),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun GlipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
