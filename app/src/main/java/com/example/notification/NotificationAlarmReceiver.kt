package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RoraxApp
import com.example.data.local.AppNotification
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_NOTIFICATION = "com.rorax.appstore.ACTION_TRIGGER_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_LINK = "extra_link"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "announcement"
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val link = intent.getStringExtra(EXTRA_LINK) ?: ""

        val app = context.applicationContext as? RoraxApp ?: return
        val container = app.container

        container.appScope.launch {
            // Check if already delivered
            val deliveredIds = container.settingsStore.getDeliveredNotificationIds()
            if (deliveredIds.contains(id)) {
                container.settingsStore.removeScheduledNotification(id)
                return@launch
            }

            // Check if user has notifications enabled
            val notificationHelper = container.notificationHelper
            val notifIntId = id.hashCode()

            notificationHelper.showAppNotification(
                id = notifIntId,
                title = title,
                message = message,
                targetPackageName = packageName,
                targetLink = link
            )

            // Mark delivered and record in notification history
            container.settingsStore.markNotificationDelivered(id)
            container.settingsStore.removeScheduledNotification(id)
            container.settingsStore.addNotification(
                AppNotification(
                    id = id,
                    packageName = packageName,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    link = link,
                    type = type
                )
            )
        }
    }
}
