package com.wassaver.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.wassaver.app.data.model.StatusFile
import com.wassaver.app.data.model.WhatsAppType
import java.io.File

class StatusRepository(private val context: Context) {

    companion object {
        // SAF tree URI paths for WhatsApp status folders
        private const val WA_STATUS_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        private const val WAB_STATUS_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp Business%2FMedia%2F.Statuses"

        // View Once (Private) folder paths — images
        private const val WA_PRIVATE_IMG_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2FWhatsApp Images%2FPrivate"
        private const val WAB_PRIVATE_IMG_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp Business%2FMedia%2FWhatsApp Images%2FPrivate"

        // View Once (Private) folder paths — videos
        private const val WA_PRIVATE_VID_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2FWhatsApp Video%2FPrivate"
        private const val WAB_PRIVATE_VID_SUFFIX = "Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp Business%2FMedia%2FWhatsApp Video%2FPrivate"

        // Legacy paths (Android 10 and below)
        private const val WA_LEGACY_PATH = "WhatsApp/Media/.Statuses"
        private const val WAB_LEGACY_PATH = "WhatsApp Business/Media/.Statuses"
        private const val WA_LEGACY_PRIVATE_IMG = "WhatsApp/Media/WhatsApp Images/Private"
        private const val WAB_LEGACY_PRIVATE_IMG = "WhatsApp Business/Media/WhatsApp Images/Private"
        private const val WA_LEGACY_PRIVATE_VID = "WhatsApp/Media/WhatsApp Video/Private"
        private const val WAB_LEGACY_PRIVATE_VID = "WhatsApp Business/Media/WhatsApp Video/Private"

        // Save directory
        private const val SAVE_DIR = "WASSaver"

        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "3gp", "webm")
        private val SUPPORTED_EXTENSIONS = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS

