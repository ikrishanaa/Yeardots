package com.krishana.onedot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Renders a pixel-exact preview of the lock-screen wallpaper.
 *
 * Mirrors [WallpaperGenerator.generateBitmap] exactly: device screen ratio,
 * dynamic responsive columns, cell-fit dot sizing, grid layout fractions,
 * today-glow and the bottom progress text — so the preview always matches
 * what actually gets applied to the lock screen.
 */
@Composable
fun WallpaperPreview(
    pastColor: Color,
    todayColor: Color,
    futureColor: Color,
    backgroundColor: Color,
    dotShape: String = "dot",
    dotDensity: Int = 2, // 0=Tiny, 1=Small, 2=Medium, 3=Large
    gridWidthFraction: Float = 0.84f,
    gridHeightFraction: Float = 0.55f,
    gridOffsetX: Float = 0f,
    gridOffsetY: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Exact device screen ratio — same dimensions the generator renders at
    val context = LocalContext.current
    val displayMetrics = remember(context) { context.resources.displayMetrics }
    val screenRatio = displayMetrics.widthPixels.toFloat() / displayMetrics.heightPixels.toFloat()

    val today = remember { LocalDate.now() }
    val currentDay = today.dayOfYear
    val daysInYear = if (today.isLeapYear) 366 else 365
    val daysLeft = daysInYear - currentDay
    val percent = ((currentDay.toFloat() / daysInYear) * 100).toInt()

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .aspectRatio(screenRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ── Identical grid geometry to WallpaperGenerator ────────────
            val w = size.width
            val h = size.height

            val gridWidth  = w * gridWidthFraction
            val gridHeight = h * gridHeightFraction
            val ar = if (gridHeight > 0f) gridWidth / gridHeight else 1f
            val columns = sqrt(daysInYear.toFloat() * ar).roundToInt().coerceIn(5, 25)
            val rows = ceil(daysInYear.toFloat() / columns).toInt()
            val cellSize = min(gridWidth / columns, gridHeight / rows)

            val densityMultiplier = when (dotDensity) {
                0 -> 0.70f // Tiny
                1 -> 1.00f // Small (default)
                2 -> 1.30f // Medium
                3 -> 1.60f // Large
                else -> 1.00f
            }
            val dotRadius   = cellSize * 0.28f * densityMultiplier
            val dotDiameter = dotRadius * 2f

            val startX = w * (0.5f + gridOffsetX) - gridWidth  / 2f
            val startY = h * (0.5f + gridOffsetY) - gridHeight / 2f

            // ── Dots ─────────────────────────────────────────────────────
            for (day in 1..daysInYear) {
                val row = (day - 1) / columns
                val col = (day - 1) % columns
                val cx = startX + col * cellSize + cellSize / 2f
                val cy = startY + row * cellSize + cellSize / 2f

                val color = when {
                    day < currentDay -> pastColor
                    day == currentDay -> todayColor
                    else -> futureColor
                }

                // Today's glow — same geometry as the generator's glowPaint
                if (day == currentDay) {
                    when (dotShape) {
                        "square" -> {
                            val g = dotDiameter * 1.2f
                            drawRoundRect(
                                color = todayColor.copy(alpha = 0.35f),
                                topLeft = Offset(cx - g / 2f, cy - g / 2f),
                                size = Size(g, g),
                                cornerRadius = CornerRadius(g * 0.05f)
                            )
                        }
                        "rounded" -> {
                            val g = dotDiameter * 1.2f
                            drawRoundRect(
                                color = todayColor.copy(alpha = 0.35f),
                                topLeft = Offset(cx - g / 2f, cy - g / 2f),
                                size = Size(g, g),
                                cornerRadius = CornerRadius(g * 0.30f)
                            )
                        }
                        "pill" -> {
                            val gw = dotDiameter * 1.2f
                            val gh = dotDiameter * 0.7f
                            drawRoundRect(
                                color = todayColor.copy(alpha = 0.35f),
                                topLeft = Offset(cx - gw / 2f, cy - gh / 2f),
                                size = Size(gw, gh),
                                cornerRadius = CornerRadius(gh / 2f)
                            )
                        }
                        else -> drawCircle(todayColor.copy(alpha = 0.35f), dotRadius * 1.25f, Offset(cx, cy))
                    }
                }

                // Main dot — identical shapes to WallpaperGenerator
                when (dotShape) {
                    "square" -> drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - dotRadius, cy - dotRadius),
                        size = Size(dotDiameter, dotDiameter),
                        cornerRadius = CornerRadius(dotDiameter * 0.05f)
                    )
                    "rounded" -> drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - dotRadius, cy - dotRadius),
                        size = Size(dotDiameter, dotDiameter),
                        cornerRadius = CornerRadius(dotDiameter * 0.35f)
                    )
                    "pill" -> {
                        val pw = dotDiameter * 1.1f
                        val ph = dotDiameter * 0.6f
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(cx - pw / 2f, cy - ph / 2f),
                            size = Size(pw, ph),
                            cornerRadius = CornerRadius(ph / 2f)
                        )
                    }
                    else -> drawCircle(color = color, radius = dotRadius, center = Offset(cx, cy))
                }
            }

            // ── Bottom progress text — identical to generator ────────────
            val text = "$daysLeft days \u2022 $percent% Complete"
            val layout = textMeasurer.measure(
                text = text,
                style = TextStyle(
                    color = Color(0xFFCCCCCC),
                    fontSize = (w * 0.025f).sp
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (w - layout.size.width) / 2f,
                    y = h * 0.92f - layout.size.height
                )
            )
        }
    }
}
