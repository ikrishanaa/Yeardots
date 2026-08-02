package com.krishana.onedot.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    android.util.Log.d("YearDots", "AboutDialog composable is being rendered")
    
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var autoUpdate by remember { mutableStateOf(false) }
    
    // Get app version
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    android.util.Log.d("YearDots", "AboutDialog version: $versionName")
    
    // Full-screen About page wrapped in Dialog for proper display
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
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
                
                // README
                AboutMenuItem(
                    icon = Icons.Default.Menu,
                    title = "README",
                    subtitle = "Check the GitHub repository and the README",
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/ikrishanaa/Yeardots")
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Latest Release
                AboutMenuItem(
                    icon = Icons.Default.Refresh,
                    title = "Latest release",
                    subtitle = "Look for changelogs and new versions",
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/ikrishanaa/Yeardots/releases")
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // GitHub Issues
                AboutMenuItem(
                    icon = Icons.Default.Warning,
                    title = "GitHub issue",
                    subtitle = "Submit an issue for bug report or feature request",
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/ikrishanaa/Yeardots/issues")
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Telegram channel
                AboutMenuItem(
                    icon = Icons.Default.Send,
                    title = "Telegram Channel",
                    subtitle = "https://t.me/yeardots",
                    onClick = {
                        try {
                            uriHandler.openUri("https://t.me/yeardots")
                        } catch (e: Exception) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Sponsor
                AboutMenuItem(
                    icon = Icons.Default.Favorite,
                    title = "Sponsor",
                    subtitle = "Support this app by sponsoring on GitHub",
                    onClick = {
                        try {
                            uriHandler.openUri("https://github.com/sponsors/ikrishanaa")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Not configured yet", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // Credits
                AboutMenuItem(
                    icon = Icons.Default.Star,
                    title = "Credits",
                    subtitle = "Credits and libre software",
                    onClick = {
                        Toast.makeText(
                            context, 
                            "Built with Jetpack Compose & Material 3", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                
                // Auto update with toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoUpdate = !autoUpdate }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Auto update",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "Automatically check for the latest version on GitHub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = autoUpdate,
                        onCheckedChange = { autoUpdate = it }
                    )
                }
                
                // Version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    }
}

@Composable
fun AboutMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
