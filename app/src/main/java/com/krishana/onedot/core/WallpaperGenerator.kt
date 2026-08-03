package com.krishana.onedot.core

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * Generates a 365-dot wallpaper representing the current year's progress.
 */
object WallpaperGenerator {

    data class GridLayout(
        val widthFraction: Float  = 0.84f, // grid width  as fraction of wallpaper width
        val heightFraction: Float = 0.55f, // grid height as fraction of wallpaper height
        val offsetX: Float = 0f,           // normalised horizontal shift (-0.5 .. +0.5)
        val offsetY: Float = 0f            // normalised vertical   shift (-0.5 .. +0.5)
    )

    data class ThemeConfig(
        val pastColor: Int,
        val todayColor: Int,
        val futureColor: Int,
        val backgroundColor: Int,
        val dotShape: String = "dot", // "dot", "square", "rounded", "pill"
        val dotDensity: Int = 1, // 0=Tiny, 1=Small, 2=Medium, 3=Large
        val gridLayout: GridLayout = GridLayout()
    )


    /**
     * Generates a bitmap with dots arranged in a 15-column grid.
     * 
     * @param width Target width in pixels
     * @param height Target height in pixels
     * @param themeConfig Color configuration
     * @return Generated wallpaper bitmap
     */
    fun generateBitmap(
        width: Int,
        height: Int,
        themeConfig: ThemeConfig
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill background
        canvas.drawColor(themeConfig.backgroundColor)

        val today = LocalDate.now()
        val yearStart = LocalDate.of(today.year, 1, 1)
        val daysInYear = if (today.isLeapYear) 366 else 365
        val currentDayOfYear = ChronoUnit.DAYS.between(yearStart, today).toInt() + 1
        
        // CRITICAL FIX #1: Handle leap year correctly - use actual days in year
        // In leap years, we have 366 days, so we must render 366 dots
        val totalDots = daysInYear
        
        val daysLeft = daysInYear - currentDayOfYear
        val percent = ((currentDayOfYear.toFloat() / daysInYear) * 100).toInt()

        // Grid layout: dynamic columns based on total dots for better aspect ratio
        val columns = 15
        val rows = (totalDots + columns - 1) / columns 


        val layout = themeConfig.gridLayout

        // --- Grid geometry derived from user fractions ---
        val gridWidth   = width  * layout.widthFraction
        val gridHeight  = height * layout.heightFraction

        // Cell size: fit columns into gridWidth (height follows proportionally)
        val cellSize = gridWidth / columns

        // Apply density multiplier based on user preference
        // Tiny=0.70x, Small=1.00x, Medium=1.30x, Large=1.60x
        val densityMultiplier = when (themeConfig.dotDensity) {
            0 -> 0.70f  // Tiny
            1 -> 1.00f  // Small (default)
            2 -> 1.30f  // Medium
            3 -> 1.60f  // Large
            else -> 1.00f
        }

        // Dot size (radius for circle, half-width for square)
        val dotRadius   = cellSize * 0.28f * densityMultiplier
        val dotDiameter = dotRadius * 2

        // Dynamic text Y — always anchored near the bottom
        val textYPosition = height * 0.92f

        // Centre the grid, then apply the user's offset shift
        val startX = width  * (0.5f + layout.offsetX) - gridWidth  / 2f
        val startY = height * (0.5f + layout.offsetY) - gridHeight / 2f

        val paint = Paint().apply {
            isAntiAlias = true              // Smooth edges
            isDither = true                 // Better color gradients
            isFilterBitmap = true           // High-quality bitmap filtering
            style = Paint.Style.FILL
        }

        // CRITICAL FIX #2: Create glowPaint ONCE outside the loop to prevent memory leak
        // Creating BlurMaskFilter 365+ times causes significant GC pressure
        val glowPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(dotRadius * 0.4f, BlurMaskFilter.Blur.NORMAL)
        }

