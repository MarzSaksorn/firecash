package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/** Toggle this to switch between dark and light palettes app-wide. */
var isDarkTheme: Boolean = true

// Stitch Design Tokens — Match Firebase UI Stitch plan
// Background: #121316 Deep Obsidian, Primary: #FF6B00 Electric Amber
// Secondary: #10B981 Emerald Green, Tertiary: #6366F1 Indigo
// Error: #EF4444 Coral Red

val FireCashBackground: Color get() = if (isDarkTheme) Color(0xFF121316) else Color(0xFFF8F9FA)
val FireCashOnBackground: Color get() = if (isDarkTheme) Color(0xFFE5E2E1) else Color(0xFF1C1B1F)
val FireCashSurface: Color get() = if (isDarkTheme) Color(0xFF1A1C20) else Color(0xFFFFFBFE)
val FireCashSurfaceDim: Color get() = if (isDarkTheme) Color(0xFF121316) else Color(0xFFDED8E1)
val FireCashSurfaceBright: Color get() = if (isDarkTheme) Color(0xFF2C3036) else Color(0xFFFFFBFE)
val FireCashSurfaceVariant: Color get() = if (isDarkTheme) Color(0xFF22252A) else Color(0xFFE7E0EC)
val FireCashSurfaceContainerLowest: Color get() = if (isDarkTheme) Color(0xFF0E0E11) else Color(0xFFFFFFFF)
val FireCashSurfaceContainerLow: Color get() = if (isDarkTheme) Color(0xFF1A1C20) else Color(0xFFF7F2FA)
val FireCashSurfaceContainer: Color get() = if (isDarkTheme) Color(0xFF1E2126) else Color(0xFFF3EDF7)
val FireCashSurfaceContainerHigh: Color get() = if (isDarkTheme) Color(0xFF22252A) else Color(0xFFEDE7F1)
val FireCashSurfaceContainerHighest: Color get() = if (isDarkTheme) Color(0xFF2C3036) else Color(0xFFE6E0EB)
val FireCashOnSurface: Color get() = if (isDarkTheme) Color(0xFFE5E2E1) else Color(0xFF1C1B1F)
val FireCashOnSurfaceVariant: Color get() = if (isDarkTheme) Color(0xFFC2C6D8) else Color(0xFF49454F)
val FireCashInverseSurface: Color get() = if (isDarkTheme) Color(0xFFE5E2E1) else Color(0xFF1C1B1F)
val FireCashInverseOnSurface: Color get() = if (isDarkTheme) Color(0xFF313030) else Color(0xFFF4EFF4)
val FireCashOutline: Color get() = if (isDarkTheme) Color(0xFF8C90A1) else Color(0xFF79747E)
val FireCashOutlineVariant: Color get() = if (isDarkTheme) Color(0xFF424656) else Color(0xFFCAC4D0)

val FireCashPrimary = Color(0xFFFF6B00)
val FireCashOnPrimary = Color(0xFFFFFFFF)
val FireCashPrimaryContainer: Color get() = if (isDarkTheme) Color(0xFFFF8A3D) else Color(0xFFFF8A3D)
val FireCashOnPrimaryContainer = Color(0xFFFFFFFF)
val FireCashInversePrimary = Color(0xFFFF6B00)

val FireCashSecondary = Color(0xFF10B981)
val FireCashOnSecondary = Color(0xFFFFFFFF)
val FireCashSecondaryContainer: Color get() = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF34D399)
val FireCashOnSecondaryContainer = Color(0xFFFFFFFF)

val FireCashTertiary = Color(0xFF6366F1)
val FireCashOnTertiary = Color(0xFFFFFFFF)
val FireCashTertiaryContainer: Color get() = if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF818CF8)
val FireCashOnTertiaryContainer = Color(0xFFFFFFFF)

val FireCashError = Color(0xFFEF4444)
val FireCashOnError = Color(0xFFFFFFFF)
val FireCashErrorContainer: Color get() = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFF87171)
val FireCashOnErrorContainer = Color(0xFFFFFFFF)