package com.wassaver.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks for app updates via GitHub Releases API.
 * No external library needed — uses built-in HttpURLConnection + org.json.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL = "https://api.github.com/repos/PBhadoo/WASSaver-Android/releases"
    private const val RELEASES_PAGE_URL = "https://github.com/PBhadoo/WASSaver-Android/releases"

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val releaseName: String,
        val body: String,
        val publishedAt: String,
        val htmlUrl: String,
        val apkDownloadUrl: String?,
        val apkSize: Long
    )

    data class UpdateResult(
        val hasUpdate: Boolean,
        val currentVersion: String,
        val latestRelease: ReleaseInfo?
    )

    /**
     * Check for updates by comparing current app version with latest GitHub release tag.
     */
    suspend fun checkForUpdate(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val latestRelease = fetchLatestRelease()

            if (latestRelease == null) {
                return@withContext UpdateResult(false, currentVersion, null)
            }

            val hasUpdate = isNewerVersion(currentVersion, latestRelease.versionName)
            UpdateResult(hasUpdate, currentVersion, latestRelease)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateResult(false, getCurrentVersion(context), null)
        }
    }

    /**
     * Fetch the latest release from GitHub API
     */
    private fun fetchLatestRelease(): ReleaseInfo? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$RELEASES_URL/latest").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "WASSaver-Android")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                parseRelease(JSONObject(response))
            } else {
                Log.w(TAG, "GitHub API returned ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching release", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetch all releases from GitHub API
     */
    suspend fun fetchAllReleases(): List<ReleaseInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL("$RELEASES_URL?per_page=10").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "WASSaver-Android")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val releases = mutableListOf<ReleaseInfo>()
                for (i in 0 until jsonArray.length()) {
                    parseRelease(jsonArray.getJSONObject(i))?.let { releases.add(it) }
                }
                releases
            } else {
                Log.w(TAG, "GitHub API returned ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching releases", e)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRelease(json: JSONObject): ReleaseInfo? {
        return try {
            val tagName = json.getString("tag_name")
            val versionName = tagName.removePrefix("v")
            val releaseName = json.optString("name", tagName)
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", RELEASES_PAGE_URL)

            // Find APK asset
            var apkUrl: String? = null
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        apkSize = asset.optLong("size", 0)
                        break
                    }
                }
            }

            ReleaseInfo(
                tagName = tagName,
                versionName = versionName,
                releaseName = releaseName,
                body = body,
                publishedAt = publishedAt,
                htmlUrl = htmlUrl,
                apkDownloadUrl = apkUrl,
                apkSize = apkSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing release", e)
            null
        }
    }

    /**
     * Get current app version name
     */
    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Compare version strings. Returns true if remote > current.
     */
    fun isNewerVersion(current: String, remote: String): Boolean {
        try {
            val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Open the releases page in browser
     */
    fun openReleasesPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening releases page", e)
        }
    }

    /**
     * Open a specific release URL in browser to download
     */
    fun openDownloadPage(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening download page", e)
        }
    }

    /**
     * Format file size
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * Format published date from ISO 8601 to readable format
     */
    fun formatDate(isoDate: String): String {
        return try {
            val parts = isoDate.split("T")
            if (parts.isNotEmpty()) parts[0] else isoDate
        } catch (e: Exception) {
            isoDate
        }
    }
}
