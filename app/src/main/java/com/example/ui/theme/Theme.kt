package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val FireCashDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF3B1800),
    primaryContainer = Color(0xFF733500),
    onPrimaryContainer = Color(0xFFFFDBBE),
    inversePrimary = Color(0xFF9C4600),
    secondary = Color(0xFF64D7A2),
    onSecondary = Color(0xFF003823),
    secondaryContainer = Color(0xFF075C40),
    onSecondaryContainer = Color(0xFF84F5BE),
    tertiary = Color(0xFFB5C4FF),
    onTertiary = Color(0xFF1D2E68),
    tertiaryContainer = Color(0xFF374986),
    onTertiaryContainer = Color(0xFFDCE2FF),
    background = Color(0xFF0D1114),
    onBackground = Color(0xFFF4F6F8),
    surface = Color(0xFF151A1E),
    onSurface = Color(0xFFF4F6F8),
    surfaceVariant = Color(0xFF242C32),
    onSurfaceVariant = Color(0xFFB9C3CB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF89959E),
    outlineVariant = Color(0xFF414B53),
    surfaceContainerLowest = Color(0xFF080B0D),
    surfaceContainerLow = Color(0xFF12171B),
    surfaceContainer = Color(0xFF1B2126),
    surfaceContainerHigh = Color(0xFF20282E),
    surfaceContainerHighest = Color(0xFF2B343B),
    surfaceDim = Color(0xFF0D1114),
    surfaceBright = Color(0xFF30383F),
    inverseSurface = Color(0xFFE1E6EA),
    inverseOnSurface = Color(0xFF2B3034)
)

private val FireCashLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C4600),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBBF),
    onPrimaryContainer = Color(0xFF351000),
    inversePrimary = Color(0xFFFFB870),
    secondary = Color(0xFF006C4C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF91F8C7),
    onSecondaryContainer = Color(0xFF002116),
    tertiary = Color(0xFF4E5F9F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE2FF),
    onTertiaryContainer = Color(0xFF07174F),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF211A16),
    surface = Color(0xFFFFFBF8),
    onSurface = Color(0xFF211A16),
    surfaceVariant = Color(0xFFF2DED1),
    onSurfaceVariant = Color(0xFF56443A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF88746A),
    outlineVariant = Color(0xFFD9C3B8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF4EE),
    surfaceContainer = Color(0xFFF9EEE8),
    surfaceContainerHigh = Color(0xFFF3E8E2),
    surfaceContainerHighest = Color(0xFFEDE2DC),
    surfaceDim = Color(0xFFE8DFDB),
    surfaceBright = Color(0xFFFFFBF8),
    inverseSurface = Color(0xFF362F2B),
    inverseOnSurface = Color(0xFFFFEDE5)
)

val FireCashShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FireCashTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FireCashDarkColorScheme else FireCashLightColorScheme,
        typography = Typography,
        shapes = FireCashShapes,
        content = content
    )
}
