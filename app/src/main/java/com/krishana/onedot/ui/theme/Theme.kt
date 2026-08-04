package com.krishana.onedot.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF4C2E00),
    secondaryContainer = Color(0xFFF57C00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFB8F3B9),
    
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFC4C7C5),
    
    outline = Color(0xFF8E918F)
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