        // Draw dots (dynamic count based on leap year)
        for (day in 1..totalDots) {
            val row = (day - 1) / columns
            val col = (day - 1) % columns

            val centerX = startX + (col * cellSize) + (cellSize / 2f)
            val centerY = startY + (row * cellSize) + (cellSize / 2f)

            // Determine color
            val color = when {
                day < currentDayOfYear -> themeConfig.pastColor
                day == currentDayOfYear -> themeConfig.todayColor
                else -> themeConfig.futureColor
            }
            paint.color = color

            // Draw Glow (Today Only)
            if (day == currentDayOfYear) {
                // Reuse the pre-created glowPaint
                glowPaint.color = themeConfig.todayColor
                
                // Glow follows the shape
                when (themeConfig.dotShape) {
                    "square" -> {
                        val glowSize = dotDiameter * 1.2f
                        val corner = glowSize * 0.05f 
                        val left = centerX - glowSize / 2
                        val top = centerY - glowSize / 2
                        canvas.drawRoundRect(left, top, left + glowSize, top + glowSize, corner, corner, glowPaint)
                    }
                    "rounded" -> {
                        val glowSize = dotDiameter * 1.2f
                        val corner = glowSize * 0.3f
                        val left = centerX - glowSize / 2
                        val top = centerY - glowSize / 2
                        canvas.drawRoundRect(left, top, left + glowSize, top + glowSize, corner, corner, glowPaint)
                    }
                    "pill" -> {
                        val w = dotDiameter * 1.2f // Wider
                        val h = dotDiameter * 0.7f // Shorter
                        val left = centerX - w / 2
                        val top = centerY - h / 2
                        canvas.drawRoundRect(left, top, left + w, top + h, h/2, h/2, glowPaint)
                    }
                    else -> { // "circle" or default
                         canvas.drawCircle(centerX, centerY, dotRadius * 1.25f, glowPaint)
                    }
                }
            }

            // Draw Main Dot
            when (themeConfig.dotShape) {
                "square" -> {
                    // Sharp square
                    val size = dotDiameter
                    val corner = size * 0.05f // Slight rounding for polish
                    val left = centerX - size / 2
                    val top = centerY - size / 2
                    canvas.drawRoundRect(left, top, left + size, top + size, corner, corner, paint)
                }
                "rounded" -> {
                    // Soft rounded square (Apple icon style)
                    val size = dotDiameter
                    val corner = size * 0.35f 
                    val left = centerX - size / 2
                    val top = centerY - size / 2
                    canvas.drawRoundRect(left, top, left + size, top + size, corner, corner, paint)
                }
                "pill" -> {
                    // Horizontal Pill / Stadium
                    // Make it slightly wider than standard dot, but shorter
                    val w = dotDiameter * 1.1f 
                    val h = dotDiameter * 0.6f
                    val left = centerX - w / 2
                    val top = centerY - h / 2
                    val radius = h / 2f // Full rounded ends
                    canvas.drawRoundRect(left, top, left + w, top + h, radius, radius, paint)
                }
                else -> { 
                    // "circle" / "dot"
                    canvas.drawCircle(centerX, centerY, dotRadius, paint)
                }
            }
        }

        // Draw Bottom Text (Progress only, no battery) with maximum quality
        val textPaint = Paint().apply {
            isAntiAlias = true                  // Smooth edges
            isSubpixelText = true              // Sub-pixel rendering for sharper text
            isLinearText = true                // Don't scale text for better quality
            isDither = true                    // Better color rendering
            isFilterBitmap = true              // Filter when scaling
            color = 0xFFCCCCCC.toInt()        // Lighter grey for better visibility
            textSize = width * 0.025f          // Much smaller text
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }

        val text = "$daysLeft days \u2022 $percent% Complete"
        
        // Draw text at dynamically calculated position below the grid
        canvas.drawText(text, width / 2f, textYPosition, textPaint)

        return bitmap
    }
}
