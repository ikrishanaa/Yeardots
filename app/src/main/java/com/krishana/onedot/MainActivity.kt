package com.krishana.onedot

import android.app.WallpaperManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishana.onedot.core.WallpaperGenerator
import com.krishana.onedot.BuildConfig
import com.krishana.onedot.data.SettingsRepository
import com.krishana.onedot.ui.components.AboutDialog
import com.krishana.onedot.ui.components.ColorSettingRow
import com.krishana.onedot.ui.components.DebugInfoRow
import com.krishana.onedot.ui.components.ImprovedColorPickerDialog
import com.krishana.onedot.ui.components.ShapeOptionItem
import com.krishana.onedot.ui.theme.OneDotTheme
import com.krishana.onedot.update.UpdateAvailableDialog
import com.krishana.onedot.update.UpdateManager
import com.krishana.onedot.util.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
            val hasWallpaperPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.SET_WALLPAPER
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            android.util.Log.d("YearDots", "SET_WALLPAPER permission: $hasWallpaperPerm")
            if (!hasWallpaperPerm) {
                android.util.Log.e("YearDots", "ERROR: SET_WALLPAPER permission not granted!")
                throw SecurityException("SET_WALLPAPER permission not granted. Please enable it in app permissions.")
            }
            android.util.Log.d("YearDots", "All permissions OK, getting WallpaperManager...")
            val wallpaperManager = WallpaperManager.getInstance(context)
            val desiredWidth = wallpaperManager.desiredMinimumWidth
            val desiredHeight = wallpaperManager.desiredMinimumHeight

            val (width, height) = if (desiredWidth > 0 && desiredHeight > 0) {
                Pair(desiredWidth, desiredHeight)
            } else {
                val displayMetrics = context.resources.displayMetrics
                Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
            
            android.util.Log.d("YearDots", "Wallpaper dimensions: ${width}x${height}")
            try {
                wallpaperManager.suggestDesiredDimensions(width, height)
            } catch (_: SecurityException) { }
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
            val themeConfig = WallpaperGenerator.ThemeConfig(
                pastColor = pastColor,
                todayColor = todayColor,
                futureColor = futureColor,
                backgroundColor = backgroundColor,
                dotShape = dotShape,
                dotDensity = dotDensity,
                gridLayout = WallpaperGenerator.GridLayout(gridWidthFraction, gridHeightFraction, gridOffsetX, gridOffsetY)
            )
            android.util.Log.d("YearDots", "Generating wallpaper bitmap...")
            val bitmap = WallpaperGenerator.generateBitmap(width, height, themeConfig)
            
            val stream = java.io.ByteArrayOutputStream()
            try {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                val inputStream = java.io.ByteArrayInputStream(stream.toByteArray())
                try {
                    android.util.Log.d("YearDots", "Setting wallpaper on LOCK SCREEN...")
                    wallpaperManager.setStream(inputStream, null, false, WallpaperManager.FLAG_LOCK)
                    android.util.Log.d("YearDots", "✓ Wallpaper set successfully at ${width}x${height}!")
                } finally {
                    inputStream.close()
                }
            } catch (e: SecurityException) {
                throw SecurityException("Permission denied when setting wallpaper.", e)
            } catch (e: IllegalArgumentException) {
                throw Exception("Invalid wallpaper size or format.", e)
            } catch (e: Exception) {
                throw Exception("Failed to set wallpaper: ${e.message}", e)
            } finally {
                stream.close()
                bitmap.recycle()
            }
        } catch (e: Exception) {
            throw Exception("Wallpaper update failed: ${e.message}", e)
        } finally {
            android.util.Log.d("YearDots", "=== WALLPAPER UPDATE END ===")
        }
    }
}


class MainActivity : ComponentActivity() {
    private var hasPermissions by mutableStateOf(false)
    private val updateManager by lazy { UpdateManager(this) }

