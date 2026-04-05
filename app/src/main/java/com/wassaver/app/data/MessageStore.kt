package com.wassaver.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple file-based storage for captured WhatsApp messages.
 * Stores messages as JSON in SharedPreferences.
 */
object MessageStore {

    private const val PREFS_NAME = "wassaver_messages"
    private const val KEY_MESSAGES = "captured_messages"
    private const val KEY_LISTENER_ENABLED = "listener_enabled"
    private const val MAX_MESSAGES = 500 // Keep last 500 messages

    data class CapturedMessage(
        val id: String,
        val sender: String,
        val text: String,
        val timestamp: Long,
        val packageName: String, // com.whatsapp or com.whatsapp.w4b
        val isGroup: Boolean,
        val groupName: String?
    ) {
        val formattedTime: String
            get() {
                val sdf = SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault())
                return sdf.format(Date(timestamp))
            }

        val formattedDate: String
            get() {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                return sdf.format(Date(timestamp))
            }

        val appName: String
            get() = if (packageName.contains("w4b")) "WA Business" else "WhatsApp"
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if the notification listener feature is enabled by the user
     */
    fun isListenerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LISTENER_ENABLED, false)
    }

    /**
     * Set whether the notification listener feature is enabled
     */
    fun setListenerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LISTENER_ENABLED, enabled).apply()
    }

    /**
     * Save a captured message
     */
    @Synchronized
    fun saveMessage(context: Context, message: CapturedMessage) {
        if (!isListenerEnabled(context)) return

        val prefs = getPrefs(context)
        val messagesJson = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val jsonArray = try {
            JSONArray(messagesJson)
        } catch (e: Exception) {
            JSONArray()
        }

        // Check for duplicate (same sender + text within 2 seconds)
        for (i in (jsonArray.length() - 1) downTo maxOf(0, jsonArray.length() - 5)) {
            val existing = jsonArray.getJSONObject(i)
            if (existing.optString("sender") == message.sender &&
                existing.optString("text") == message.text &&
                Math.abs(existing.optLong("timestamp") - message.timestamp) < 2000
            ) {
                return // Duplicate, skip
            }
        }

        val messageJson = JSONObject().apply {
            put("id", message.id)
            put("sender", message.sender)
            put("text", message.text)
            put("timestamp", message.timestamp)
            put("packageName", message.packageName)
            put("isGroup", message.isGroup)
            put("groupName", message.groupName ?: "")
        }

        jsonArray.put(messageJson)

        // Trim to max messages
        val trimmedArray = if (jsonArray.length() > MAX_MESSAGES) {
            val newArray = JSONArray()
            for (i in (jsonArray.length() - MAX_MESSAGES) until jsonArray.length()) {
                newArray.put(jsonArray.getJSONObject(i))
            }
            newArray
        } else {
            jsonArray
        }

        prefs.edit().putString(KEY_MESSAGES, trimmedArray.toString()).apply()
    }

    /**
     * Get all captured messages, most recent first
     */
    fun getMessages(context: Context): List<CapturedMessage> {
        val prefs = getPrefs(context)
        val messagesJson = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val jsonArray = try {
            JSONArray(messagesJson)
        } catch (e: Exception) {
            return emptyList()
        }

        val messages = mutableListOf<CapturedMessage>()
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                messages.add(
                    CapturedMessage(
                        id = obj.optString("id", ""),
                        sender = obj.optString("sender", "Unknown"),
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", 0),
                        packageName = obj.optString("packageName", "com.whatsapp"),
                        isGroup = obj.optBoolean("isGroup", false),
                        groupName = obj.optString("groupName", "").ifBlank { null }
                    )
                )
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }

        return messages.sortedByDescending { it.timestamp }
    }

    /**
     * Search messages
     */
    fun searchMessages(context: Context, query: String): List<CapturedMessage> {
        if (query.isBlank()) return getMessages(context)
        val lowerQuery = query.lowercase()
        return getMessages(context).filter {
            it.sender.lowercase().contains(lowerQuery) ||
            it.text.lowercase().contains(lowerQuery) ||
            (it.groupName?.lowercase()?.contains(lowerQuery) == true)
        }
    }

    /**
     * Delete a single message
     */
    @Synchronized
    fun deleteMessage(context: Context, messageId: String) {
        val prefs = getPrefs(context)
        val messagesJson = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val jsonArray = try {
            JSONArray(messagesJson)
        } catch (e: Exception) {
            return
        }

        val newArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.optString("id") != messageId) {
                newArray.put(obj)
            }
        }

        prefs.edit().putString(KEY_MESSAGES, newArray.toString()).apply()
    }

    /**
     * Clear all messages
     */
    fun clearAllMessages(context: Context) {
        getPrefs(context).edit().putString(KEY_MESSAGES, "[]").apply()
    }

    /**
     * Get message count
     */
    fun getMessageCount(context: Context): Int {
        val prefs = getPrefs(context)
        val messagesJson = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        return try {
            JSONArray(messagesJson).length()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Check if notification listener permission is granted
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledListeners = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) == true
    }
}
