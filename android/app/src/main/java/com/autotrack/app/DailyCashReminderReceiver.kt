package com.autotrack.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class DailyCashReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "daily_cash_reminder_channel"
        const val NOTIFICATION_ID = 2026

        fun schedule(context: Context, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyCashReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: Exception) {
                // Fallback for strict alarm permissions
            }
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DailyCashReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!DataSyncManager.isServiceEnabled()) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel if Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Cash Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to log daily cash transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action 1: Log Cash Transaction (Opens MainActivity with manual log trigger)
        val logIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_LOG_CASH"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val logPendingIntent = PendingIntent.getActivity(
            context,
            1,
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: No Cash Spent Today (Dismisses notification)
        val noCashIntent = Intent(context, DailyCashReminderReceiver::class.java).apply {
            action = "ACTION_NO_CASH"
        }
        val noCashPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            noCashIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (intent.action == "ACTION_NO_CASH") {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val vaultCode = DataSyncManager.getVaultCode() ?: ""
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💵 Daily Cash Check")
            .setContentText("Did you spend any cash today? Tap to log or confirm.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Log Cash Spent", logPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "No Cash Today", noCashPendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
