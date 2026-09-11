package com.autotrack.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            ForegroundWatchService.start(context)
            if (DataSyncManager.getCashReminderEnabled()) {
                val timeParts = DataSyncManager.getCashReminderTime().split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 21
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                DailyCashReminderReceiver.schedule(context, hour, minute)
            }
        }
    }
}
