package com.example.data.remote

import com.example.data.Constants
import com.example.data.model.StoreResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class StoreApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun fetchStoreJson(): Pair<String, StoreResponse> = withContext(Dispatchers.IO) {
        val cacheBustingUrl = "${Constants.JSON_URL}?t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(cacheBustingUrl)
            .addHeader("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP error code: ${response.code}")
        }

        val rawBody = response.body?.string() ?: throw IOException("Empty response body from store API")
        val parsed = json.decodeFromString<StoreResponse>(rawBody)
        // Filter out apps with blank package or apkUrl as per spec
        val validApps = parsed.apps.filter { it.packageName.isNotBlank() && it.apkUrl.isNotBlank() }
        rawBody to parsed.copy(apps = validApps)
    }

    suspend fun fetchGitHubNotifications(): Pair<String, com.example.data.model.GitHubNotificationsResponse> = withContext(Dispatchers.IO) {
        val cacheBustingUrl = "${Constants.GITHUB_NOTIFICATIONS_URL}?t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(cacheBustingUrl)
            .addHeader("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP error code: ${response.code}")
        }

        val rawBody = response.body?.string() ?: throw IOException("Empty response body from notifications API")
        val parsed = json.decodeFromString<com.example.data.model.GitHubNotificationsResponse>(rawBody)
        // Filter valid entries (id, title, message, date must be present; time is optional)
        val validList = parsed.notifications.filter {
            it.id.isNotBlank() && it.title.isNotBlank() && it.message.isNotBlank() && it.date.isNotBlank()
        }
        rawBody to parsed.copy(notifications = validList)
    }
}
