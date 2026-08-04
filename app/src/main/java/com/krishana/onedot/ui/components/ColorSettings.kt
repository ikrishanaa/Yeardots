package com.krishana.onedot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ColorSettingRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun ImprovedColorPickerDialog(
    title: String,
    currentColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val presetColors = listOf(
        // Whites & Grays
        Color(0xFFFFFFFF), Color(0xFFCCCCCC), Color(0xFF999999), Color(0xFF666666),
        Color(0xFF333333), Color(0xFF000000),
        // Primary Colors
        Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF),
        // Oranges
        Color(0xFFFF6B35), Color(0xFFFF9800), Color(0xFFFFB74D),
        // Blues
        Color(0xFF2196F3), Color(0xFF1976D2), Color(0xFF0D47A1),
        // Greens
        Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF1B5E20),
        // Purples
        Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFF4A148C),
        // Others
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFFFFEB3B),
        Color(0xFF00BCD4), Color(0xFF795548), Color(0xFF607D8B)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Current color preview circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .then(
                        if (currentColor.luminance() > 0.5f)
                            Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        else Modifier
                    )
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Choose a color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                presetColors.chunked(6).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        rowColors.forEach { color ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.85f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                ),
                                label = "colorDotScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (color.luminance() > 0.5f)
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onColorSelected(color) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
