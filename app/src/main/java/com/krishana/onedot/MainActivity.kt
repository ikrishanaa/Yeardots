package com.krishana.onedot

import android.app.WallpaperManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishana.onedot.core.WallpaperGenerator
import com.krishana.onedot.data.SettingsRepository
import com.krishana.onedot.ui.components.AboutDialog
import com.krishana.onedot.ui.components.ColorSettingRow
import com.krishana.onedot.ui.components.DebugInfoRow
import com.krishana.onedot.ui.components.ImprovedColorPickerDialog
import com.krishana.onedot.ui.components.ShapeOptionItem
import com.krishana.onedot.ui.components.WallpaperPreview
import com.krishana.onedot.ui.theme.OneDotTheme
import com.krishana.onedot.util.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Apply wallpaper immediately and synchronously
 */
suspend fun applyWallpaperNow(context: Context, repository: SettingsRepository) {
    withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("YearDots", "=== WALLPAPER UPDATE START ===")
            
            // CRITICAL: Check permissions before attempting to set wallpaper
            android.util.Log.d("YearDots", "Checking SET_WALLPAPER permission...")
            val hasWallpaperPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SET_WALLPAPER
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            android.util.Log.d("YearDots", "SET_WALLPAPER permission: $hasWallpaperPerm")
            
            if (!hasWallpaperPerm) {
                android.util.Log.e("YearDots", "ERROR: SET_WALLPAPER permission not granted!")
                throw SecurityException("SET_WALLPAPER permission not granted. Please enable it in app permissions.")
            }
            
            // CRITICAL FIX #4: Removed unnecessary storage permission checks
            // WallpaperManager.setBitmap() only requires SET_WALLPAPER permission
            // Storage permissions are NOT needed for setting wallpaper programmatically
            android.util.Log.d("YearDots", "Storage permissions not required for WallpaperManager.setBitmap()")
            
            android.util.Log.d("YearDots", "All permissions OK, getting WallpaperManager...")
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Get ACTUAL screen dimensions for pixel-perfect quality
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            // Use 1.5x super-sampling for even sharper quality
            val width = (screenWidth * 1.5f).toInt()
            val height = (screenHeight * 1.5f).toInt()
            
            android.util.Log.d("YearDots", "Screen dimensions: ${screenWidth}x${screenHeight}")
            android.util.Log.d("YearDots", "Wallpaper dimensions (1.5x super-sampled): ${width}x${height}")
            
            // Suggest dimensions to prevent downscaling (optional - requires SET_WALLPAPER_HINTS permission)
            try {
                wallpaperManager.suggestDesiredDimensions(width, height)
                android.util.Log.d("YearDots", "Successfully suggested dimensions: ${width}x${height}")
            } catch (e: SecurityException) {
                android.util.Log.w("YearDots", "Cannot suggest dimensions (missing SET_WALLPAPER_HINTS permission), continuing anyway...")
            }
            
            // Get current settings
            android.util.Log.d("YearDots", "Loading color settings...")
            val pastColor = repository.getPastColor()
            val todayColor = repository.getTodayColor()
            val futureColor = repository.getFutureColor()
            val backgroundColor = repository.getBackgroundColor()
            val dotShape = repository.getDotShape()
            val dotDensity = repository.getDotDensity()
            
            val gridWidthFraction  = repository.getGridWidthFraction()
            val gridHeightFraction = repository.getGridHeightFraction()
            val gridOffsetX = repository.getGridOffsetX()
            val gridOffsetY = repository.getGridOffsetY()
            
            android.util.Log.d("YearDots", "Colors - Past: 0x${pastColor.toString(16)}, Today: 0x${todayColor.toString(16)}, Future: 0x${futureColor.toString(16)}, BG: 0x${backgroundColor.toString(16)}, Shape: $dotShape, Density: $dotDensity")
            
            val themeConfig = WallpaperGenerator.ThemeConfig(
                pastColor = pastColor,
                todayColor = todayColor,
                futureColor = futureColor,
                backgroundColor = backgroundColor,
                dotShape = dotShape,
                dotDensity = dotDensity,
                gridLayout = WallpaperGenerator.GridLayout(gridWidthFraction, gridHeightFraction, gridOffsetX, gridOffsetY)
            )
            
            // Generate bitmap
            android.util.Log.d("YearDots", "Generating wallpaper bitmap...")
            val bitmap = WallpaperGenerator.generateBitmap(width, height, themeConfig)
            android.util.Log.d("YearDots", "Bitmap generated: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")
            
            try {
                // Use setBitmap with high-quality settings
                // allowWhileIdle=false ensures full quality processing
                android.util.Log.d("YearDots", "Setting wallpaper on LOCK SCREEN with high quality...")
                wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
                android.util.Log.d("YearDots", "✓ Wallpaper set successfully at ${width}x${height}!")
            } catch (e: SecurityException) {
                android.util.Log.e("YearDots", "SecurityException: ${e.message}", e)
                android.util.Log.e("YearDots", "Stack trace: ${e.stackTraceToString()}")
                throw SecurityException("Permission denied when setting wallpaper. Please ensure all permissions are granted in Settings > Apps > Year Dots > Permissions.", e)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("YearDots", "IllegalArgumentException: ${e.message}", e)
                throw Exception("Invalid wallpaper size or format. Please try again.", e)
            } catch (e: Exception) {
                android.util.Log.e("YearDots", "Unexpected error: ${e.message}", e)
                android.util.Log.e("YearDots", "Error type: ${e.javaClass.name}")
                android.util.Log.e("YearDots", "Stack trace: ${e.stackTraceToString()}")
                throw Exception("Failed to set wallpaper: ${e.message}", e)
            } finally {
                bitmap.recycle()
                android.util.Log.d("YearDots", "Bitmap recycled")
            }
        } catch (e: SecurityException) {
            // Re-throw security exceptions with clear message
            android.util.Log.e("YearDots", "Final SecurityException handler: ${e.message}", e)
            throw Exception("Permission Error: ${e.message}", e)
        } catch (e: Exception) {
            // Re-throw with context
            android.util.Log.e("YearDots", "Final error handler: ${e.message}", e)
            throw Exception("Wallpaper update failed: ${e.message}", e)
        } finally {
            android.util.Log.d("YearDots", "=== WALLPAPER UPDATE END ===")
        }
    }
}





