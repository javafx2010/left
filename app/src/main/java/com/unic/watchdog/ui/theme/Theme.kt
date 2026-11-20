package com.unic.watchdog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryDark,
    secondary = BrandAccent,
    onSecondary = Color.White,
    tertiary = BrandPrimarySoft,
    surface = Color(0xFF1E2A30),
    onSurface = Color(0xFFE6EDF0)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandPrimarySoft,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BrandAccentSoft,
    tertiary = BrandPrimaryDark,
    background = Color(0xFFF4F8F9),
    onBackground = Ink,
    surface = Color(0xFFFFFFFF),
    onSurface = Ink,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = SubInk,
    outline = LineColor
)

@Composable
fun WatchDogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 原型为浅色品牌设计，固定使用品牌配色以保证视觉一致。
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
