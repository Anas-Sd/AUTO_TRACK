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

        // 1. Check against user-editable allowlist + financial/SMS fallback keywords
        val allowedPackages = DataSyncManager.getAllowedPackages()
        val defaultKeywords = listOf("phonepe", "paisa", "paytm", "navi", "super.money", "supermoney", "bhim", "npci", "messaging", "mms", "bank", "wallet", "gpay")
        val isAllowed = allowedPackages.any { pkg -> packageName.contains(pkg, ignoreCase = true) } ||
                        defaultKeywords.any { kw -> packageName.contains(kw, ignoreCase = true) }
        if (!isAllowed) return

        // 2. Extract ALL notification text fields comprehensively
        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val infoText = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
        val ticker = sbn.notification?.tickerText?.toString() ?: ""

        val fullText = "$title $text $bigText $subText $infoText $ticker".trim()
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

        // 5. AUTO-SAVE TRANSACTION TO SUPABASE IMMEDIATELY
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            DataSyncManager.saveTransaction(
                context = applicationContext,
                amount = parsed.amount,
                type = parsed.type,
                vendor = parsed.vendor,
                categoryId = null,
                sourceApp = parsed.sourceApp,
                note = null,
                rawNotification = parsed.rawText
            )
        }

        // 6. Show Toast & Trigger Overlay Popup Window over current app
        Handler(Looper.getMainLooper()).post {
            val toastMsg = "⚡ AutoTrack: Caught ${if (parsed.type == "income") "+" else "-"}₹${parsed.amount} (${parsed.sourceApp})"
            android.widget.Toast.makeText(applicationContext, toastMsg, android.widget.Toast.LENGTH_LONG).show()

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
