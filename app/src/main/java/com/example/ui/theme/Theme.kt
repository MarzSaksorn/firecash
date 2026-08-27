package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val FireCashDarkColorScheme = darkColorScheme(
    primary = FireCashPrimary,
    onPrimary = FireCashOnPrimary,
    primaryContainer = FireCashPrimaryContainer,
    onPrimaryContainer = FireCashOnPrimaryContainer,
    inversePrimary = FireCashInversePrimary,
    secondary = FireCashSecondary,
    onSecondary = FireCashOnSecondary,
    secondaryContainer = FireCashSecondaryContainer,
    onSecondaryContainer = FireCashOnSecondaryContainer,
    tertiary = FireCashTertiary,
    onTertiary = FireCashOnTertiary,
    tertiaryContainer = FireCashTertiaryContainer,
    onTertiaryContainer = FireCashOnTertiaryContainer,
    background = FireCashBackground,
    onBackground = FireCashOnBackground,
    surface = FireCashSurface,
    onSurface = FireCashOnSurface,
    surfaceVariant = FireCashSurfaceContainerHighest,
    onSurfaceVariant = FireCashOnSurfaceVariant,
    surfaceTint = FireCashPrimary,
    inverseSurface = FireCashInverseSurface,
    inverseOnSurface = FireCashInverseOnSurface,
    error = FireCashError,
    onError = FireCashOnError,
    errorContainer = FireCashErrorContainer,
    onErrorContainer = FireCashOnErrorContainer,
    outline = FireCashOutline,
    outlineVariant = FireCashOutlineVariant,
    surfaceContainerLowest = FireCashSurfaceContainerLowest,
    surfaceContainerLow = FireCashSurfaceContainerLow,
    surfaceContainer = FireCashSurfaceContainer,
    surfaceContainerHigh = FireCashSurfaceContainerHigh,
    surfaceContainerHighest = FireCashSurfaceContainerHighest,
    surfaceDim = FireCashSurfaceDim,
    surfaceBright = FireCashSurfaceBright
)

val FireCashShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FireCashTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FireCashDarkColorScheme,
        typography = Typography,
        shapes = FireCashShapes,
        content = content
    )
}
