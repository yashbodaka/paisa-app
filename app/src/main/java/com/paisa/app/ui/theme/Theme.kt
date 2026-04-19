package com.paisa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF146C43),
    onPrimary = Color.White,
    secondary = Color(0xFFB94E48),
    onSecondary = Color.White,
    tertiary = Color(0xFF316B83),
    background = Color(0xFFFBFCF8),
    onBackground = Color(0xFF151915),
    surface = Color.White,
    onSurface = Color(0xFF151915),
    surfaceVariant = Color(0xFFE5EEE8),
    onSurfaceVariant = Color(0xFF3C463F),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD89F),
    onPrimary = Color(0xFF00391F),
    secondary = Color(0xFFFFB4AC),
    onSecondary = Color(0xFF690006),
    tertiary = Color(0xFF9ED0E3),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE1E4DE),
    surface = Color(0xFF181C18),
    onSurface = Color(0xFFE1E4DE),
    surfaceVariant = Color(0xFF3F4941),
    onSurfaceVariant = Color(0xFFC1C9BE),
    error = Color(0xFFFFB4AB)
)

@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

