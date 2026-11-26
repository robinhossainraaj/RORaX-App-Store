package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RoraxApp
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val app = context.applicationContext as? RoraxApp ?: return
            val container = app.container

            container.appScope.launch {
                val scheduled = container.settingsStore.getScheduledNotifications()
                val delivered = container.settingsStore.getDeliveredNotificationIds()
                val now = System.currentTimeMillis()

                scheduled.forEach { item ->
                    if (!delivered.contains(item.id) && item.triggerEpochMillis > now) {
                        container.notificationScheduler.schedule(item)
                    }
                }
            }
        }
    }
}
