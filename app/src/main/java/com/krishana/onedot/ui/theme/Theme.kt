package com.krishana.onedot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFFFF9800),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

object YearDotsColorScheme {
    @Composable
    fun generateColors(): List<Color> {
        return listOf(
            Color(0xFF1976D2), // Primary - Today
            Color(0xFF757575), // Secondary - Past
            Color(0xFFBDBDBD), // Tertiary - Future
            Color(0xFFFF9800)  // Accent
        )
    }
}

@Composable
fun OneDotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
