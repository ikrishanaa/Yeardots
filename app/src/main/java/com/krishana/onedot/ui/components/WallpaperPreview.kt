package com.krishana.onedot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun WallpaperPreview(
    pastColor: Color,
    todayColor: Color,
    futureColor: Color,
    backgroundColor: Color,
    dotShape: String = "dot",
    dotDensity: Int = 2 // 0=Tiny, 1=Small, 2=Medium, 3=Large
) {
    // Calculate current day of year dynamically for accurate preview
    val currentDay = java.time.LocalDate.now().dayOfYear
    
    // Use same grid layout as actual wallpaper (15 columns)
    // Show partial grid that represents the full layout better
    val columns = 15
    val rows = 15 // Keep preview manageable but representative
    val totalDots = columns * rows
    
    // Calculate dot size based on density - matching generator multipliers
    // Generator uses cellSize * 0.28f * densityMultiplier where multiplier is 0.70, 1.00, 1.30, 1.60
    // For preview in dp, we scale proportionally
    val dotSize = when (dotDensity) {
        0 -> 8.dp   // Tiny (0.70x)
        1 -> 11.dp   // Small (1.00x - default)
        2 -> 14.dp  // Medium (1.30x)
        3 -> 18.dp  // Large (1.60x)
        else -> 11.dp
    }
    
    // Get shape object based on dotShape name
    val getShape: (String) -> Shape = { shapeName ->
        when (shapeName) {
            "dot", "circle" -> CircleShape
            "rounded" -> RoundedCornerShape(30) // 30% rounding
            "square" -> RoundedCornerShape(15)  // 15% rounding
            "pill" -> RoundedCornerShape(50)    // Fully rounded for pill
            else -> CircleShape
        }
    }
    
    val shape = getShape(dotShape)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            repeat(15) { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(15) { col ->
                        val dotNumber = row * 15 + col + 1
                        val dotColor = when {
                            dotNumber < currentDay -> pastColor
                            dotNumber == currentDay -> todayColor
                            else -> futureColor
                        }
                        
                        // Modifier for size and shape
                        val baseModifier = if (dotShape == "pill") {
                            Modifier
                                .width(dotSize * 1.8f) // Wider for pill
                                .height(dotSize * 0.9f) // Slightly shorter
                        } else {
                            Modifier.size(dotSize)
                        }
                        
                        Box(
                            modifier = baseModifier.background(dotColor, shape)
                        )
                    }
                }
            }
        }
    }
}