        fun getStatusTreeUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_STATUS_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_STATUS_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix")
        }

        fun getStatusDocumentUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_STATUS_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_STATUS_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix/document/primary%3A$suffix")
        }

        fun getInitialUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_STATUS_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_STATUS_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$suffix")
        }

        // View Once URI helpers
        fun getViewOnceImageTreeUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_IMG_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_IMG_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix")
        }

        fun getViewOnceVideoTreeUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_VID_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_VID_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix")
        }

        fun getViewOnceImageDocumentUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_IMG_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_IMG_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix/document/primary%3A$suffix")
        }

        fun getViewOnceVideoDocumentUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_VID_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_VID_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$suffix/document/primary%3A$suffix")
        }

        fun getViewOnceImageInitialUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_IMG_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_IMG_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$suffix")
        }

        fun getViewOnceVideoInitialUri(type: WhatsAppType): Uri {
            val suffix = when (type) {
                WhatsAppType.WHATSAPP -> WA_PRIVATE_VID_SUFFIX
                WhatsAppType.WHATSAPP_BUSINESS -> WAB_PRIVATE_VID_SUFFIX
            }
            return Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$suffix")
        }
    }

    /**
     * Check if we have persisted SAF permission for the given WhatsApp type.
     * Uses flexible matching — exact URI or path-based keyword matching.
     * Uses unique package identifiers to avoid com.whatsapp matching com.whatsapp.w4b
     */
    fun hasPermission(type: WhatsAppType): Boolean {
        val treeUri = getStatusTreeUri(type)
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        return context.contentResolver.persistedUriPermissions.any { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && (
                perm.uri == treeUri ||
                (path.contains(uniqueKeyword) && path.contains("Statuses"))
            )
        }
    }

    /**
     * Find the actual persisted URI for the status folder
     */
    private fun findPersistedStatusUri(type: WhatsAppType): Uri? {
        val treeUri = getStatusTreeUri(type)
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        return context.contentResolver.persistedUriPermissions.firstOrNull { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && (
                perm.uri == treeUri ||
                (path.contains(uniqueKeyword) && path.contains("Statuses"))
            )
        }?.uri
    }

    /**
     * Persist the URI permission after user grants access via SAF
     */
    fun persistPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load statuses from the WhatsApp status folder using SAF (Android 11+)
     * or direct file access (Android 10 and below)
     */
    fun loadStatuses(type: WhatsAppType): List<StatusFile> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            loadStatusesViaSAF(type)
        } else {
            loadStatusesLegacy(type)
        }
    }

    private fun loadStatusesViaSAF(type: WhatsAppType): List<StatusFile> {
        // Use actual persisted URI first, fall back to hardcoded ones
        val persistedUri = findPersistedStatusUri(type)
        val treeUri = persistedUri ?: getStatusTreeUri(type)
        val documentUri = getStatusDocumentUri(type)

        return try {
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
                ?: DocumentFile.fromTreeUri(context, documentUri)
                ?: return emptyList()

            val savedFiles = getSavedFileNames()

            documentFile.listFiles()
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
                        isSaved = name in savedFiles
                    )
                }
                .sortedByDescending { it.dateModified }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private fun loadStatusesLegacy(type: WhatsAppType): List<StatusFile> {
        val path = when (type) {
            WhatsAppType.WHATSAPP -> WA_LEGACY_PATH
            WhatsAppType.WHATSAPP_BUSINESS -> WAB_LEGACY_PATH
        }

        val statusDir = File(Environment.getExternalStorageDirectory(), path)
        if (!statusDir.exists() || !statusDir.isDirectory) return emptyList()

        val savedFiles = getSavedFileNames()

        return statusDir.listFiles()
            ?.filter { file ->
                val ext = file.extension.lowercase()
                ext in SUPPORTED_EXTENSIONS && !file.name.startsWith(".")
            }
            ?.map { file ->
                val ext = file.extension.lowercase()
                StatusFile(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    dateModified = file.lastModified(),
                    size = file.length(),
                    isVideo = ext in VIDEO_EXTENSIONS,
                    isSaved = file.name in savedFiles
                )
            }
            ?.sortedByDescending { it.dateModified }
            ?: emptyList()
    }

    /**
     * Save a status file to the device gallery
     */
    fun saveStatus(statusFile: StatusFile): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(statusFile.uri)
                ?: return false

            val mimeType = if (statusFile.isVideo) "video/mp4" else "image/jpeg"
            val relativePath = "${Environment.DIRECTORY_PICTURES}/$SAVE_DIR"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, statusFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }

                val collection = if (statusFile.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val uri = context.contentResolver.insert(collection, contentValues)
                    ?: return false

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
            } else {
                @Suppress("DEPRECATION")
                val saveDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    SAVE_DIR
                )
                if (!saveDir.exists()) saveDir.mkdirs()

                val outFile = File(saveDir, statusFile.name)
                outFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()

                // Notify media scanner
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if we have persisted SAF permission for View Once folders.
     * Uses flexible matching — checks if any persisted URI path contains the key folder segments.
     */
    fun hasViewOnceImagePermission(type: WhatsAppType): Boolean {
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        return context.contentResolver.persistedUriPermissions.any { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && path.contains(uniqueKeyword) && path.contains("Images") && path.contains("Private")
        }
    }

    fun hasViewOnceVideoPermission(type: WhatsAppType): Boolean {
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        return context.contentResolver.persistedUriPermissions.any { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && path.contains(uniqueKeyword) && path.contains("Video") && path.contains("Private")
        }
    }

    /**
     * Find the actual persisted URI for a View Once folder
     */
    private fun findPersistedViewOnceUri(type: WhatsAppType, isVideo: Boolean): Uri? {
        val uniqueKeyword = when (type) {
            WhatsAppType.WHATSAPP -> "com.whatsapp%2F"
            WhatsAppType.WHATSAPP_BUSINESS -> "com.whatsapp.w4b"
        }
        val mediaKeyword = if (isVideo) "Video" else "Images"
        return context.contentResolver.persistedUriPermissions.firstOrNull { perm ->
            val path = perm.uri.toString()
            perm.isReadPermission && path.contains(uniqueKeyword) && path.contains(mediaKeyword) && path.contains("Private")
        }?.uri
    }

    /**
     * Load View Once media from WhatsApp Private folders
     */
    fun loadViewOnceMedia(type: WhatsAppType): List<StatusFile> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            loadViewOnceViaSAF(type)
        } else {
            loadViewOnceLegacy(type)
        }
    }

    private fun loadViewOnceViaSAF(type: WhatsAppType): List<StatusFile> {
        val result = mutableListOf<StatusFile>()
        val savedFiles = getSavedFileNames()

        // Load private images
        val imgUri = findPersistedViewOnceUri(type, isVideo = false)
        if (imgUri != null) {
            try {
                val imgDoc = DocumentFile.fromTreeUri(context, imgUri)
                imgDoc?.listFiles()
                    ?.filter { file ->
                        val ext = file.name?.substringAfterLast(".", "")?.lowercase() ?: ""
                        ext in SUPPORTED_EXTENSIONS && file.name?.startsWith(".") == false
                    }
                    ?.forEach { file ->
                        val name = file.name ?: "unknown"
                        val ext = name.substringAfterLast(".", "").lowercase()
                        result.add(
                            StatusFile(
                                uri = file.uri,
                                name = name,
                                dateModified = file.lastModified(),
                                size = file.length(),
                                isVideo = ext in VIDEO_EXTENSIONS,
                                isSaved = name in savedFiles
                            )
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load private videos
        val vidUri = findPersistedViewOnceUri(type, isVideo = true)
        if (vidUri != null) {
            try {
                val vidDoc = DocumentFile.fromTreeUri(context, vidUri)
                vidDoc?.listFiles()
                    ?.filter { file ->
                        val ext = file.name?.substringAfterLast(".", "")?.lowercase() ?: ""
                        ext in SUPPORTED_EXTENSIONS && file.name?.startsWith(".") == false
                    }
                    ?.forEach { file ->
                        val name = file.name ?: "unknown"
                        val ext = name.substringAfterLast(".", "").lowercase()
                        result.add(
                            StatusFile(
                                uri = file.uri,
                                name = name,
                                dateModified = file.lastModified(),
                                size = file.length(),
                                isVideo = ext in VIDEO_EXTENSIONS,
                                isSaved = name in savedFiles
                            )
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result.sortedByDescending { it.dateModified }
    }

    @Suppress("DEPRECATION")
    private fun loadViewOnceLegacy(type: WhatsAppType): List<StatusFile> {
        val result = mutableListOf<StatusFile>()
        val savedFiles = getSavedFileNames()

        val imgPath = when (type) {
            WhatsAppType.WHATSAPP -> WA_LEGACY_PRIVATE_IMG
            WhatsAppType.WHATSAPP_BUSINESS -> WAB_LEGACY_PRIVATE_IMG
        }
        val vidPath = when (type) {
            WhatsAppType.WHATSAPP -> WA_LEGACY_PRIVATE_VID
            WhatsAppType.WHATSAPP_BUSINESS -> WAB_LEGACY_PRIVATE_VID
        }

        // Private images
        val imgDir = File(Environment.getExternalStorageDirectory(), imgPath)
        if (imgDir.exists() && imgDir.isDirectory) {
            imgDir.listFiles()?.filter { file ->
                val ext = file.extension.lowercase()
                ext in SUPPORTED_EXTENSIONS && !file.name.startsWith(".")
            }?.forEach { file ->
                val ext = file.extension.lowercase()
                result.add(
                    StatusFile(
                        uri = Uri.fromFile(file),
                        name = file.name,
                        dateModified = file.lastModified(),
                        size = file.length(),
                        isVideo = ext in VIDEO_EXTENSIONS,
                        isSaved = file.name in savedFiles
                    )
                )
            }
        }

        // Private videos
        val vidDir = File(Environment.getExternalStorageDirectory(), vidPath)
        if (vidDir.exists() && vidDir.isDirectory) {
            vidDir.listFiles()?.filter { file ->
                val ext = file.extension.lowercase()
                ext in SUPPORTED_EXTENSIONS && !file.name.startsWith(".")
            }?.forEach { file ->
                val ext = file.extension.lowercase()
                result.add(
                    StatusFile(
                        uri = Uri.fromFile(file),
                        name = file.name,
                        dateModified = file.lastModified(),
                        size = file.length(),
                        isVideo = ext in VIDEO_EXTENSIONS,
                        isSaved = file.name in savedFiles
                    )
                )
            }
        }

        return result.sortedByDescending { it.dateModified }
    }

    /**
     * Get URI for sharing/reposting a status
     */
    fun getShareUri(statusFile: StatusFile): Uri {
        return statusFile.uri
    }

    /**
     * Load saved statuses from the WASSaver directory
     */
    fun loadSavedStatuses(): List<StatusFile> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                loadSavedStatusesModern()
            } else {
                loadSavedStatusesLegacy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun loadSavedStatusesModern(): List<StatusFile> {
        val result = mutableListOf<StatusFile>()

        // Query images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        context.contentResolver.query(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            imageProjection,
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$SAVE_DIR%"),
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id.toString()
                )
                result.add(
                    StatusFile(
                        uri = uri,
                        name = cursor.getString(nameCol),
                        dateModified = cursor.getLong(dateCol) * 1000,
                        size = cursor.getLong(sizeCol),
                        isVideo = false,
                        isSaved = true
                    )
                )
            }
        }

        // Query videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RELATIVE_PATH
        )

        context.contentResolver.query(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            videoProjection,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%$SAVE_DIR%"),
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id.toString()
                )
                result.add(
                    StatusFile(
                        uri = uri,
                        name = cursor.getString(nameCol),
                        dateModified = cursor.getLong(dateCol) * 1000,
                        size = cursor.getLong(sizeCol),
                        isVideo = true,
                        isSaved = true
                    )
                )
            }
        }

        return result.sortedByDescending { it.dateModified }
    }

    @Suppress("DEPRECATION")
    private fun loadSavedStatusesLegacy(): List<StatusFile> {
        val saveDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SAVE_DIR
        )
        if (!saveDir.exists()) return emptyList()

        return saveDir.listFiles()
            ?.filter { file ->
                val ext = file.extension.lowercase()
                ext in SUPPORTED_EXTENSIONS
            }
            ?.map { file ->
                val ext = file.extension.lowercase()
                StatusFile(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    dateModified = file.lastModified(),
                    size = file.length(),
                    isVideo = ext in VIDEO_EXTENSIONS,
                    isSaved = true
                )
            }
            ?.sortedByDescending { it.dateModified }
            ?: emptyList()
    }

    private fun getSavedFileNames(): Set<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val names = mutableSetOf<String>()
                // Check images
                context.contentResolver.query(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
                    arrayOf("%$SAVE_DIR%"),
                    null
                )?.use { cursor ->
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        names.add(cursor.getString(nameCol))
                    }
                }
                // Check videos
                context.contentResolver.query(
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    arrayOf(MediaStore.Video.Media.DISPLAY_NAME),
                    "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
                    arrayOf("%$SAVE_DIR%"),
                    null
                )?.use { cursor ->
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        names.add(cursor.getString(nameCol))
                    }
                }
                names
            } else {
                @Suppress("DEPRECATION")
                val saveDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    SAVE_DIR
                )
                saveDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }
}
