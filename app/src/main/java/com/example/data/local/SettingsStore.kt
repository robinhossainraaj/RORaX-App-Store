package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ScheduledNotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "rorax_settings")

@Serializable
data class AppNotification(
    val id: String,
    val packageName: String = "",
    val title: String,
    val message: String,
    val timestamp: Long,
    val link: String = "",
    val type: String = "announcement",
    val isRead: Boolean = false
)

class SettingsStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_SEEN_PACKAGES = stringSetPreferencesKey("seen_packages")
        val KEY_NOTIFICATION_HISTORY = stringPreferencesKey("notification_history")
        val KEY_FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
        val KEY_DELIVERED_NOTIFICATION_IDS = stringSetPreferencesKey("delivered_notification_ids")
        val KEY_SCHEDULED_NOTIFICATIONS = stringPreferencesKey("scheduled_notifications_json")

        fun lastNotifiedKey(packageName: String) = longPreferencesKey("last_notified_$packageName")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "dark"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val firstRunDoneFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_RUN_DONE] ?: false
    }

    suspend fun setFirstRunDone(done: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_RUN_DONE] = done
        }
    }

    val seenPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SEEN_PACKAGES] ?: emptySet()
    }

    suspend fun addSeenPackages(packages: Collection<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SEEN_PACKAGES] ?: emptySet()
            preferences[KEY_SEEN_PACKAGES] = current + packages
        }
    }

    suspend fun getLastNotifiedVersion(packageName: String): Long {
        val prefs = context.dataStore.data.first()
        return prefs[lastNotifiedKey(packageName)] ?: 0L
    }

    suspend fun setLastNotifiedVersion(packageName: String, versionCode: Long) {
        context.dataStore.edit { preferences ->
            preferences[lastNotifiedKey(packageName)] = versionCode
        }
    }

    val notificationHistoryFlow: Flow<List<AppNotification>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_NOTIFICATION_HISTORY] ?: "[]"
        try {
            json.decodeFromString<List<AppNotification>>(raw).sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val unreadNotificationsCountFlow: Flow<Int> = notificationHistoryFlow.map { list ->
        list.count { !it.isRead }
    }

    suspend fun addNotification(notification: AppNotification) {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_NOTIFICATION_HISTORY] ?: "[]"
            val current = try {
                json.decodeFromString<List<AppNotification>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            // Avoid duplicate entries in history
            current.removeAll { it.id == notification.id }
            current.add(0, notification)
            // Cap at 50 entries as per spec
            val trimmed = current.take(50)
            preferences[KEY_NOTIFICATION_HISTORY] = json.encodeToString(trimmed)
        }
    }

    suspend fun markAllNotificationsAsRead() {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_NOTIFICATION_HISTORY] ?: "[]"
            val current = try {
                json.decodeFromString<List<AppNotification>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
            val updated = current.map { it.copy(isRead = true) }
            preferences[KEY_NOTIFICATION_HISTORY] = json.encodeToString(updated)
        }
    }

    // Scheduled & Delivered notifications tracking
    suspend fun getDeliveredNotificationIds(): Set<String> {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_DELIVERED_NOTIFICATION_IDS] ?: emptySet()
    }

    suspend fun markNotificationDelivered(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_DELIVERED_NOTIFICATION_IDS] ?: emptySet()
            preferences[KEY_DELIVERED_NOTIFICATION_IDS] = current + id
        }
    }

    suspend fun getScheduledNotifications(): List<ScheduledNotificationEntity> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_SCHEDULED_NOTIFICATIONS] ?: "[]"
        return try {
            json.decodeFromString<List<ScheduledNotificationEntity>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveScheduledNotifications(list: List<ScheduledNotificationEntity>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCHEDULED_NOTIFICATIONS] = json.encodeToString(list)
        }
    }

    suspend fun removeScheduledNotification(id: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_SCHEDULED_NOTIFICATIONS] ?: "[]"
            val current = try {
                json.decodeFromString<List<ScheduledNotificationEntity>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            current.removeAll { it.id == id }
            preferences[KEY_SCHEDULED_NOTIFICATIONS] = json.encodeToString(current)
        }
    }
}
