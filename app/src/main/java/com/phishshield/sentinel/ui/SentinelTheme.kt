package com.phishshield.sentinel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF74FB38), onPrimary = Color(0xFF102400),
    primaryContainer = Color(0xFF23451B), onPrimaryContainer = Color(0xFFC0FF9E),
    secondary = Color(0xFFB3D3A4), onSecondary = Color(0xFF1F351A),
    secondaryContainer = Color(0xFF293F24), onSecondaryContainer = Color(0xFFD1EDC1),
    tertiary = Color(0xFF9CD5C2), background = Color(0xFF010B1E),
    surface = Color(0xFF010B1E), surfaceContainer = Color(0xFF111D30),
    surfaceContainerLow = Color(0xFF0A1628), surfaceContainerHigh = Color(0xFF19263A),
    surfaceContainerHighest = Color(0xFF223047), surfaceVariant = Color(0xFF223047),
    onBackground = Color(0xFFF0F4F8), onSurface = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFFB7C5D1), outline = Color(0xFF7F939F),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF2B650C), onPrimary = Color.White,
    primaryContainer = Color(0xFF74FB38), onPrimaryContainer = Color(0xFF102400),
    secondary = Color(0xFF49633E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7EDC9), onSecondaryContainer = Color(0xFF18330D),
    tertiary = Color(0xFF236B52), background = Color(0xFFF5F9F1),
    surface = Color(0xFFF5F9F1), surfaceContainer = Color(0xFFE8F0E2),
    surfaceContainerLow = Color(0xFFF0F6E9), surfaceContainerHigh = Color(0xFFE0EAD9),
    surfaceContainerHighest = Color(0xFFD9E3D2), surfaceVariant = Color(0xFFE0EAD9),
    onBackground = Color(0xFF14212B), onSurface = Color(0xFF14212B),
    onSurfaceVariant = Color(0xFF415765),
    outline = Color(0xFF6D8066), outlineVariant = Color(0xFFC0CDB7),
)

@Composable
fun SentinelTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
