package com.autotrack.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class AutoTrackApp : Application() {

    companion object {
        const val CHANNEL_ID = "auto_track_foreground_channel"
        lateinit var instance: AutoTrackApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        DataSyncManager.init(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)

            val reminderChannel = NotificationChannel(
                DailyCashReminderReceiver.CHANNEL_ID,
                "Daily Cash Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to log daily cash transactions"
            }
            manager.createNotificationChannel(reminderChannel)
        }
    }
}
