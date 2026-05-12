package com.metrolist.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.desktop.core.DarkModeSetting
import com.metrolist.desktop.core.ThemeManager

private data class SeedPalette(
    val primary: Color, val onPrimary: Color,
    val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color,
    val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color, val onError: Color,
)

@Composable
fun isDesktopDarkTheme(): Boolean {
    return when (ThemeManager.config.darkMode) {
        DarkModeSetting.ON -> true
        DarkModeSetting.OFF -> false
        DarkModeSetting.SYSTEM -> isSystemInDarkTheme()
    }
}

@Composable
fun MetrolistDesktopTheme(
    content: @Composable () -> Unit,
) {
    val isDark = isDesktopDarkTheme()
    val config = ThemeManager.config
    val palette = generateSeedPalette(config.accentColor, isDark)
    val baseScheme = if (isDark) seedPaletteToDarkScheme(palette) else seedPaletteToLightScheme(palette)
    val colorScheme = if (isDark && config.pureBlack) applyPureBlack(baseScheme) else baseScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun generateSeedPalette(seed: Color, isDark: Boolean): SeedPalette {
    val r = seed.red
    val g = seed.green
    val b = seed.blue

    val primary = seed
    val onPrimary = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val primaryContainer = if (isDark) {
        Color(red = (r * 0.3f).coerceIn(0f, 1f), green = (g * 0.3f).coerceIn(0f, 1f), blue = (b * 0.3f).coerceIn(0f, 1f), alpha = 1f)
    } else {
        Color(red = (r + (1f - r) * 0.8f).coerceIn(0f, 1f), green = (g + (1f - g) * 0.8f).coerceIn(0f, 1f), blue = (b + (1f - b) * 0.8f).coerceIn(0f, 1f), alpha = 1f)
    }
    val onPrimaryContainer = if (isDark) primary.copy(alpha = 1f) else {
        Color(red = (r * 0.3f).coerceIn(0f, 1f), green = (g * 0.3f).coerceIn(0f, 1f), blue = (b * 0.3f).coerceIn(0f, 1f), alpha = 1f)
    }

    val secondary = Color(red = ((1f - r) * 0.7f + r * 0.3f).coerceIn(0f, 1f), green = ((1f - g) * 0.7f + g * 0.3f).coerceIn(0f, 1f), blue = (b * 0.7f).coerceIn(0f, 1f), alpha = 1f)
    val onSecondary = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val secondaryContainer = if (isDark) {
        Color(red = (secondary.red * 0.3f).coerceIn(0f, 1f), green = (secondary.green * 0.3f).coerceIn(0f, 1f), blue = (secondary.blue * 0.3f).coerceIn(0f, 1f), alpha = 1f)
    } else {
        Color(red = (secondary.red + (1f - secondary.red) * 0.8f).coerceIn(0f, 1f), green = (secondary.green + (1f - secondary.green) * 0.8f).coerceIn(0f, 1f), blue = (secondary.blue + (1f - secondary.blue) * 0.8f).coerceIn(0f, 1f), alpha = 1f)
    }

    val tertiary = Color(red = (b * 0.7f + g * 0.3f).coerceIn(0f, 1f), green = (r * 0.5f).coerceIn(0f, 1f), blue = ((1f - b) * 0.7f + b * 0.3f).coerceIn(0f, 1f), alpha = 1f)
    val onTertiary = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

    val bg = if (isDark) Color(0xFF121212) else Color(0xFFFFF8F0)
    val onBg = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1C1B1F)
    val surface = if (isDark) Color(0xFF1A1A2E) else Color(0xFFFFF8F0)
    val onSurface = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1C1B1F)
    val surfaceVariant = if (isDark) Color(0xFF2D2D44) else Color(0xFFF0E6D8)
    val onSurfaceVariant = if (isDark) Color(0xFF9E9E9E) else Color(0xFF6E6E6E)
    val outline = if (isDark) Color(0xFF444466) else Color(0xFFD0C8B8)
    val error = Color(0xFFCF6679)
    val onError = Color(0xFF000000)

    return SeedPalette(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSurface,
        tertiary = tertiary, onTertiary = onTertiary,
        background = bg, onBackground = onBg,
        surface = surface, onSurface = onSurface,
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        error = error, onError = onError,
    )
}

private fun seedPaletteToDarkScheme(p: SeedPalette) = darkColorScheme(
    primary = p.primary, onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer, onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary, onSecondary = p.onSecondary,
    secondaryContainer = p.secondaryContainer, onSecondaryContainer = p.onSecondaryContainer,
    tertiary = p.tertiary, onTertiary = p.onTertiary,
    background = p.background, onBackground = p.onBackground,
    surface = p.surface, onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant, onSurfaceVariant = p.onSurfaceVariant,
    outline = p.outline,
    error = p.error, onError = p.onError,
)

private fun seedPaletteToLightScheme(p: SeedPalette) = lightColorScheme(
    primary = p.primary, onPrimary = p.onPrimary,
    primaryContainer = p.primaryContainer, onPrimaryContainer = p.onPrimaryContainer,
    secondary = p.secondary, onSecondary = p.onSecondary,
    secondaryContainer = p.secondaryContainer, onSecondaryContainer = p.onSecondaryContainer,
    tertiary = p.tertiary, onTertiary = p.onTertiary,
    background = p.background, onBackground = p.onBackground,
    surface = p.surface, onSurface = p.onSurface,
    surfaceVariant = p.surfaceVariant, onSurfaceVariant = p.onSurfaceVariant,
    outline = p.outline,
    error = p.error, onError = p.onError,
)

private fun applyPureBlack(scheme: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme {
    return scheme.copy(surface = Color.Black, background = Color.Black)
}

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)
