package com.example.data.local

import android.content.Context
import com.example.data.Constants
import com.example.data.model.StoreResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class JsonCache(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
) {
    private val cacheFile: File
        get() = File(context.filesDir, Constants.STORE_CACHE_FILE)

    suspend fun saveCache(rawJson: String) = withContext(Dispatchers.IO) {
        try {
            cacheFile.writeText(rawJson)
        } catch (_: Exception) {
            // Non-fatal if writing cache fails
        }
    }

    suspend fun readCache(): StoreResponse? = withContext(Dispatchers.IO) {
        try {
            if (cacheFile.exists()) {
                val raw = cacheFile.readText()
                if (raw.isNotBlank()) {
                    val parsed = json.decodeFromString<StoreResponse>(raw)
                    val validApps = parsed.apps.filter { it.packageName.isNotBlank() && it.apkUrl.isNotBlank() }
                    return@withContext parsed.copy(apps = validApps)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private val notificationsCacheFile: File
        get() = File(context.filesDir, Constants.NOTIFICATIONS_CACHE_FILE)

    suspend fun saveNotificationsCache(rawJson: String) = withContext(Dispatchers.IO) {
        try {
            notificationsCacheFile.writeText(rawJson)
        } catch (_: Exception) {}
    }

    suspend fun readNotificationsCache(): com.example.data.model.GitHubNotificationsResponse? = withContext(Dispatchers.IO) {
        try {
            if (notificationsCacheFile.exists()) {
                val raw = notificationsCacheFile.readText()
                if (raw.isNotBlank()) {
                    val parsed = json.decodeFromString<com.example.data.model.GitHubNotificationsResponse>(raw)
                    val validList = parsed.notifications.filter {
                        it.id.isNotBlank() && it.title.isNotBlank() && it.message.isNotBlank() && it.date.isNotBlank()
                    }
                    return@withContext parsed.copy(notifications = validList)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
