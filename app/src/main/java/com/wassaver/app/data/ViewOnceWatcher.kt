package com.wassaver.app.data

import android.content.Context
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.wassaver.app.data.model.StatusFile
import com.wassaver.app.data.model.WhatsAppType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Watches WhatsApp Private (View Once) folders in the background while the app is open.
 * When new files appear, they are auto-saved to the gallery immediately.
 */
class ViewOnceWatcher(
    private val context: Context,
    private val repository: StatusRepository
) {
    private var watchJob: Job? = null
    private val knownFiles = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _autoSaveCount = MutableStateFlow(0)
    val autoSaveCount: StateFlow<Int> = _autoSaveCount.asStateFlow()

    private val _isWatching = MutableStateFlow(false)
    val isWatching: StateFlow<Boolean> = _isWatching.asStateFlow()

    companion object {
        private const val POLL_INTERVAL_MS = 5000L // Check every 5 seconds
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "3gp", "webm")
        private val SUPPORTED_EXTENSIONS = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
    }

    /**
     * Start watching all View Once folders that we have permission for.
     * Initial scan populates known files without saving them (they may already exist).
     */
    fun startWatching() {
        if (watchJob?.isActive == true) return

        _isWatching.value = true
        watchJob = scope.launch {
            // Initial scan — record existing files so we don't re-save old ones
            val initialFiles = scanAllPrivateFolders()
            knownFiles.addAll(initialFiles.map { it.name })

            // Polling loop
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                try {
                    val currentFiles = scanAllPrivateFolders()
                    val newFiles = currentFiles.filter { it.name !in knownFiles }

                    for (file in newFiles) {
                        knownFiles.add(file.name)
                        if (!file.isSaved) {
                            val success = repository.saveStatus(file)
                            if (success) {
                                _autoSaveCount.value++
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "📥 Media auto-saved: ${if (file.isVideo) "Video" else "Photo"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        _isWatching.value = false
    }

    fun resetCount() {
        _autoSaveCount.value = 0
    }

    private fun scanAllPrivateFolders(): List<StatusFile> {
        val result = mutableListOf<StatusFile>()

        // Scan both WA and WAB
        for (type in WhatsAppType.entries) {
            result.addAll(scanPrivateFolder(type, isVideo = false))
            result.addAll(scanPrivateFolder(type, isVideo = true))
        }

        return result
    }

    private fun scanPrivateFolder(type: WhatsAppType, isVideo: Boolean): List<StatusFile> {
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        val mediaKeyword = if (isVideo) "Video" else "Images"

        val uri = context.contentResolver.persistedUriPermissions.firstOrNull { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && path.contains(uniqueKeyword) && path.contains(mediaKeyword) && path.contains("Private")
        }?.uri ?: return emptyList()

        return try {
            val doc = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
            val savedFiles = try {
                // Quick check without full MediaStore query
                emptySet<String>()
            } catch (_: Exception) {
                emptySet()
            }

            (doc.listFiles() ?: emptyArray())
                .filter { file ->
                    val ext = file.name?.substringAfterLast(".", "")?.lowercase() ?: ""
                    ext in SUPPORTED_EXTENSIONS && file.name?.startsWith(".") == false
                }
                .map { file ->
                    val name = file.name ?: "unknown"
                    val ext = name.substringAfterLast(".", "").lowercase()
                    StatusFile(
                        uri = file.uri,
                        name = name,
                        dateModified = file.lastModified(),
                        size = file.length(),
                        isVideo = ext in VIDEO_EXTENSIONS,
                        isSaved = false
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