    // MutableStateFlow so Compose can collect it reactively
    private val _pendingUpdate = MutableStateFlow<UpdateManager.ReleaseInfo?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        hasPermissions = allGranted
        if (!allGranted) {
            Toast.makeText(this, "⚠️ Permissions are needed for the app to function properly", Toast.LENGTH_LONG).show()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        if (!hasPermissions) requestPermissions()
        WorkScheduler.scheduleDailyWallpaperUpdate(this)

        // ── Silently check for updates on GitHub ──────────────────────
        lifecycleScope.launch {
            try {
                val latest = updateManager.fetchLatestRelease()
                if (latest != null) {
                    val currentVersion = BuildConfig.VERSION_NAME
                    if (updateManager.compareVersions(latest.version, currentVersion) > 0) {
                        _pendingUpdate.value = latest
                    }
                }
            } catch (_: Exception) {
                // Silently skip — user doesn't need to see network errors
            }
        }

        setContent {
            OneDotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!hasPermissions) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⚠️ Permissions Needed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Please grant all permissions to allow the wallpaper to update automatically.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { requestPermissions() }) { Text("Grant Permissions") }
                                }
                            }
                        }
                        SettingsScreen(pendingUpdate = _pendingUpdate)
                    }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); checkPermissions() }
    private fun checkPermissions() {
        val permissions = getRequiredPermissions()
        hasPermissions = permissions.all { androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    }
    private fun requestPermissions() { permissionLauncher.launch(getRequiredPermissions().toTypedArray()) }
    private fun getRequiredPermissions(): List<String> = listOf(android.Manifest.permission.SET_WALLPAPER)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Bottom Navigation
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pendingUpdate: MutableStateFlow<UpdateManager.ReleaseInfo?>
) {
    val context = LocalContext.current
    val updateManager = remember { UpdateManager(context) }
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Collect saved colors from DataStore ───────────────────────────────
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

    // ── Pending changes ───────────────────────────────────────────────────
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

    var showPastColorPicker by remember { mutableStateOf(false) }
    var showTodayColorPicker by remember { mutableStateOf(false) }
    var showFutureColorPicker by remember { mutableStateOf(false) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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

    val hasChanges = pendingPastColor != null || pendingTodayColor != null ||
                     pendingFutureColor != null || pendingBackgroundColor != null ||
                     pendingDotShape != null || pendingDotDensity != null ||
                     pendingGridWidthFraction != null || pendingGridHeightFraction != null ||
                     pendingGridOffsetX != null || pendingGridOffsetY != null

    // ── Scaffold with bottom navigation ──────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp, 
                        color = Color.White.copy(alpha = 0.15f), 
                        shape = RoundedCornerShape(24.dp)
                    ),
                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f),
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                    label = { Text("Layout") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.White.copy(alpha = 0.15f),
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    label = { Text("Customize") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.White.copy(alpha = 0.15f),
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
                    label = { Text("Goal") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.White.copy(alpha = 0.15f),
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> LayoutTab(
                bottomPadding = paddingValues.calculateBottomPadding(),
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
                onBack = { /* stay on layout */ },
                onSave = { wf, hf, ox, oy ->
                    pendingGridWidthFraction = wf
                    pendingGridHeightFraction = hf
                    pendingGridOffsetX = ox
                    pendingGridOffsetY = oy
                },
                onApplyToLockscreen = { showSaveDialog = true }
            )
            1 -> CustomizeTab(
                modifier = Modifier.padding(paddingValues),
                currentPastColor = currentPastColor,
                currentTodayColor = currentTodayColor,
                currentFutureColor = currentFutureColor,
                currentBackgroundColor = currentBackgroundColor,
                currentDotShape = currentDotShape,
                currentDotDensity = currentDotDensity,
                hasChanges = hasChanges,
                pendingGridWidthFraction = pendingGridWidthFraction,
                pendingGridHeightFraction = pendingGridHeightFraction,
                pendingGridOffsetX = pendingGridOffsetX,
                pendingGridOffsetY = pendingGridOffsetY,
                onPastColorClick = { showPastColorPicker = true },
                onTodayColorClick = { showTodayColorPicker = true },
                onFutureColorClick = { showFutureColorPicker = true },
                onBackgroundColorClick = { showBackgroundColorPicker = true },
                onShapeChange = { pendingDotShape = it },
                onDensityChange = { pendingDotDensity = it },
                onSaveClick = { showSaveDialog = true },
                onAboutClick = { showAboutDialog = true }
            )
            2 -> GoalTab(modifier = Modifier.padding(paddingValues))
        }
    }

    // ── Dialogs (rendered as overlays) ───────────────────────────────────
    if (showPastColorPicker) {
        ImprovedColorPickerDialog(
            title = "Past Days Color",
            currentColor = Color(currentPastColor),
            onDismiss = { showPastColorPicker = false },
            onColorSelected = { color -> pendingPastColor = color.toArgb(); showPastColorPicker = false }
        )
    }
    if (showTodayColorPicker) {
        ImprovedColorPickerDialog(
            title = "Current Day Color",
            currentColor = Color(currentTodayColor),
            onDismiss = { showTodayColorPicker = false },
            onColorSelected = { color -> pendingTodayColor = color.toArgb(); showTodayColorPicker = false }
        )
    }
    if (showFutureColorPicker) {
        ImprovedColorPickerDialog(
            title = "Future Days Color",
            currentColor = Color(currentFutureColor),
            onDismiss = { showFutureColorPicker = false },
            onColorSelected = { color -> pendingFutureColor = color.toArgb(); showFutureColorPicker = false }
        )
    }
    if (showBackgroundColorPicker) {
        ImprovedColorPickerDialog(
            title = "Background Color",
            currentColor = Color(currentBackgroundColor),
            onDismiss = { showBackgroundColorPicker = false },
            onColorSelected = { color -> pendingBackgroundColor = color.toArgb(); showBackgroundColorPicker = false }
        )
    }
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Apply Settings?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("This will save your changes and apply them to your lock screen wallpaper.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            pendingPastColor?.let { repository.updatePastColor(it) }
                            pendingTodayColor?.let { repository.updateTodayColor(it) }
                            pendingFutureColor?.let { repository.updateFutureColor(it) }
                            pendingBackgroundColor?.let { repository.updateBackgroundColor(it) }
                            pendingDotShape?.let { repository.updateDotShape(it) }
                            pendingDotDensity?.let { repository.updateDotDensity(it) }
                            if (pendingGridWidthFraction != null || pendingGridHeightFraction != null || pendingGridOffsetX != null || pendingGridOffsetY != null) {
                                repository.updateGridLayout(
                                    pendingGridWidthFraction ?: savedGridWidthFraction,
                                    pendingGridHeightFraction ?: savedGridHeightFraction,
                                    pendingGridOffsetX ?: savedGridOffsetX,
                                    pendingGridOffsetY ?: savedGridOffsetY
                                )
                            }
                            applyWallpaperNow(context, repository)
                            pendingPastColor = null; pendingTodayColor = null; pendingFutureColor = null
                            pendingBackgroundColor = null; pendingDotShape = null; pendingDotDensity = null
                            pendingGridWidthFraction = null; pendingGridHeightFraction = null
                            pendingGridOffsetX = null; pendingGridOffsetY = null
                            showSaveDialog = false
                            Toast.makeText(context, "✓ Settings applied to lock screen!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            showSaveDialog = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Auto-updater dialog ────────────────────────────────────────────
    val updateState by pendingUpdate.collectAsState()
    if (updateState != null) {
        UpdateAvailableDialog(
            releaseInfo = updateState!!,
            updateManager = updateManager,
            onDismiss = { pendingUpdate.value = null }
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// Tab 3 — Goal (Coming Soon)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GoalTab(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrackChanges,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Tab 2 — Customize
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeTab(
    modifier: Modifier = Modifier,
    currentPastColor: Int,
    currentTodayColor: Int,
    currentFutureColor: Int,
    currentBackgroundColor: Int,
    currentDotShape: String,
    currentDotDensity: Int,
    hasChanges: Boolean,
    pendingGridWidthFraction: Float?,
    pendingGridHeightFraction: Float?,
    pendingGridOffsetX: Float?,
    pendingGridOffsetY: Float?,
    onPastColorClick: () -> Unit,
    onTodayColorClick: () -> Unit,
    onFutureColorClick: () -> Unit,
    onBackgroundColorClick: () -> Unit,
    onShapeChange: (String) -> Unit,
    onDensityChange: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header: app name + About icon ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()           // clears status bar on all devices
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Yearsdots",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = onAboutClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── Color Palette Card ────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "COLOR PALETTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ColorPaletteItem(modifier = Modifier.weight(1f), topLabel = "PAST", bottomLabel = "Days", color = Color(currentPastColor), onClick = onPastColorClick)
                        ColorPaletteItem(modifier = Modifier.weight(1f), topLabel = "TODAY", bottomLabel = "Current", color = Color(currentTodayColor), onClick = onTodayColorClick, hasGlow = true)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ColorPaletteItem(modifier = Modifier.weight(1f), topLabel = "FUTURE", bottomLabel = "Days", color = Color(currentFutureColor), onClick = onFutureColorClick)
                        ColorPaletteItem(modifier = Modifier.weight(1f), topLabel = "BASE", bottomLabel = "Theme", color = Color(currentBackgroundColor), onClick = onBackgroundColorClick, hasBorder = true)
                    }
                }
            }
        }

        // ── Shape Card ────────────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SHAPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val shapes = listOf("circle", "rounded", "square", "pill")
                        shapes.forEachIndexed { index, shapeStr ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = shapes.size),
                                onClick = { onShapeChange(shapeStr) },
                                selected = currentDotShape == shapeStr,
                                icon = {},
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                val isSelected = currentDotShape == shapeStr
                                val shapeColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                when (shapeStr) {
                                    "circle" -> Box(modifier = Modifier.size(20.dp).background(shapeColor, CircleShape))
                                    "rounded" -> Box(modifier = Modifier.size(20.dp).background(shapeColor, RoundedCornerShape(6.dp)))
                                    "square" -> Box(modifier = Modifier.size(20.dp).background(shapeColor, androidx.compose.ui.graphics.RectangleShape))
                                    "pill" -> Box(modifier = Modifier.width(24.dp).height(12.dp).background(shapeColor, RoundedCornerShape(50)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Size Card ─────────────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 20.dp)
                )
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val labels = listOf("Tiny", "Small", "Medium", "Large")
                        labels.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                                onClick = { onDensityChange(index) },
                                selected = index == currentDotDensity,
                                icon = {},
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = if (index == currentDotDensity) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Save Button
        if (hasChanges) {
            FilledTonalButton(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Apply to Lockscreen", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Tab 1 — Layout
// ════════════════════════════════════════════════════════════════════════════════

@Composable
private fun LayoutTab(
    bottomPadding: androidx.compose.ui.unit.Dp,
    initialWidthFraction: Float,
    initialHeightFraction: Float,
    initialOffsetX: Float,
    initialOffsetY: Float,
    pastColor: Color,
    todayColor: Color,
    futureColor: Color,
    backgroundColor: Color,
    dotShape: String,
    dotDensity: Int,
    onBack: () -> Unit,
    onSave: (widthFraction: Float, heightFraction: Float, offsetX: Float, offsetY: Float) -> Unit,
    onApplyToLockscreen: () -> Unit,
) {
    com.krishana.onedot.ui.components.LayoutEditorScreen(
        initialWidthFraction  = initialWidthFraction,
        initialHeightFraction = initialHeightFraction,
        initialOffsetX = initialOffsetX,
        initialOffsetY = initialOffsetY,
        pastColor = pastColor,
        todayColor = todayColor,
        futureColor = futureColor,
        backgroundColor = backgroundColor,
        dotShape = dotShape,
        dotDensity = dotDensity,
        onDismiss = onBack,
        onSave = onSave,
        onApplyToLockscreen = onApplyToLockscreen,
        bottomPadding = bottomPadding,
    )
}


// ═══════════════════════════════════════════════════════════════════════════════
// Shared Helpers
// ═══════════════════════════════════════════════════════════════════════════════

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
