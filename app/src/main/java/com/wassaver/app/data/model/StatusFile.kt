package com.wassaver.app.data.model

import android.net.Uri

data class StatusFile(
    val uri: Uri,
    val name: String,
    val dateModified: Long,
    val size: Long,
    val isVideo: Boolean,
    val isSaved: Boolean = false
) {
    val isImage: Boolean get() = !isVideo
}

enum class WhatsAppType(val displayName: String) {
    WHATSAPP("WhatsApp"),
    WHATSAPP_BUSINESS("WA Business")
}

enum class MediaFilter(val displayName: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos")
}
