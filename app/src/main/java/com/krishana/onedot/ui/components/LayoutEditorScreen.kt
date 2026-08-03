package com.krishana.onedot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LayoutEditorScreen(
    initialScale: Float,
    initialOffsetX: Float,
    initialOffsetY: Float,
    pastColor: Color,
    todayColor: Color,
    futureColor: Color,
    backgroundColor: Color,
    dotShape: String,
    dotDensity: Int,
    onDismiss: () -> Unit,
    onSave: (scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var scale by remember { mutableStateOf(initialScale) }
    var offsetX by remember { mutableStateOf(initialOffsetX) }
    var offsetY by remember { mutableStateOf(initialOffsetY) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Lock screen usually has black bg or the wallpaper's bg
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 2.5f)
                        
                        // pan is in pixels. We need to normalize it by screen dimensions
                        // since our offsets are normalized [0, 1]
                        offsetX += pan.x / size.width
                        offsetY += pan.y / size.height
                    }
                }
        ) {
            val density = LocalDensity.current
            val layoutWidthPx = with(density) { maxWidth.toPx() }
            val layoutHeightPx = with(density) { maxHeight.toPx() }
            
            // Draw a mock clock at the top (typical lock screen clock position)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = maxHeight * 0.12f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "10:00",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 80.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Mon, Jan 1",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Draw the grid with scale and offset
            // We use graphicsLayer to apply transform
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX * layoutWidthPx,
                        translationY = offsetY * layoutHeightPx
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Here we render the preview.
                // The WallpaperPreview aspect ratio is 15:25
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f) // Matches sidePadding roughly
                ) {
                    WallpaperPreview(
                        pastColor = pastColor,
                        todayColor = todayColor,
                        futureColor = futureColor,
                        backgroundColor = Color.Transparent,
                        dotShape = dotShape,
                        dotDensity = dotDensity
                    )
                }
            }
        }
        
        // Overlay Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    scale = 1.0f
                    offsetX = 0f
                    offsetY = 0f
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("Reset")
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = { onSave(scale, offsetX, offsetY) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        }
        
        // Instructional overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Pinch to scale • Drag to position",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
