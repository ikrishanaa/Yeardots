package com.krishana.onedot.ui.components

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.vectorResource
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.star

class PolygonShape : Shape {
    private val matrix = Matrix()
    private val composePath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = 0.8f,
            rounding = CornerRounding(radius = 0.5f)
        )
        val normalized = polygon.normalized()
        val androidPath = normalized.toPath()
        
        composePath.rewind()
        composePath.addPath(androidPath.asComposePath())
        
        matrix.reset()
        // Cookie9Sided has a -90 degree (or 270 degree) rotation applied to point upwards
        matrix.rotateZ(-90f)
        matrix.scale(size.width, size.height)
        
        composePath.transform(matrix)
        
        // Since we rotated the path around 0,0, we might need to translate it back into bounds.
        // Wait, normalized() bounds are between 0..1 centered. If we scale first, then rotate around center?
        // Actually, the matrix operations are applied in order.
        // Let's do it cleanly:
        // We know `normalized()` outputs vertices inside 0..1. The center is at 0.5, 0.5.
        // If we rotate around Z without translating, it rotates around 0,0 (top left).
        // Let's rotate around center.
        matrix.reset()
        matrix.translate(size.width / 2f, size.height / 2f)
        matrix.rotateZ(-90f)
        matrix.translate(-size.width / 2f, -size.height / 2f)
        matrix.scale(size.width, size.height)
        
        composePath.rewind()
        composePath.addPath(androidPath.asComposePath())
        composePath.transform(matrix)
        
        return Outline.Generic(composePath)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar with back button
                TopAppBar(
                    title = {
                        Text(
                            "About",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "App Icon",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Yearsdot",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = versionName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "UNIVERSAL",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Lead Developer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = "https://github.com/ikrishanaa.png",
                                    contentDescription = "Developer",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(PolygonShape())
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Krishana Yadav",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Lead developer",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Social Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SocialFilledTonalButton(icon = Icons.Default.Language) {
                                    try {
                                        uriHandler.openUri("https://yearsdot.qzz.io")
                                    } catch (e: Exception) {}
                                }
                                SocialFilledTonalButton(icon = ImageVector.vectorResource(id = com.krishana.onedot.R.drawable.ic_github)) {
                                    try {
                                        uriHandler.openUri("https://github.com/ikrishanaa")
                                    } catch (e: Exception) {}
                                }
                                SocialFilledTonalButton(icon = Icons.Default.Person) {
                                    try {
                                        uriHandler.openUri("https://ikrishnaa.dev/")
                                    } catch (e: Exception) {}
                                }
                                SocialFilledTonalButton(icon = ImageVector.vectorResource(id = com.krishana.onedot.R.drawable.ic_instagram)) {
                                    try {
                                        uriHandler.openUri("https://www.instagram.com/ydv_krsn")
                                    } catch (e: Exception) {}
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Buy me a coffee
                            FilledTonalButton(
                                onClick = { 
                                    try {
                                        uriHandler.openUri("https://github.com/sponsors/ikrishanaa")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Not configured yet", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sponsor on GitHub", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Community & Info Section
                    Text(
                        text = "Community & Info",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    
                    CollaboratorCard(
                        icon = Icons.Default.Send,
                        title = "Telegram channel",
                        onClick = {
                            try {
                                uriHandler.openUri("https://t.me/yeardots")
                            } catch (e: Exception) {
                                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CollaboratorCard(
                        icon = Icons.Default.Info,
                        title = "GitHub issues",
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/ikrishanaa/Yeardots/issues")
                            } catch (e: Exception) {
                                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SocialFilledTonalButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CollaboratorCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "menuItemScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
