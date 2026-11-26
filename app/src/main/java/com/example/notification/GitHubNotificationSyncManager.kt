package com.example.notification

import com.example.data.Constants
import com.example.data.local.AppNotification
import com.example.data.local.JsonCache
import com.example.data.local.SettingsStore
import com.example.data.model.GitHubNotificationItem
import com.example.data.model.ScheduledNotificationEntity
import com.example.data.remote.StoreApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GitHubNotificationSyncManager(
    private val storeApi: StoreApi,
    private val jsonCache: JsonCache,
    private val settingsStore: SettingsStore,
    private val scheduler: NotificationScheduler,
    private val notificationHelper: NotificationHelper
) {

    suspend fun syncNotifications(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val notificationsEnabled = settingsStore.notificationsEnabledFlow.first()
            if (!notificationsEnabled) {
                // If user toggled notifications off, cancel any pending alarms
                val scheduled = settingsStore.getScheduledNotifications()
                scheduled.forEach { scheduler.cancel(it.id) }
                settingsStore.saveScheduledNotifications(emptyList())
                return@withContext Result.success(Unit)
            }

            // Fetch from GitHub; fall back to local cache if network is unavailable
            val response = try {
                val (raw, parsed) = storeApi.fetchGitHubNotifications()
                jsonCache.saveNotificationsCache(raw)
                parsed
            } catch (_: Exception) {
                jsonCache.readNotificationsCache()
            } ?: return@withContext Result.failure(Exception("Unable to fetch or read notifications feed"))

            val deliveredIds = settingsStore.getDeliveredNotificationIds()
            val currentScheduledMap = settingsStore.getScheduledNotifications().associateBy { it.id }
            val now = System.currentTimeMillis()

            val activeScheduledList = mutableListOf<ScheduledNotificationEntity>()
            val incomingFeedIds = mutableSetOf<String>()

            for (item in response.notifications) {
                if (item.id.isBlank() || item.title.isBlank() || item.message.isBlank()) {
                    continue
                }
                incomingFeedIds.add(item.id)

                val triggerTime = item.calculateTriggerEpochMillis() ?: now

                // Always record in-app notification center history so the Notifications screen displays it
                settingsStore.addNotification(
                    AppNotification(
                        id = item.id,
                        packageName = item.`package`,
                        title = item.title,
                        message = item.message,
                        timestamp = triggerTime,
                        link = item.link,
                        type = item.type
                    )
                )

                // Skip system tray alarm scheduling if already delivered/fired
                if (deliveredIds.contains(item.id)) {
                    continue
                }

                if (triggerTime > now) {
                    // Future notification: schedule alarm for exact local delivery
                    val entity = ScheduledNotificationEntity(
                        id = item.id,
                        title = item.title,
                        message = item.message,
                        type = item.type,
                        packageName = item.`package`,
                        link = item.link,
                        triggerEpochMillis = triggerTime,
                        rawDate = item.date,
                        rawTime = item.time
                    )

                    val existing = currentScheduledMap[item.id]
                    if (existing != null) {
                        // If date/time or content changed, cancel and reschedule
                        if (existing.triggerEpochMillis != triggerTime ||
                            existing.title != item.title ||
                            existing.message != item.message
                        ) {
                            scheduler.cancel(item.id)
                            scheduler.schedule(entity)
                        }
                    } else {
                        scheduler.schedule(entity)
                    }

                    activeScheduledList.add(entity)
                } else {
                    // Past/Overdue notification: apply catch-up window policy
                    val isWithinCatchUpWindow = (now - triggerTime) <= Constants.CATCH_UP_WINDOW_MILLIS
                    if (isWithinCatchUpWindow) {
                        // Show delayed notification once immediately in system status bar
                        val notifIntId = item.id.hashCode()
                        notificationHelper.showAppNotification(
                            id = notifIntId,
                            title = item.title,
                            message = item.message,
                            targetPackageName = item.`package`,
                            targetLink = item.link
                        )

                        settingsStore.markNotificationDelivered(item.id)
                    } else {
                        // Older than catch-up window: quietly mark delivered so it never shows system popup
                        settingsStore.markNotificationDelivered(item.id)
                    }
                }
            }

            // Cancel any previously scheduled alarms whose entries were removed from the JSON feed
            for ((oldId, _) in currentScheduledMap) {
                if (!incomingFeedIds.contains(oldId)) {
                    scheduler.cancel(oldId)
                }
            }

            settingsStore.saveScheduledNotifications(activeScheduledList)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
