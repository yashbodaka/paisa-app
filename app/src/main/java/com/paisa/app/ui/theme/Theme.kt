package com.paisa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paisa.app.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF5D4037), // Deep Espresso Brown (Income/Primary)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFEBE9), // Softest Taupe
    onPrimaryContainer = Color(0xFF211512),
    
    secondary = Color(0xFFD2B48C), // Tan (Borrowed)
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFFF5EEDC),
    onSecondaryContainer = Color(0xFF5D4037),

    tertiary = Color(0xFF9E7676), // Rosy Brown (Lent)
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E5E5),
    onTertiaryContainer = Color(0xFF593D3D),
    
    background = Color(0xFFFCF9F2), // Linen White
    onBackground = Color(0xFF261D1A), 
    
    surface = Color(0xFFF7F1E5), // Creamy Surface
    onSurface = Color(0xFF261D1A),
    
    surfaceVariant = Color(0xFFEDE4D3), 
    onSurfaceVariant = Color(0xFF5D4037),
    
    outline = Color(0xFF4E342E), // Rich Umber for hand-drawn borders
    
    error = Color(0xFF8C1D18), 
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7CCC8), // Soft Latte (Income)
    onPrimary = Color(0xFF3E2723), 
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFF5EEDC),
    
    secondary = Color(0xFFD2B48C), // Tan (Borrowed)
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF4E342E),
    onSecondaryContainer = Color(0xFFEDE4D3),
    
    tertiary = Color(0xFFD4B3B3), // Light Rosy Brown (Lent)
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF3C302B),
    onTertiaryContainer = Color(0xFFD7CCC8),
    
    background = Color(0xFF1B1411), // Midnight Roasted Coffee
    onBackground = Color(0xFFF5EEDC),
    
    surface = Color(0xFF261D1A), // Dark Espresso
    onSurface = Color(0xFFF5EEDC),
    
    surfaceVariant = Color(0xFF3C302B),
    onSurfaceVariant = Color(0xFFD7CCC8),
    
    outline = Color(0xFF8D6E63), // Muted Copper for dark borders
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

val PlaytimeFont = FontFamily(Font(R.font.playtime))
val PalitoonFont = FontFamily(Font(R.font.palitoon))

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = PlaytimeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlaytimeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PalitoonFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PalitoonFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PalitoonFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlaytimeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
)

@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
