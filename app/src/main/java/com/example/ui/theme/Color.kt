package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** The live app is dark-first, while keeping a readable light palette for settings. */
var isDarkTheme: Boolean = true

// Dark palette: deep graphite surfaces with warm amber actions and cool mint status states.
private val DarkBackground = Color(0xFF0D1114)
private val DarkOnBackground = Color(0xFFF4F6F8)
private val DarkSurface = Color(0xFF151A1E)
private val DarkSurfaceDim = Color(0xFF0D1114)
private val DarkSurfaceBright = Color(0xFF30383F)
private val DarkSurfaceVariant = Color(0xFF242C32)
private val DarkSurfaceLowest = Color(0xFF080B0D)
private val DarkSurfaceLow = Color(0xFF12171B)
private val DarkSurfaceContainer = Color(0xFF1B2126)
private val DarkSurfaceHigh = Color(0xFF20282E)
private val DarkSurfaceHighest = Color(0xFF2B343B)
private val DarkOnSurface = Color(0xFFF4F6F8)
private val DarkOnSurfaceVariant = Color(0xFFB9C3CB)
private val DarkOutline = Color(0xFF89959E)
private val DarkOutlineVariant = Color(0xFF414B53)
private val DarkInverseSurface = Color(0xFFE1E6EA)
private val DarkInverseOnSurface = Color(0xFF2B3034)

// Light palette: the same identity without sacrificing text contrast.
private val LightBackground = Color(0xFFFFFBF8)
private val LightOnBackground = Color(0xFF211A16)
private val LightSurface = Color(0xFFFFFBF8)
private val LightSurfaceDim = Color(0xFFE8DFDB)
private val LightSurfaceBright = Color(0xFFFFFBF8)
private val LightSurfaceVariant = Color(0xFFF2DED1)
private val LightSurfaceLowest = Color(0xFFFFFFFF)
private val LightSurfaceLow = Color(0xFFFFF4EE)
private val LightSurfaceContainer = Color(0xFFF9EEE8)
private val LightSurfaceHigh = Color(0xFFF3E8E2)
private val LightSurfaceHighest = Color(0xFFEDE2DC)
private val LightOnSurface = Color(0xFF211A16)
private val LightOnSurfaceVariant = Color(0xFF56443A)
private val LightOutline = Color(0xFF88746A)
private val LightOutlineVariant = Color(0xFFD9C3B8)
private val LightInverseSurface = Color(0xFF362F2B)
private val LightInverseOnSurface = Color(0xFFFFEDE5)

val FireCashBackground: Color get() = if (isDarkTheme) DarkBackground else LightBackground
val FireCashOnBackground: Color get() = if (isDarkTheme) DarkOnBackground else LightOnBackground
val FireCashSurface: Color get() = if (isDarkTheme) DarkSurface else LightSurface
val FireCashSurfaceDim: Color get() = if (isDarkTheme) DarkSurfaceDim else LightSurfaceDim
val FireCashSurfaceBright: Color get() = if (isDarkTheme) DarkSurfaceBright else LightSurfaceBright
val FireCashSurfaceVariant: Color get() = if (isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant
val FireCashSurfaceContainerLowest: Color get() = if (isDarkTheme) DarkSurfaceLowest else LightSurfaceLowest
val FireCashSurfaceContainerLow: Color get() = if (isDarkTheme) DarkSurfaceLow else LightSurfaceLow
val FireCashSurfaceContainer: Color get() = if (isDarkTheme) DarkSurfaceContainer else LightSurfaceContainer
val FireCashSurfaceContainerHigh: Color get() = if (isDarkTheme) DarkSurfaceHigh else LightSurfaceHigh
val FireCashSurfaceContainerHighest: Color get() = if (isDarkTheme) DarkSurfaceHighest else LightSurfaceHighest
val FireCashOnSurface: Color get() = if (isDarkTheme) DarkOnSurface else LightOnSurface
val FireCashOnSurfaceVariant: Color get() = if (isDarkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant
val FireCashInverseSurface: Color get() = if (isDarkTheme) DarkInverseSurface else LightInverseSurface
val FireCashInverseOnSurface: Color get() = if (isDarkTheme) DarkInverseOnSurface else LightInverseOnSurface
val FireCashOutline: Color get() = if (isDarkTheme) DarkOutline else LightOutline
val FireCashOutlineVariant: Color get() = if (isDarkTheme) DarkOutlineVariant else LightOutlineVariant

val FireCashPrimary: Color get() = if (isDarkTheme) Color(0xFFFFB870) else Color(0xFF9C4600)
val FireCashOnPrimary: Color get() = if (isDarkTheme) Color(0xFF3B1800) else Color.White
val FireCashPrimaryContainer: Color get() = if (isDarkTheme) Color(0xFF733500) else Color(0xFFFFDBBF)
val FireCashOnPrimaryContainer: Color get() = if (isDarkTheme) Color(0xFFFFDBBE) else Color(0xFF351000)
val FireCashInversePrimary: Color get() = if (isDarkTheme) Color(0xFF9C4600) else Color(0xFFFFB870)

val FireCashSecondary: Color get() = if (isDarkTheme) Color(0xFF64D7A2) else Color(0xFF006C4C)
val FireCashOnSecondary: Color get() = if (isDarkTheme) Color(0xFF003823) else Color.White
val FireCashSecondaryContainer: Color get() = if (isDarkTheme) Color(0xFF075C40) else Color(0xFF91F8C7)
val FireCashOnSecondaryContainer: Color get() = if (isDarkTheme) Color(0xFF84F5BE) else Color(0xFF002116)

val FireCashTertiary: Color get() = if (isDarkTheme) Color(0xFFB5C4FF) else Color(0xFF4E5F9F)
val FireCashOnTertiary: Color get() = if (isDarkTheme) Color(0xFF1D2E68) else Color.White
val FireCashTertiaryContainer: Color get() = if (isDarkTheme) Color(0xFF374986) else Color(0xFFDCE2FF)
val FireCashOnTertiaryContainer: Color get() = if (isDarkTheme) Color(0xFFDCE2FF) else Color(0xFF07174F)

val FireCashError: Color get() = if (isDarkTheme) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
val FireCashOnError: Color get() = if (isDarkTheme) Color(0xFF690005) else Color.White
val FireCashErrorContainer: Color get() = if (isDarkTheme) Color(0xFF93000A) else Color(0xFFFFDAD6)
val FireCashOnErrorContainer: Color get() = if (isDarkTheme) Color(0xFFFFDAD6) else Color(0xFF410002)
