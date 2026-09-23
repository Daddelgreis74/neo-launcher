package com.neodeck.launcher.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val publishedAt: String,
    val isNewer: Boolean,
    val fileSizeFormatted: String = ""
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data object UpToDate : UpdateState
    data class Downloading(val progress: Float, val info: UpdateInfo) : UpdateState
    data class ReadyToInstall(val apkFile: File, val info: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

class GitHubUpdateManager(private val context: Context) {

    companion object {
        const val DEFAULT_REPO_OWNER = "Daddelgreis74"
        const val DEFAULT_REPO_NAME = "neo-launcher"
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(
        owner: String = DEFAULT_REPO_OWNER,
        repo: String = DEFAULT_REPO_NAME
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NeoLauncher-Android")
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(
                    Exception("GitHub API Antwort: ${connection.responseCode} (${connection.responseMessage})")
                )
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            val tagName = json.optString("tag_name", "").removePrefix("v").trim()
            val releaseTitle = json.optString("name", "Version $tagName")
            val body = json.optString("body", "Keine Versionshinweise verfügbar.")
            val publishedAt = json.optString("published_at", "")

            // Find .apk asset
            val assets = json.optJSONArray("assets")
            var apkDownloadUrl = ""
            var apkSizeBytes = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val currentVer = getCurrentVersionName().removePrefix("v").trim()
            val isNewer = isVersionNewer(tagName, currentVer)

            val sizeMb = if (apkSizeBytes > 0) {
                String.format(java.util.Locale.US, "%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
            } else ""

            val info = UpdateInfo(
                versionName = tagName,
                releaseTitle = releaseTitle,
                changelog = body,
                downloadUrl = apkDownloadUrl,
                publishedAt = publishedAt,
                isNewer = isNewer,
                fileSizeFormatted = sizeMb
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadUpdate(
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (updateInfo.downloadUrl.isBlank()) {
                return@withContext Result.failure(Exception("Keine APK-Datei im GitHub Release gefunden."))
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updatesDir, "NeoLauncher-v${updateInfo.versionName}.apk")

            var currentUrl = updateInfo.downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            // Follow HTTP redirects (GitHub releases redirect to AWS S3 / objects.githubusercontent.com)
            while (true) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "NeoLauncher-Android")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    currentUrl = connection.getHeaderField("Location") ?: break
                    redirects++
                    if (redirects > 5) break
                } else {
                    break
                }
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            // Check unknown sources permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    return Result.failure(
                        Exception("Bitte Berechtigung für 'Unbekannte Apps installieren' in den Einstellungen aktivieren.")
                    )
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
