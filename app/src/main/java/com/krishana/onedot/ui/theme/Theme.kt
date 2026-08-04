package com.krishana.onedot.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Premium AMOLED Dark Scheme ────────────────────────────────────────────────
// Curated fallback for devices without dynamic color support (pre-Android 12).
// Uses M3 tonal surfaces instead of flat dark colors for depth.
private val DarkColorScheme = darkColorScheme(
    // Primary — a soft, legible blue
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD1E4FF),

    // Secondary — warm amber accent
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF4C2E00),
    secondaryContainer = Color(0xFFF57C00),
    onSecondaryContainer = Color(0xFFFFDDB3),

    // Tertiary — fresh green for highlights
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFB8F3B9),

    // Background & Surface — true AMOLED black with tonal hierarchy
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Surface tonal hierarchy — M3 standard tonal surfaces
    surfaceTint = Color(0xFF90CAF9),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF252525),

    // Outline
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    // Error
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Inverse
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF0061A4),
)


// ── Theme Entry Point ─────────────────────────────────────────────────────────
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
        typography = YearDotsTypography,
        content = content
    )
}
