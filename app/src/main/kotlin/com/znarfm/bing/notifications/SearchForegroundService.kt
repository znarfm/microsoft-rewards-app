package com.znarfm.bing.notifications

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.ContextCompat

class SearchForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OR_UPDATE -> {
                acquireWakeLockIfNeeded()
                val current = intent.getIntExtra(EXTRA_CURRENT, 0)
                val total = intent.getIntExtra(EXTRA_TOTAL, 0)
                val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0)
                val precomputedPercent = intent.getIntExtra(EXTRA_PERCENT, -1)
                val percentArg = if (precomputedPercent >= 0) precomputedPercent else null

                val notification = NotificationHelper.buildPromotedSearchProgressNotification(
                    this,
                    current,
                    total,
                    remainingSeconds,
                    percentArg
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NotificationHelper.PROGRESS_NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
                    }
                } catch (_: Throwable) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NotificationHelper.PROGRESS_NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                releaseWakeLock()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MicrosoftRewards:SearchWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        }
        wakeLock?.let {
            if (!it.isHeld) {
                try {
                    it.acquire(30 * 60 * 1000L) // 30 minutes safety timeout
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_OR_UPDATE = "com.znarfm.bing.FGS_START_OR_UPDATE"
        const val ACTION_STOP = "com.znarfm.bing.FGS_STOP"

        private const val EXTRA_CURRENT = "extra_current"
        private const val EXTRA_TOTAL = "extra_total"
        private const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
        private const val EXTRA_PERCENT = "extra_percent"

        fun startOrUpdate(
            context: Context,
            current: Int,
            total: Int,
            remainingSeconds: Int,
            precomputedPercent: Int? = null
        ) {
            val intent = Intent(context, SearchForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_CURRENT, current)
                putExtra(EXTRA_TOTAL, total)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_PERCENT, precomputedPercent ?: -1)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SearchForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Throwable) {
            }
        }
    }
}
