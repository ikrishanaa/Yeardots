package com.krishana.onedot.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages update checking, APK downloading, and APK installation for the
 * YearDots app.
 *
 * All network / disk work runs on [Dispatchers.IO]. The caller (Compose UI)
 * owns the lifecycle — flows are cold and cancel when the collecting coroutine
 * is cancelled.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_API =
            "https://api.github.com/repos/ikrishanaa/Yeardots/releases/latest"
        private const val DOWNLOAD_DIR = "Updates"
        private const val APK_FILENAME = "update.apk"
    }

    // ── Public data types ─────────────────────────────────────────────────

    data class ReleaseInfo(
        val version: String,       // e.g. "v2.1.0"
        val downloadUrl: String,   // direct .apk asset download URL
        val fileSizeBytes: Long,
        val changelog: String,     // truncated to ~800 chars
        val publishedAt: String
    )

    data class DownloadProgress(
        val bytesDownloaded: Long = 0,
        val bytesTotal: Long = 0,
        val failed: Boolean = false
    ) {
        val isComplete: Boolean
            get() = bytesTotal > 0 && bytesDownloaded >= bytesTotal

        val fraction: Float
            get() = if (bytesTotal > 0) {
                bytesDownloaded.toFloat() / bytesTotal
            } else 0f

        val percent: Int
            get() = (fraction * 100).toInt().coerceIn(0, 100)
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VERSION COMPARISON
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Numeric semver comparison.
     *
     * Examples:
     *   compareVersions("v1.9.1", "v2.0.0")  → -1
     *   compareVersions("v2.10",  "v2.2")    →  1   (10 > 2)
     *   compareVersions("2.0.2",  "v2.0.2")  →  0
     */
    fun compareVersions(a: String, b: String): Int {
        val partsA = a.trimStart('v', 'V').split(".")
            .map { it.toIntOrNull() ?: 0 }
        val partsB = b.trimStart('v', 'V').split(".")
            .map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            val diff = va - vb
            if (diff != 0) return diff
        }
        return 0
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GITHUB API — FETCH LATEST RELEASE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches the latest release from the GitHub API.
     *
     * Returns null silently on *any* failure — the caller should treat null
     * as "no update available right now" rather than surfacing an error.
     *
     * Failures include: no network, rate limiting, 404, or no .apk asset.
     */
    suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.getString("tag_name")
            val bodyText = json.optString("body", "")

            // Find the first .apk asset
            val assets = json.getJSONArray("assets")
            var downloadUrl: String? = null
            var fileSize: Long = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    fileSize = asset.getLong("size")
                    break
                }
            }
            if (downloadUrl == null) return@withContext null

            ReleaseInfo(
                version = tag,
                downloadUrl = downloadUrl,
                fileSizeBytes = fileSize,
                changelog = bodyText.take(800),
                publishedAt = json.optString("published_at", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DOWNLOAD  — DownloadManager + polling Flow
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Enqueues the APK download via [DownloadManager] (hidden notification).
     * Returns the download ID plus a cold [Flow] that polls every 500 ms
     * and emits [DownloadProgress].
     *
     * The flow completes after the final success/failure emission.
     * Cancelling the coroutine stops the polling.
     */
    fun downloadApk(release: ReleaseInfo): Pair<Long, Flow<DownloadProgress>> {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_DIR
        )
        if (!dir.exists()) dir.mkdirs()
        val apkFile = File(dir, APK_FILENAME)
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(release.downloadUrl))
            .setTitle("YearDots Update")
            .setDescription("Downloading ${release.version}…")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_HIDDEN
            )
            .setDestinationUri(Uri.fromFile(apkFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val mgr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = mgr.enqueue(request)

        val progressFlow: Flow<DownloadProgress> = flow {
            var done = false
            while (!done) {
                val prog = queryProgress(mgr, downloadId)
                emit(prog)
                if (prog.isComplete || prog.failed) {
                    done = true
                } else {
                    delay(500L)
                }
            }
        }

        return downloadId to progressFlow
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTALL  — FileProvider Intent
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns an [Intent.ACTION_VIEW] intent pointing at the downloaded .apk
     * through the app's FileProvider. Returns null if the file is missing.
     */
    fun getInstallIntent(): Intent? {
        return try {
            val apkFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "$DOWNLOAD_DIR/$APK_FILENAME"
            )
            if (!apkFile.exists()) return null

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Internals
    // ═══════════════════════════════════════════════════════════════════════

    private fun queryProgress(mgr: DownloadManager, id: Long): DownloadProgress {
        var cursor: Cursor? = null
        try {
            cursor = mgr.query(DownloadManager.Query().setFilterById(id))
            if (cursor == null || cursor.moveToFirst()) return DownloadProgress()

            val sIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val dIdx = cursor.getColumnIndex(
                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
            )
            val tIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            return when (cursor.getInt(sIdx)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val total = cursor.getLong(tIdx).takeIf { it > 0 }
                        ?: cursor.getLong(dIdx).takeIf { it > 0 } ?: 0L
                    DownloadProgress(bytesDownloaded = total, bytesTotal = total)
                }
                DownloadManager.STATUS_FAILED -> DownloadProgress(failed = true)
                else -> DownloadProgress(
                    bytesDownloaded = cursor.getLong(dIdx).takeIf { it > 0 } ?: 0,
                    bytesTotal = cursor.getLong(tIdx).takeIf { it > 0 } ?: 0
                )
            }
        } finally {
            cursor?.close()
        }
    }
}