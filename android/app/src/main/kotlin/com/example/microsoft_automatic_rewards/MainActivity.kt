package com.spin311.microsoft_automatic_rewards

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "com.spin311.microsoft_automatic_rewards/shortcuts"
    private var initialShortcutAction: String? = null
    private var shortcutChannel: MethodChannel? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        handleIntent(intent)

        shortcutChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).apply {
            setMethodCallHandler { call, result ->
                if (call.method == "getInitialShortcut") {
                    val action = initialShortcutAction
                    initialShortcutAction = null
                    result.success(action)
                } else {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = extractShortcutAction(intent)
        if (action != null) {
            shortcutChannel?.invokeMethod("onShortcutTriggered", action)
        }
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
