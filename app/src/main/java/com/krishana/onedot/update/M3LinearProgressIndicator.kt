package com.krishana.onedot.update

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun M3LinearProgressIndicator(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    trackThickness: Dp = 8.dp,    // Matches the thicker pill shape you have
    gapSize: Dp = 4.dp,           // M3 standard transparent gap
    drawStopIndicator: Boolean = true,
    waveAmplitude: Dp = 2.dp,     // The "little wave"
    waveWavelength: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    // 1. Smooth glide to the exact percentage
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "M3Progress"
    )

    // 2. Infinite wave animation (the continuous "slithering" effect)
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val activePath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // Height accounts for the track thickness plus the wave peaks on top and bottom
            .height(trackThickness + waveAmplitude * 2) 
    ) {
        val w = size.width
        val h = trackThickness.toPx()
        val centerY = size.height / 2f
        val g = gapSize.toPx()
        
        val activeWidth = w * animatedProgress
        val hasActive = activeWidth > 0f

        // Stop indicator is a circle of size h x h at the very end
        val s = if (drawStopIndicator) h else 0f 

        // Determine where the unfilled track should start and end
        val trackStartX = if (hasActive) activeWidth + g else 0f
        val trackEndX = if (drawStopIndicator) w - s - g else w
        val trackWidth = trackEndX - trackStartX

        // Y offset for straight rounded rects so they perfectly vertically align with the wave
        val topY = centerY - h / 2f

        // --- DRAW UNFILLED TRACK ---
        if (trackWidth > 0f) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(trackStartX, topY),
                size = Size(trackWidth, h),
                cornerRadius = CornerRadius(h / 2f)
            )
        }

        // --- DRAW STOP INDICATOR ---
        // Only draw if the active progress hasn't completely swallowed it yet
        if (drawStopIndicator && activeWidth < w - s / 2f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(w - s, topY),
                size = Size(s, h),
                cornerRadius = CornerRadius(h / 2f)
            )
        }

        // --- DRAW ACTIVE PROGRESS (THE LITTLE WAVE) ---
        if (hasActive) {
            val amplitudePx = waveAmplitude.toPx()
            val wavelengthPx = waveWavelength.toPx()
            val startX = h / 2f
            val endX = maxOf(startX, activeWidth - h / 2f)
            val strokeLength = endX - startX

            if (strokeLength > 0f) {
                activePath.reset()
                activePath.moveTo(startX, centerY)
                
                val step = 4f 
                var x = startX
                while (x <= endX) {
                    val distanceToHead = endX - x
                    val currentAmplitude = if (distanceToHead < wavelengthPx) {
                        amplitudePx * (distanceToHead / wavelengthPx) // Taper off to flat at the head
                    } else {
                        amplitudePx
                    }
                    
                    val relativeX = x - startX
                    val y = centerY + currentAmplitude * sin((relativeX / wavelengthPx) * 2 * PI - phaseShift).toFloat()
                    activePath.lineTo(x, y)
                    x += step
                }
                activePath.lineTo(endX, centerY)

                drawPath(
                    path = activePath,
                    color = color,
                    style = Stroke(
                        width = h,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                // If it's too short to stroke a wave, draw a tiny perfect pill
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, topY),
                    size = Size(activeWidth, h),
                    cornerRadius = CornerRadius(h / 2f)
                )
            }
        }
    }
}
