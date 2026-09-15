package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FireCashColorScheme = darkColorScheme(
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
    surfaceVariant = FireCashSurfaceVariant,
    onSurfaceVariant = FireCashOnSurfaceVariant,
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

private val FireCashLightScheme = lightColorScheme(
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
    surfaceVariant = FireCashSurfaceVariant,
    onSurfaceVariant = FireCashOnSurfaceVariant,
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
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FireCashColorScheme else FireCashLightScheme,
        typography = Typography,
        shapes = FireCashShapes,
        content = content
    )
}