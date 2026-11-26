package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.Constants
import com.example.data.local.JsonCache
import com.example.data.local.SettingsStore
import com.example.data.remote.StoreApi
import com.example.data.repository.StoreRepository
import com.example.domain.InstallStateResolver
import com.example.install.ApkDownloader
import com.example.install.ApkInstaller
import com.example.notification.GitHubNotificationSyncManager
import com.example.notification.NotificationHelper
import com.example.notification.NotificationScheduler
import com.example.worker.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppContainer(val application: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val storeApi = StoreApi()
    val jsonCache = JsonCache(application)
    val settingsStore = SettingsStore(application)
    val resolver = InstallStateResolver(application)
    val apkDownloader = ApkDownloader(application)
    val apkInstaller = ApkInstaller(application)
    val notificationHelper = NotificationHelper(application)
    val notificationScheduler = NotificationScheduler(application)

    val gitHubNotificationSyncManager = GitHubNotificationSyncManager(
        storeApi = storeApi,
        jsonCache = jsonCache,
        settingsStore = settingsStore,
        scheduler = notificationScheduler,
        notificationHelper = notificationHelper
    )

    val storeRepository = StoreRepository(
        storeApi = storeApi,
        jsonCache = jsonCache,
        resolver = resolver,
        apkDownloader = apkDownloader,
        apkInstaller = apkInstaller,
        appScope = appScope
    )

    fun syncScheduledNotifications() {
        appScope.launch(Dispatchers.IO) {
            gitHubNotificationSyncManager.syncNotifications()
        }
    }

    fun schedulePeriodicUpdateCheck(enabled: Boolean) {
        val workManager = WorkManager.getInstance(application)
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_UPDATE_CHECK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork(Constants.WORK_UPDATE_CHECK_TAG)
        }
    }
}

class RoraxApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Initialize background update check worker based on setting
        container.appScope.launch {
            val enabled = container.settingsStore.notificationsEnabledFlow.first()
            container.schedulePeriodicUpdateCheck(enabled)
            // Initial sync of GitHub scheduled notifications on app startup
            container.syncScheduledNotifications()
        }
    }
}
