package com.krishana.onedot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// ─── Clock preset sizes ─────────────────────────────────────────────────────
private enum class ClockPreset(val label: String, val heightFrac: Float) {
    NONE  ("None",   0.00f),
    SMALL ("Small",  0.14f),
    MEDIUM("Medium", 0.22f),
    LARGE ("Large",  0.30f),
    XL    ("XL",     0.40f),
}

// ─── Constants ───────────────────────────────────────────────────────────────
private const val COLUMNS       = 15
private const val TOTAL_DOTS    = 365
private const val SNAP_THRESHOLD = 0.04f   // normalized — snaps when centre is within 4%
private const val MIN_WIDTH_F   = 0.30f
private const val MAX_WIDTH_F   = 1.00f
private const val MIN_HEIGHT_F  = 0.18f
private const val MAX_HEIGHT_F  = 0.95f
private const val HANDLE_PX     = 44f      // logical touch target for handles (dp-ish, scaled later)

// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LayoutEditorScreen(
    initialWidthFraction  : Float = 0.84f,
    initialHeightFraction : Float = 0.55f,
    initialOffsetX        : Float = 0f,
    initialOffsetY        : Float = 0f,
    pastColor             : Color,
    todayColor            : Color,
    futureColor           : Color,
    backgroundColor       : Color,
    dotShape              : String,
    dotDensity            : Int,
    onDismiss             : () -> Unit,
    onSave                : (widthFraction: Float, heightFraction: Float, offsetX: Float, offsetY: Float) -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    val density = LocalDensity.current

    // ── Canvas size in pixels ──────────────────────────────────────────────
    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }

    // ── Grid bounds stored as left/top/right/bottom fractions (0..1) ──────
    // Derived: widthFraction = right-left, heightFraction = bottom-top
    //          offsetX = (left+right)/2 - 0.5, offsetY = (top+bottom)/2 - 0.5
    var gridL by remember { mutableStateOf(0.5f - initialWidthFraction  / 2f + initialOffsetX) }
    var gridT by remember { mutableStateOf(0.5f - initialHeightFraction / 2f + initialOffsetY) }
    var gridR by remember { mutableStateOf(0.5f + initialWidthFraction  / 2f + initialOffsetX) }
    var gridB by remember { mutableStateOf(0.5f + initialHeightFraction / 2f + initialOffsetY) }

    // ── Interaction state ──────────────────────────────────────────────────
    var isDragging   by remember { mutableStateOf(false) }
    var isSnapped    by remember { mutableStateOf(false) }
    var clockPreset  by remember { mutableStateOf(ClockPreset.MEDIUM) }

    // ── Ghost alpha animates when dragging ────────────────────────────────
    val gridAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.45f else 1.0f,
        animationSpec = spring(),
        label = "ghost_alpha"
    )

    // ── Snap indicator pulse ──────────────────────────────────────────────
    val snapBorderAlpha by animateFloatAsState(
        targetValue = if (isSnapped) 1.0f else 0.0f,
        label = "snap_border"
    )

    // ── Helpers ───────────────────────────────────────────────────────────
    fun clampBounds() {
        val w = (gridR - gridL).coerceIn(MIN_WIDTH_F, MAX_WIDTH_F)
        val h = (gridB - gridT).coerceIn(MIN_HEIGHT_F, MAX_HEIGHT_F)
        val cx = ((gridL + gridR) / 2f).coerceIn(w / 2f, 1f - w / 2f)
        val cy = ((gridT + gridB) / 2f).coerceIn(h / 2f, 1f - h / 2f)
        gridL = cx - w / 2f
        gridR = cx + w / 2f
        gridT = cy - h / 2f
        gridB = cy + h / 2f
    }

    fun trySnapToCenter() {
        val cx = (gridL + gridR) / 2f
        val cy = (gridT + gridB) / 2f
        val needsSnap = abs(cx - 0.5f) < SNAP_THRESHOLD && abs(cy - 0.5f) < SNAP_THRESHOLD
        if (needsSnap && !isSnapped) {
            val hw = (gridR - gridL) / 2f
            val hh = (gridB - gridT) / 2f
            gridL = 0.5f - hw
            gridR = 0.5f + hw
            gridT = 0.5f - hh
            gridB = 0.5f + hh
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSnapped = true
        } else if (!needsSnap) {
            isSnapped = false
        }
    }

    fun checkBoundaryHaptic(before: FloatArray) {
        // Fire a short tick if a boundary was hit (value didn't change as expected)
        val changed = before[0] != gridL || before[1] != gridT || before[2] != gridR || before[3] != gridB
        if (!changed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Pixel-space getters
    fun pxL() = gridL * screenW
    fun pxT() = gridT * screenH
    fun pxR() = gridR * screenW
    fun pxB() = gridB * screenH
    fun pxW() = (gridR - gridL) * screenW
    fun pxH() = (gridB - gridT) * screenH

    // ── Root container ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onGloballyPositioned { coords ->
                screenW = coords.size.width.toFloat()
                screenH = coords.size.height.toFloat()
            }
    ) {
        // ── Background gradient ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // ── Clock mock overlay ───────────────────────────────────────────
        if (clockPreset != ClockPreset.NONE && screenH > 0f) {
            val clockH = with(density) { (screenH * clockPreset.heightFrac).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(clockH)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "12:00",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = when (clockPreset) {
                        ClockPreset.SMALL  -> 20.sp
                        ClockPreset.MEDIUM -> 32.sp
                        ClockPreset.LARGE  -> 44.sp
                        ClockPreset.XL     -> 58.sp
                        else               -> 32.sp
                    },
                    fontWeight = FontWeight.Light
                )
            }
        }

        // ── Resizable dot grid ───────────────────────────────────────────
        if (screenW > 0f && screenH > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pxL().roundToInt(), pxT().roundToInt()) }
                    .size(
                        width  = with(density) { pxW().toDp() },
                        height = with(density) { pxH().toDp() }
                    )
                    .alpha(gridAlpha)
            ) {
                // ── Dot canvas ───────────────────────────────────────────
                val currentDay = remember { LocalDate.now().dayOfYear }
                val densityMul = when (dotDensity) { 0 -> 0.70f; 2 -> 1.30f; 3 -> 1.60f; else -> 1.00f }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellSize = size.width / COLUMNS
                    val dotRadius = cellSize * 0.28f * densityMul

                    for (i in 0 until TOTAL_DOTS) {
                        val col = i % COLUMNS
                        val row = i / COLUMNS
                        val cx  = col * cellSize + cellSize / 2f
                        val cy  = row * cellSize + cellSize / 2f
                        val color = when {
                            i + 1 < currentDay  -> pastColor
                            i + 1 == currentDay -> todayColor
                            else                -> futureColor
                        }
                        when (dotShape) {
                            "square" -> drawRect(
                                color    = color,
                                topLeft  = Offset(cx - dotRadius, cy - dotRadius),
                                size     = Size(dotRadius * 2, dotRadius * 2)
                            )
                            "rounded" -> drawRoundRect(
                                color       = color,
                                topLeft     = Offset(cx - dotRadius, cy - dotRadius),
                                size        = Size(dotRadius * 2, dotRadius * 2),
                                cornerRadius = CornerRadius(dotRadius * 0.35f)
                            )
                            "pill" -> drawRoundRect(
                                color       = color,
                                topLeft     = Offset(cx - dotRadius, cy - dotRadius * 0.55f),
                                size        = Size(dotRadius * 2, dotRadius * 1.1f),
                                cornerRadius = CornerRadius(dotRadius * 0.55f)
                            )
                            else -> drawCircle(color = color, radius = dotRadius, center = Offset(cx, cy))
                        }
                    }
                }

                // ── Dashed border ─────────────────────────────────────
                val snapColor = Color(0xFFF97316)   // accent orange
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(
                        width  = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                    drawRect(
                        color  = if (isSnapped) snapColor else Color.White.copy(alpha = 0.55f),
                        style  = stroke
                    )
                    // Corner dots
                    val r = 4.dp.toPx()
                    val dotColor = if (isSnapped) snapColor else Color.White.copy(alpha = 0.8f)
                    listOf(
                        Offset(0f, 0f), Offset(size.width, 0f),
                        Offset(0f, size.height), Offset(size.width, size.height)
                    ).forEach { drawCircle(dotColor, r, it) }
                }

                // ── Centre-pan touch region (full grid body) ──────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(screenW, screenH) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd   = { isDragging = false; trySnapToCenter() },
                                onDrag      = { change, drag ->
                                    change.consume()
                                    val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                    val dx = drag.x / screenW
                                    val dy = drag.y / screenH
                                    gridL += dx; gridR += dx
                                    gridT += dy; gridB += dy
                                    // boundary clamp: don't go off screen
                                    if (gridL < 0f) { gridR -= gridL; gridL = 0f }
                                    if (gridR > 1f) { gridL -= (gridR - 1f); gridR = 1f }
                                    if (gridT < 0f) { gridB -= gridT; gridT = 0f }
                                    if (gridB > 1f) { gridT -= (gridB - 1f); gridB = 1f }
                                    checkBoundaryHaptic(before)
                                }
                            )
                        }
                )

                // ── 8 Resize handles ──────────────────────────────────
                val hTouchSize = with(density) { 44.dp }

                // TOP edge
                Box(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 80.dp, height = hTouchSize)
                    .offset(y = (-hTouchSize / 2))
                    .pointerInput(screenW, screenH) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd   = { isDragging = false },
                            onDrag      = { change, drag ->
                                change.consume()
                                val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                val dy = drag.y / screenH
                                gridT = (gridT + dy).coerceAtMost(gridB - MIN_HEIGHT_F).coerceAtLeast(0f)
                                checkBoundaryHaptic(before)
                            }
                        )
                    }
                ) {
                    // Visual indicator
                    ResizeHandleBar(isHorizontal = true, modifier = Modifier.align(Alignment.Center))
                }

                // BOTTOM edge
                Box(modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 80.dp, height = hTouchSize)
                    .offset(y = (hTouchSize / 2))
                    .pointerInput(screenW, screenH) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd   = { isDragging = false },
                            onDrag      = { change, drag ->
                                change.consume()
                                val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                val dy = drag.y / screenH
                                gridB = (gridB + dy).coerceAtLeast(gridT + MIN_HEIGHT_F).coerceAtMost(1f)
                                checkBoundaryHaptic(before)
                            }
                        )
                    }
                ) {
                    ResizeHandleBar(isHorizontal = true, modifier = Modifier.align(Alignment.Center))
                }

                // LEFT edge
                Box(modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(width = hTouchSize, height = 80.dp)
                    .offset(x = (-hTouchSize / 2))
                    .pointerInput(screenW, screenH) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd   = { isDragging = false },
                            onDrag      = { change, drag ->
                                change.consume()
                                val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                val dx = drag.x / screenW
                                gridL = (gridL + dx).coerceAtMost(gridR - MIN_WIDTH_F).coerceAtLeast(0f)
                                checkBoundaryHaptic(before)
                            }
                        )
                    }
                ) {
                    ResizeHandleBar(isHorizontal = false, modifier = Modifier.align(Alignment.Center))
                }

                // RIGHT edge
                Box(modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = hTouchSize, height = 80.dp)
                    .offset(x = (hTouchSize / 2))
                    .pointerInput(screenW, screenH) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd   = { isDragging = false },
                            onDrag      = { change, drag ->
                                change.consume()
                                val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                val dx = drag.x / screenW
                                gridR = (gridR + dx).coerceAtLeast(gridL + MIN_WIDTH_F).coerceAtMost(1f)
                                checkBoundaryHaptic(before)
                            }
                        )
                    }
                ) {
                    ResizeHandleBar(isHorizontal = false, modifier = Modifier.align(Alignment.Center))
                }

                // ── Corner handles ────────────────────────────────────
                val corners = listOf(
                    Alignment.TopStart to Triple(true, true, false),
                    Alignment.TopEnd   to Triple(true, false, true),
                    Alignment.BottomStart to Triple(false, true, false),
                    Alignment.BottomEnd   to Triple(false, false, true),
                )
                corners.forEach { (alignment, flags) ->
                    val (isTop, isLeft, _) = flags
                    Box(modifier = Modifier
                        .align(alignment)
                        .size(hTouchSize)
                        .pointerInput(screenW, screenH) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd   = { isDragging = false },
                                onDrag      = { change, drag ->
                                    change.consume()
                                    val before = floatArrayOf(gridL, gridT, gridR, gridB)
                                    val dx = drag.x / screenW
                                    val dy = drag.y / screenH
                                    if (isLeft)  gridL = (gridL + dx).coerceAtMost(gridR - MIN_WIDTH_F).coerceAtLeast(0f)
                                    else         gridR = (gridR + dx).coerceAtLeast(gridL + MIN_WIDTH_F).coerceAtMost(1f)
                                    if (isTop)   gridT = (gridT + dy).coerceAtMost(gridB - MIN_HEIGHT_F).coerceAtLeast(0f)
                                    else         gridB = (gridB + dy).coerceAtLeast(gridT + MIN_HEIGHT_F).coerceAtMost(1f)
                                    checkBoundaryHaptic(before)
                                }
                            )
                        }
                    ) {
                        CornerHandleMark(
                            isTop  = isTop,
                            isLeft = isLeft,
                            modifier = Modifier.align(alignment)
                        )
                    }
                }
            }
        }

        // ── Snap indicator label ─────────────────────────────────────────
        if (isSnapped) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(snapBorderAlpha)
                    .background(
                        Color(0xFFF97316).copy(alpha = 0.18f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("⊕ Centred", color = Color(0xFFF97316), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Bottom control panel ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clock preset chips
            Text(
                text = "Clock Size Preview",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ClockPreset.entries.forEach { preset ->
                    val selected = clockPreset == preset
                    FilterChip(
                        selected = selected,
                        onClick  = { clockPreset = preset },
                        label    = { Text(preset.label, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = Color(0xFFF97316),
                            selectedLabelColor        = Color.White,
                            containerColor            = Color.White.copy(alpha = 0.10f),
                            labelColor                = Color.White.copy(alpha = 0.70f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Grid size info
            if (screenW > 0f && screenH > 0f) {
                val wPct = ((gridR - gridL) * 100).roundToInt()
                val hPct = ((gridB - gridT) * 100).roundToInt()
                Text(
                    text = "Grid  ${wPct}% wide · ${hPct}% tall",
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        gridL = 0.5f - 0.42f; gridR = 0.5f + 0.42f
                        gridT = 0.5f - 0.275f; gridB = 0.5f + 0.275f
                        isSnapped = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                }

                // Cancel
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }

                // Save / Done
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clampBounds()
                        val wf = gridR - gridL
                        val hf = gridB - gridT
                        val ox = (gridL + gridR) / 2f - 0.5f
                        val oy = (gridT + gridB) / 2f - 0.5f
                        onSave(wf, hf, ox, oy)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Grid Layout",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Drag edges · pinch corners · pan inside",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }
    }
}

// ─── Small composables ────────────────────────────────────────────────────────

/** A pill-shaped drag indicator bar (horizontal or vertical) */
@Composable
private fun ResizeHandleBar(isHorizontal: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .then(
                if (isHorizontal)
                    Modifier.size(width = 40.dp, height = 5.dp)
                else
                    Modifier.size(width = 5.dp, height = 40.dp)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.80f))
    )
}

/** An L-shaped corner mark drawn with Canvas */
@Composable
private fun CornerHandleMark(isTop: Boolean, isLeft: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = Stroke(width = 2.5.dp.toPx())
        val color  = Color.White
        val arm    = size.width * 0.75f
        val x0     = if (isLeft) 0f else size.width
        val y0     = if (isTop)  0f else size.height
        val xDir   = if (isLeft) 1f else -1f
        val yDir   = if (isTop)  1f else -1f
        val path   = androidx.compose.ui.graphics.Path().apply {
            moveTo(x0 + xDir * arm, y0)
            lineTo(x0, y0)
            lineTo(x0, y0 + yDir * arm)
        }
        drawPath(path, color, style = stroke)
    }
}