class MainActivity : ComponentActivity() {
    
    // Permission state
    private var hasPermissions by mutableStateOf(false)
    
    // Permission launcher - must be registered before onCreate
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        hasPermissions = allGranted
        
        if (!allGranted) {
            Toast.makeText(
                this,
                "⚠️ Permissions are needed for the app to function properly",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initial check
        checkPermissions()
        
        if (!hasPermissions) {
            // Request on startup if not granted
            requestPermissions()
        }
        
        // Schedule daily update on first launch
        WorkScheduler.scheduleDailyWallpaperUpdate(this)
        
        setContent {
            OneDotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Permission Warning Banner
                        if (!hasPermissions) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "⚠️ Permissions Needed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Please grant all permissions to allow the wallpaper to update automatically.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { requestPermissions() }) {
                                        Text("Grant Permissions")
                                    }
                                }
                            }
                        }
                        
                        // Main Settings UI
                        SettingsScreen()
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-check permissions when user returns to app
        // (e.g., after granting permissions in Settings)
        checkPermissions()
    }
    
    private fun checkPermissions() {
        val permissions = getRequiredPermissions()
        hasPermissions = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        val permissions = getRequiredPermissions()
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // CRITICAL FIX #4: Only SET_WALLPAPER permission is actually required
        // Storage permissions are NOT needed for WallpaperManager.setBitmap()
        // The app will work perfectly without them, avoiding unnecessary permission requests
        // that confuse users and cause failures when denied
        
        // Core functionality - ONLY permission actually required
        permissions.add(android.Manifest.permission.SET_WALLPAPER)

        // Note: Removed all storage permissions as they are not needed:
        // - READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE: Not needed
        // - READ_MEDIA_IMAGES: Not needed  
        // - POST_NOTIFICATIONS: Not used in this app
        // WallpaperManager.setBitmap() works with just SET_WALLPAPER permission

        return permissions
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    // Collect saved colors from DataStore
    val savedPastColor by repository.pastColorFlow.collectAsState(initial = SettingsRepository.DEFAULT_PAST_COLOR)
    val savedTodayColor by repository.todayColorFlow.collectAsState(initial = SettingsRepository.DEFAULT_TODAY_COLOR)
    val savedFutureColor by repository.futureColorFlow.collectAsState(initial = SettingsRepository.DEFAULT_FUTURE_COLOR)
    val savedBackgroundColor by repository.backgroundColorFlow.collectAsState(initial = SettingsRepository.DEFAULT_BACKGROUND_COLOR)
    val savedDotShape by repository.dotShapeFlow.collectAsState(initial = SettingsRepository.DEFAULT_DOT_SHAPE)
    val savedDotDensity by repository.dotDensityFlow.collectAsState(initial = SettingsRepository.DEFAULT_DOT_DENSITY)
    val savedGridWidthFraction  by repository.gridWidthFractionFlow.collectAsState(initial = SettingsRepository.DEFAULT_GRID_WIDTH_FRACTION)
    val savedGridHeightFraction by repository.gridHeightFractionFlow.collectAsState(initial = SettingsRepository.DEFAULT_GRID_HEIGHT_FRACTION)
    val savedGridOffsetX by repository.gridOffsetXFlow.collectAsState(initial = SettingsRepository.DEFAULT_GRID_OFFSET_X)
    val savedGridOffsetY by repository.gridOffsetYFlow.collectAsState(initial = SettingsRepository.DEFAULT_GRID_OFFSET_Y)

    // Pending colors (not saved yet)
    var pendingPastColor by remember { mutableStateOf<Int?>(null) }
    var pendingTodayColor by remember { mutableStateOf<Int?>(null) }
    var pendingFutureColor by remember { mutableStateOf<Int?>(null) }
    var pendingBackgroundColor by remember { mutableStateOf<Int?>(null) }
    var pendingDotShape by remember { mutableStateOf<String?>(null) }
    var pendingDotDensity by remember { mutableStateOf<Int?>(null) }
    var pendingGridWidthFraction  by remember { mutableStateOf<Float?>(null) }
    var pendingGridHeightFraction by remember { mutableStateOf<Float?>(null) }
    var pendingGridOffsetX by remember { mutableStateOf<Float?>(null) }
    var pendingGridOffsetY by remember { mutableStateOf<Float?>(null) }

    // Color picker dialogs
    var showPastColorPicker by remember { mutableStateOf(false) }
    var showTodayColorPicker by remember { mutableStateOf(false) }
    var showFutureColorPicker by remember { mutableStateOf(false) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    
    // Confirmation dialog
    var showSaveDialog by remember { mutableStateOf(false) }

    // Get current colors (pending or saved)
    val currentPastColor = pendingPastColor ?: savedPastColor
    val currentTodayColor = pendingTodayColor ?: savedTodayColor
    val currentFutureColor = pendingFutureColor ?: savedFutureColor
    val currentBackgroundColor = pendingBackgroundColor ?: savedBackgroundColor
    val currentDotShape = pendingDotShape ?: savedDotShape
    val currentDotDensity = pendingDotDensity ?: savedDotDensity
    val currentGridWidthFraction  = pendingGridWidthFraction  ?: savedGridWidthFraction
    val currentGridHeightFraction = pendingGridHeightFraction ?: savedGridHeightFraction
    val currentGridOffsetX = pendingGridOffsetX ?: savedGridOffsetX
    val currentGridOffsetY = pendingGridOffsetY ?: savedGridOffsetY

    // Check if there are unsaved changes
    val hasChanges = pendingPastColor != null || 
                     pendingTodayColor != null || 
                     pendingFutureColor != null || 
                     pendingBackgroundColor != null ||
                     pendingDotShape != null ||
                     pendingDotDensity != null ||
                     pendingGridWidthFraction  != null ||
                     pendingGridHeightFraction != null ||
                     pendingGridOffsetX != null ||
                     pendingGridOffsetY != null

    var showAboutDialog by remember { mutableStateOf(false) }
    var showLayoutEditor by remember { mutableStateOf(false) }

    if (showLayoutEditor) {
        com.krishana.onedot.ui.components.LayoutEditorScreen(
            initialWidthFraction  = currentGridWidthFraction,
            initialHeightFraction = currentGridHeightFraction,
            initialOffsetX = currentGridOffsetX,
            initialOffsetY = currentGridOffsetY,
            pastColor = Color(currentPastColor),
            todayColor = Color(currentTodayColor),
            futureColor = Color(currentFutureColor),
            backgroundColor = Color(currentBackgroundColor),
            dotShape = currentDotShape,
            dotDensity = currentDotDensity,
            onDismiss = { showLayoutEditor = false },
            onSave = { wf, hf, offsetX, offsetY ->
                pendingGridWidthFraction  = wf
                pendingGridHeightFraction = hf
                pendingGridOffsetX = offsetX
                pendingGridOffsetY = offsetY
                showLayoutEditor = false
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Year Dots",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )
            IconButton(
                onClick = { 
                    android.util.Log.d("YearDots", "Info icon clicked - setting showAboutDialog to true")
                    showAboutDialog = true 
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Preview Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section Label
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                
                // Preview container with background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(currentBackgroundColor))
                        .clickable { showLayoutEditor = true },
                    contentAlignment = Alignment.Center
                ) {
                    WallpaperPreview(
                        pastColor = Color(currentPastColor),
                        todayColor = Color(currentTodayColor),
                        futureColor = Color(currentFutureColor),
                        backgroundColor = Color.Transparent,
                        dotShape = currentDotShape,
                        dotDensity = currentDotDensity
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showLayoutEditor = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()
                ) {
                    Text("Edit Lock Screen Layout")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Color Palette Card
        ElevatedCard(
           modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "COLOR PALETTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
                
                // 2x2 Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Past Days
                        ColorPaletteItem(
                            modifier = Modifier.weight(1f),
                            topLabel = "PAST",
                            bottomLabel = "Days",
                            color = Color(currentPastColor),
                            onClick = { showPastColorPicker = true }
                        )
                        
                        // Today
                        ColorPaletteItem(
                            modifier = Modifier.weight(1f),
                            topLabel = "TODAY",
                            bottomLabel = "Current",
                            color = Color(currentTodayColor),
                            onClick = { showTodayColorPicker = true },
                            hasGlow = true
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Future Days
                        ColorPaletteItem(
                            modifier = Modifier.weight(1f),
                            topLabel = "FUTURE",
                            bottomLabel = "Days",
                            color = Color(currentFutureColor),
                            onClick = { showFutureColorPicker = true }
                        )
                        
                        // Background
                        ColorPaletteItem(
                            modifier = Modifier.weight(1f),
                            topLabel = "BASE",
                            bottomLabel = "Theme",
                            color = Color(currentBackgroundColor),
                            onClick = { showBackgroundColorPicker = true },
                            hasBorder = true
                        )
                    }
                }
            }
        }

        // Shape Selection Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SHAPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
                
                // Horizontal shape selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Dot
                    ShapeSelectorItem(
                        modifier = Modifier.weight(1f),
                        selected = currentDotShape == "circle",
                        onClick = { pendingDotShape = "circle" }
                    ) { isSelected ->
                        val shapeColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            animationSpec = tween(300),
                            label = "shapeColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(shapeColor, CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    
                    // Rounded
                    ShapeSelectorItem(
                        modifier = Modifier.weight(1f),
                        selected = currentDotShape == "rounded",
                        onClick = { pendingDotShape = "rounded" }
                    ) { isSelected ->
                        val shapeColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            animationSpec = tween(300),
                            label = "shapeColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(shapeColor, RoundedCornerShape(6.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    
                    // Square
                    ShapeSelectorItem(
                        modifier = Modifier.weight(1f),
                        selected = currentDotShape == "square",
                        onClick = { pendingDotShape = "square" }
                    ) { isSelected ->
                        val shapeColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            animationSpec = tween(300),
                            label = "shapeColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(shapeColor, RoundedCornerShape(2.dp))
                        )
                    }
                    
                    Spacer(modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .align(Alignment.CenterVertically)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    
                    // Pill
                    ShapeSelectorItem(
                        modifier = Modifier.weight(1f),
                        selected = currentDotShape == "pill",
                        onClick = { pendingDotShape = "pill" }
                    ) { isSelected ->
                        val shapeColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            animationSpec = tween(300),
                            label = "shapeColor"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(12.dp)
                                    .background(shapeColor, RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        // Size Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 20.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val labels = listOf("Tiny", "Small", "Medium", "Large")
                        labels.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                                onClick = { pendingDotDensity = index },
                                selected = index == currentDotDensity,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (index == currentDotDensity) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Save & Apply Button
        if (hasChanges) {
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Save & Apply to Lockscreen",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Color Picker Dialogs
    if (showPastColorPicker) {
        ImprovedColorPickerDialog(
            title = "Past Days Color",
            currentColor = Color(currentPastColor),
            onDismiss = { showPastColorPicker = false },
            onColorSelected = { color ->
                pendingPastColor = color.toArgb()
                showPastColorPicker = false
            }
        )
    }

    if (showTodayColorPicker) {
        ImprovedColorPickerDialog(
            title = "Current Day Color",
            currentColor = Color(currentTodayColor),
            onDismiss = { showTodayColorPicker = false },
            onColorSelected = { color ->
                pendingTodayColor = color.toArgb()
                showTodayColorPicker = false
            }
        )
    }

    if (showFutureColorPicker) {
        ImprovedColorPickerDialog(
            title = "Future Days Color",
            currentColor = Color(currentFutureColor),
            onDismiss = { showFutureColorPicker = false },
            onColorSelected = { color ->
                pendingFutureColor = color.toArgb()
                showFutureColorPicker = false
            }
        )
    }

    if (showBackgroundColorPicker) {
        ImprovedColorPickerDialog(
            title = "Background Color",
            currentColor = Color(currentBackgroundColor),
            onDismiss = { showBackgroundColorPicker = false },
            onColorSelected = { color ->
                pendingBackgroundColor = color.toArgb()
                showBackgroundColorPicker = false
            }
        )
    }

    // About Dialog
    android.util.Log.d("YearDots", "showAboutDialog state: $showAboutDialog")
    if (showAboutDialog) {
        android.util.Log.d("YearDots", "Rendering AboutDialog...")
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Apply Settings?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "This will save your color changes and apply them to your lock screen wallpaper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                // Save pending colors
                                pendingPastColor?.let { repository.updatePastColor(it) }
                                pendingTodayColor?.let { repository.updateTodayColor(it) }
                                pendingFutureColor?.let { repository.updateFutureColor(it) }
                                pendingBackgroundColor?.let { repository.updateBackgroundColor(it) }
                                pendingDotShape?.let { repository.updateDotShape(it) }
                                pendingDotDensity?.let { repository.updateDotDensity(it) }
                                
                                if (pendingGridWidthFraction != null || pendingGridHeightFraction != null ||
                                    pendingGridOffsetX != null || pendingGridOffsetY != null) {
                                    repository.updateGridLayout(
                                        pendingGridWidthFraction  ?: savedGridWidthFraction,
                                        pendingGridHeightFraction ?: savedGridHeightFraction,
                                        pendingGridOffsetX ?: savedGridOffsetX,
                                        pendingGridOffsetY ?: savedGridOffsetY
                                    )
                                }
                                
                                // Apply wallpaper immediately (synchronous)
                                applyWallpaperNow(context, repository)
                                
                                // Clear pending changes
                                pendingPastColor = null
                                pendingTodayColor = null
                                pendingFutureColor = null
                                pendingBackgroundColor = null
                                pendingDotShape = null
                                pendingDotDensity = null
                                pendingGridWidthFraction  = null
                                pendingGridHeightFraction = null
                                pendingGridOffsetX = null
                                pendingGridOffsetY = null
                                
                                showSaveDialog = false
                                
                                Toast.makeText(
                                    context,
                                    "✓ Settings applied to lock screen!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                showSaveDialog = false
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ── Animated Color Palette Item with press scale ──────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteItem(
    modifier: Modifier = Modifier,
    topLabel: String,
    bottomLabel: String,
    color: Color,
    onClick: () -> Unit,
    hasGlow: Boolean = false,
    hasBorder: Boolean = false
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 400),
        label = "paletteColor"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "paletteScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bottomLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (hasBorder) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ) else Modifier
                    )
                    .clip(CircleShape)
                    .background(animatedColor)
                    .then(
                        if (hasGlow) Modifier.shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = animatedColor.copy(alpha = 0.45f),
                            spotColor = animatedColor.copy(alpha = 0.45f)
                        ) else Modifier
                    )
            )
        }
    }
}

// ── Animated Shape Selector with gliding capsule ─────────────────────────────
@Composable
fun ShapeSelectorItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "shapeSelectorBg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        content(selected)
    }
}
