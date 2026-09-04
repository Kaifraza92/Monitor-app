package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ZuhraRedPrimary,
    onPrimary = Color.White,
    primaryContainer = ZuhraRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE57373),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF492524),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE2E8F0),
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = StatusFailed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ZuhraCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = ZuhraRedDark,
    secondary = Color(0xFF775655),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1515),
    tertiary = Color(0xFF475569),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = StatusFailed,
    onError = Color.White
)

@Composable
fun AlzuhraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
