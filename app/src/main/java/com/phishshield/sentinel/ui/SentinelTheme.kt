package com.phishshield.sentinel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF63E6BE), onPrimary = Color(0xFF00382A),
    secondary = Color(0xFFA8C7FA), background = Color(0xFF101820),
    surface = Color(0xFF101820), surfaceContainer = Color(0xFF1B2732),
    onBackground = Color(0xFFF0F4F8), onSurface = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFFB7C5D1), outline = Color(0xFF7F939F),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF006C50), onPrimary = Color.White,
    secondary = Color(0xFF315E96), background = Color(0xFFF5F8FA),
    surface = Color(0xFFF5F8FA), surfaceContainer = Color(0xFFE7EEF2),
    onBackground = Color(0xFF14212B), onSurface = Color(0xFF14212B),
    onSurfaceVariant = Color(0xFF415765),
)

@Composable
fun SentinelTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
