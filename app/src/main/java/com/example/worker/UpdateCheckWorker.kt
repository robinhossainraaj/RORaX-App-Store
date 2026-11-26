package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.AppNotification
import com.example.data.local.JsonCache
import com.example.data.local.SettingsStore
import com.example.data.remote.StoreApi
import com.example.domain.InstallState
import com.example.domain.InstallStateResolver
import com.example.notification.GitHubNotificationSyncManager
import com.example.notification.NotificationHelper
import com.example.notification.NotificationScheduler
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val notificationsEnabled = settingsStore.notificationsEnabledFlow.first()
        if (!notificationsEnabled) {
            return Result.success()
        }

        val notificationHelper = NotificationHelper(applicationContext)
        val scheduler = NotificationScheduler(applicationContext)
        val storeApi = StoreApi()
        val jsonCache = JsonCache(applicationContext)

        // 1. Sync GitHub scheduled notifications feed (Dubai time parsing + local alarm scheduling + catch-up)
        val syncManager = GitHubNotificationSyncManager(
            storeApi = storeApi,
            jsonCache = jsonCache,
            settingsStore = settingsStore,
            scheduler = scheduler,
            notificationHelper = notificationHelper
        )
        syncManager.syncNotifications()

        if (!notificationHelper.hasNotificationPermission()) {
            return Result.success()
        }

        // 2. Check for app store updates and new releases
        val resolver = InstallStateResolver(applicationContext)

        val storeResponse = try {
            val (raw, parsed) = storeApi.fetchStoreJson()
            jsonCache.saveCache(raw)
            parsed
        } catch (_: Exception) {
            jsonCache.readCache()
        } ?: return Result.retry()

        val isFirstRunDone = settingsStore.firstRunDoneFlow.first()
        val seenPackages = settingsStore.seenPackagesFlow.first()
        val currentPackages = storeResponse.apps.map { it.packageName }.toSet()

        if (!isFirstRunDone) {
            // First time running: do not spam notifications, record all apps as seen
            settingsStore.addSeenPackages(currentPackages)
            settingsStore.setFirstRunDone(true)
            return Result.success()
        }

        for (app in storeResponse.apps) {
            val isNewApp = !seenPackages.contains(app.packageName)
            val installState = resolver.resolve(app)
            val lastNotified = settingsStore.getLastNotifiedVersion(app.packageName)

            if (isNewApp && app.versionCode > lastNotified) {
                // New app alert
                val title = applicationContext.getString(R.string.app_name)
                val msg = "New app: ${app.name}"
                val notificationId = app.packageName.hashCode()

                notificationHelper.showAppNotification(
                    id = notificationId,
                    title = title,
                    message = msg,
                    targetPackageName = app.packageName
                )

                settingsStore.addNotification(
                    AppNotification(
                        id = "${app.packageName}_new_${app.versionCode}",
                        packageName = app.packageName,
                        title = title,
                        message = msg,
                        timestamp = System.currentTimeMillis(),
                        type = "new_app"
                    )
                )

                settingsStore.setLastNotifiedVersion(app.packageName, app.versionCode)
            } else if (installState is InstallState.UpdateAvailable && app.versionCode > lastNotified) {
                // App update alert
                val installedInfo = resolver.getInstalledPackageInfo(app.packageName)
                val installedVerName = installedInfo?.second ?: ""
                val title = applicationContext.getString(R.string.app_name)
                val msg = if (installedVerName.isNotBlank() && app.versionName.isNotBlank()) {
                    "Update available: ${app.name} $installedVerName to ${app.versionName}"
                } else {
                    "Update available: ${app.name}"
                }
                val notificationId = app.packageName.hashCode()

                notificationHelper.showAppNotification(
                    id = notificationId,
                    title = title,
                    message = msg,
                    targetPackageName = app.packageName
                )

                settingsStore.addNotification(
                    AppNotification(
                        id = "${app.packageName}_update_${app.versionCode}",
                        packageName = app.packageName,
                        title = title,
                        message = msg,
                        timestamp = System.currentTimeMillis(),
                        type = "update"
                    )
                )

                settingsStore.setLastNotifiedVersion(app.packageName, app.versionCode)
            }
        }

        settingsStore.addSeenPackages(currentPackages)
        return Result.success()
    }
}
