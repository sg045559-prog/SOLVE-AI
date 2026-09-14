package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LeafDarkPrimary,
    onPrimary = Color(0xFF003822),
    primaryContainer = LeafGreenSecondary,
    onPrimaryContainer = LeafGreenUltraLight,
    secondary = LeafDarkSecondary,
    onSecondary = Color(0xFF003822),
    secondaryContainer = LeafDarkSurfaceVariant,
    onSecondaryContainer = LeafDarkTextPrimary,
    tertiary = LeafDarkTertiary,
    onTertiary = Color(0xFF003822),
    background = LeafDarkBackground,
    onBackground = LeafDarkTextPrimary,
    surface = LeafDarkSurface,
    onSurface = LeafDarkTextPrimary,
    surfaceVariant = LeafDarkSurfaceVariant,
    onSurfaceVariant = LeafDarkTextSecondary,
    outline = LeafDarkOutline,
    outlineVariant = Color(0xFF1E3A2B)
)

private val LightColorScheme = lightColorScheme(
    primary = LeafGreenSecondary,
    onPrimary = Color.White,
    primaryContainer = LeafGreenUltraLight,
    onPrimaryContainer = LeafGreenPrimary,
    secondary = LeafGreenTertiary,
    onSecondary = Color.White,
    secondaryContainer = LeafLightSurfaceVariant,
    onSecondaryContainer = LeafGreenPrimary,
    tertiary = LeafGreenAccent,
    onTertiary = Color.White,
    background = LeafLightBackground,
    onBackground = LeafLightTextPrimary,
    surface = LeafLightSurface,
    onSurface = LeafLightTextPrimary,
    surfaceVariant = LeafLightSurfaceVariant,
    onSurfaceVariant = LeafLightTextSecondary,
    outline = LeafLightOutline,
    outlineVariant = Color(0xFFD3E4DA)
)

@Composable
fun SolveAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep leaf branding primary by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
