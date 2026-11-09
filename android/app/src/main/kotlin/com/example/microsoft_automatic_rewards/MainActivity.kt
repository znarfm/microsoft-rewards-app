package com.spin311.microsoft_automatic_rewards

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "com.spin311.microsoft_automatic_rewards/shortcuts"
    private val stopSearchAction = "com.spin311.microsoft_automatic_rewards.STOP_SEARCH"
    private val dismissNotificationAction = "com.spin311.microsoft_automatic_rewards.DISMISS_PROGRESS"
    private val progressChannelId = "search_progress_channel"
    private val progressNotificationId = 2

    private var initialShortcutAction: String? = null
    private var shortcutChannel: MethodChannel? = null
    private var stopReceiverRegistered = false
    private var dismissedByUser = false

    private val stopSearchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                stopSearchAction -> {
                    cancelPromotedNotification()
                    val prefs = context.getSharedPreferences(
                        "FlutterSharedPreferences",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit().putBoolean("flutter.search_cancelled_from_notification", true).apply()
                    shortcutChannel?.invokeMethod("onShortcutTriggered", "stop_search")
                }
                dismissNotificationAction -> {
                    dismissedByUser = true
                }
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        handleIntent(intent)
        registerStopReceiver()

        shortcutChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "getInitialShortcut" -> {
                        val action = initialShortcutAction
                        initialShortcutAction = null
                        result.success(action)
                    }
                    "showPromotedSearchProgress" -> {
                        val current = call.argument<Int>("current") ?: 0
                        val total = call.argument<Int>("total") ?: 1
                        val remainingSeconds = call.argument<Int>("remainingSeconds") ?: 0
                        showPromotedNotification(current, total, remainingSeconds)
                        result.success(true)
                    }
                    "cancelPromotedSearchProgress" -> {
                        cancelPromotedNotification()
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    private fun registerStopReceiver() {
        if (stopReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(stopSearchAction)
            addAction(dismissNotificationAction)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopSearchReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopSearchReceiver, filter)
        }
        stopReceiverRegistered = true
    }

    private fun showPromotedNotification(current: Int, total: Int, remainingSeconds: Int) {
        if (current <= 1 && remainingSeconds == 0) {
            dismissedByUser = false
        }
        if (dismissedByUser) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                progressChannelId,
                "Search Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ongoing notification while search is running"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val percent = if (total > 0) ((current.toDouble() / total.toDouble()) * 100).toInt() else 0
        val bodyText = if (remainingSeconds > 0) {
            "$current/$total completed ($percent%) • Next in ${remainingSeconds}s"
        } else {
            "$current/$total completed ($percent%) • Searching…"
        }
        // Status bar chip text (< 7 chars required by Android 16 to display full text in chip)
        val chipText = "$current/$total"

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(stopSearchAction).apply {
            setPackage(packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(dismissNotificationAction).apply {
            setPackage(packageName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, progressChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val stopAction = Notification.Action.Builder(
            null,
            "Stop",
            stopPendingIntent
        ).build()

        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bing Search Automation")
            .setContentText(bodyText)
            .setSubText("$percent%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(stopAction)
            .setProgress(total.coerceAtLeast(1), current.coerceAtLeast(0), false)

        // Request Android 16 (API 36+) Promoted Ongoing / Live Update treatment
        val extras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putCharSequence("android.shortCriticalText", chipText)
        }
        builder.addExtras(extras)

        if (Build.VERSION.SDK_INT >= 36) {
            applyAndroid16LiveUpdateFeatures(builder, chipText, percent)
        }

        manager.notify(progressNotificationId, builder.build())
    }

    private fun applyAndroid16LiveUpdateFeatures(
        builder: Notification.Builder,
        chipText: String,
        percent: Int
    ) {
        try {
            // Notification.Builder#setRequestPromotedOngoing(boolean)
            builder.javaClass
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        } catch (_: Throwable) {}

        try {
            // Notification.Builder#setShortCriticalText(CharSequence)
            builder.javaClass
                .getMethod("setShortCriticalText", CharSequence::class.java)
                .invoke(builder, chipText)
        } catch (_: Throwable) {}

        try {
            // Notification.ProgressStyle for Android 16 Live Updates
            val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
            progressStyleClass
                .getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
                .invoke(progressStyle, true)
            progressStyleClass
                .getMethod("setProgress", Int::class.javaPrimitiveType)
                .invoke(progressStyle, percent.coerceIn(0, 100))
            progressStyleClass
                .getMethod("setProgressIndeterminate", Boolean::class.javaPrimitiveType)
                .invoke(progressStyle, false)

            val styleClass = Class.forName("android.app.Notification\$Style")
            builder.javaClass
                .getMethod("setStyle", styleClass)
                .invoke(builder, progressStyle)
        } catch (_: Throwable) {}
    }

    private fun cancelPromotedNotification() {
        dismissedByUser = false
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(progressNotificationId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = extractShortcutAction(intent)
        if (action != null) {
            shortcutChannel?.invokeMethod("onShortcutTriggered", action)
        }
    }

    override fun onDestroy() {
        if (stopReceiverRegistered) {
            try {
                unregisterReceiver(stopSearchReceiver)
            } catch (_: Throwable) {}
            stopReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        initialShortcutAction = extractShortcutAction(intent)
    }

    private fun extractShortcutAction(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == "com.spin311.microsoft_automatic_rewards.START_SEARCH" ||
            intent.getStringExtra("shortcut_action") == "start_search") {
            return "start_search"
        }
        return null
    }
}
