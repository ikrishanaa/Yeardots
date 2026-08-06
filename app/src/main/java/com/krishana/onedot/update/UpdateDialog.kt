package com.krishana.onedot.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krishana.onedot.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Dialog lifecycle phases for the auto-updater dialog.
 */
private enum class UpdatePhase { PROMPT, DOWNLOADING, READY }

/**
 * Three-state Material 3 AlertDialog for the auto-updater.
 *
 * States:
 *  1. PROMPT   — "Update Available", Download button
 *  2. DOWNLOADING — animated LinearProgressIndicator with percentage
 *  3. READY    — "Ready to Install", Install button
 *
 * Usage from SettingsScreen:
 * ```
 * UpdateAvailableDialog(
 *     releaseInfo = updateState!!,
 *     updateManager = updateManager,
 *     onDismiss = { pendingUpdate.value = null }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    releaseInfo: UpdateManager.ReleaseInfo,
    updateManager: UpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var phase by remember { mutableStateOf(UpdatePhase.PROMPT) }
    var downloadPercent by remember { mutableIntStateOf(0) }
    var downloadFailed by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    // Cancel download coroutine when dialog leaves composition
    DisposableEffect(Unit) {
        onDispose { downloadJob?.cancel() }
    }

    AlertDialog(
        onDismissRequest = {
            downloadJob?.cancel()
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = when (phase) {
                    UpdatePhase.PROMPT -> "Update Available"
                    UpdatePhase.DOWNLOADING -> "Downloading\u2026"
                    UpdatePhase.READY -> "Ready to Install"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Version badges
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "New: ${releaseInfo.version}\t\u2192\tCurrent: v${BuildConfig.VERSION_NAME}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // ── PROMPT state ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = phase == UpdatePhase.PROMPT,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "A new version of YearDots is available. " +
                                   "Download now to get the latest improvements.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Changelog card
                        if (releaseInfo.changelog.isNotBlank()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = releaseInfo.changelog,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── DOWNLOADING state ─────────────────────────────────────
                AnimatedVisibility(
                    visible = phase == UpdatePhase.DOWNLOADING,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$downloadPercent%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        M3LinearProgressIndicator(
                            progress = downloadPercent / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            trackThickness = 8.dp
                        )

                        Text(
                            text = if (downloadFailed)
                                "Download failed. Check your connection."
                            else
                                "Please wait while we download the update\u2026",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── READY state ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = phase == UpdatePhase.READY,
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "The update has been downloaded.\n" +
                                   "Tap Install to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Retry button on failure ──────────────────────────────
                if (downloadFailed) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        downloadFailed = false
                        phase = UpdatePhase.PROMPT
                    }) {
                        Text("Try Again")
                    }
                }
            }
        },
        confirmButton = {
            when (phase) {
                UpdatePhase.PROMPT -> {
                    Button(
                        onClick = {
                            phase = UpdatePhase.DOWNLOADING
                            downloadJob = scope.launch {
                                val (_, flow) = updateManager.downloadApk(releaseInfo)
                                try {
                                    flow.collect { progress ->
                                        if (progress.failed) {
                                            downloadFailed = true
                                            phase = UpdatePhase.PROMPT
                                        } else {
                                            downloadPercent = progress.percent
                                            if (progress.isComplete) {
                                                phase = UpdatePhase.READY
                                            }
                                        }
                                    }
                                } catch (_: kotlinx.coroutines.CancellationException) {
                                    // Dialog dismissed — do nothing
                                }
                            }
                        }
                    ) {
                        Text("Download")
                    }
                }

                UpdatePhase.DOWNLOADING -> {
                    /* No button — user cancels via dismiss button */
                }

                UpdatePhase.READY -> {
                    Button(
                        onClick = {
                            val intent = updateManager.getInstallIntent()
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                            onDismiss()
                        }
                    ) {
                        Text("Install")
                    }
                }
            }
        },
        dismissButton = {
            when (phase) {
                UpdatePhase.PROMPT -> {
                    TextButton(onClick = {
                        downloadJob?.cancel()
                        onDismiss()
                    }) {
                        Text("Later")
                    }
                }
                UpdatePhase.DOWNLOADING -> {
                    OutlinedButton(onClick = {
                        downloadJob?.cancel()
                        onDismiss()
                    }) {
                        Text("Cancel")
                    }
                }
                UpdatePhase.READY -> {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}