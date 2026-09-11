package com.autotrack.app

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

class TransactionListenerService : NotificationListenerService() {

    companion object {
        // Cache to dedupe notifications within 60-second window
        private val recentNotificationHashes = ConcurrentHashMap<String, Long>()
        private const val DEDUPE_WINDOW_MS = 60_000L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure foreground watch service is running
        ForegroundWatchService.start(this)
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!DataSyncManager.isServiceEnabled()) return
        val packageName = sbn.packageName ?: return

        // 1. Check against user-editable allowlist
        val allowedPackages = DataSyncManager.getAllowedPackages()
        val isAllowed = allowedPackages.any { pkg -> packageName.contains(pkg, ignoreCase = true) }
        if (!isAllowed) return

        // 2. Extract notification text
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val fullText = "$title ${bigText ?: subText ?: ""}".trim()

        if (fullText.isBlank()) return

        // 3. Deduplicate: hash of (package + text + 1-minute bucket)
        val now = System.currentTimeMillis()
        cleanOldHashes(now)

        val dedupeKey = "$packageName:$fullText:${now / DEDUPE_WINDOW_MS}"
        if (recentNotificationHashes.containsKey(dedupeKey)) {
            return // Already processed this notification
        }
        recentNotificationHashes[dedupeKey] = now

        // 4. Parse transaction
        val parsed = TransactionParser.parse(fullText, packageName) ?: return

        // 5. Trigger Overlay Popup Window over current app
        Handler(Looper.getMainLooper()).post {
            OverlayManager.show(applicationContext, parsed)
        }
    }

    private fun cleanOldHashes(now: Long) {
        val it = recentNotificationHashes.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value > DEDUPE_WINDOW_MS) {
                it.remove()
            }
        }
    }
}
