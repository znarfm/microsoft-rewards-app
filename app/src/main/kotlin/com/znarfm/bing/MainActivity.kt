package com.znarfm.bing

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.znarfm.bing.notifications.NotificationHelper
import com.znarfm.bing.ui.SearchState
import com.znarfm.bing.ui.SearchViewModel
import com.znarfm.bing.ui.screens.LoginScreen
import com.znarfm.bing.ui.screens.SearchScreen
import com.znarfm.bing.ui.theme.AppThemeMode
import com.znarfm.bing.ui.theme.MicrosoftRewardsTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SearchViewModel by viewModels()
    private var receiverRegistered = false
    private var pendingStartShortcut = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                NotificationHelper.STOP_SEARCH_ACTION -> {
                    NotificationHelper.cancelPromotedSearchProgress(context)
                    viewModel.prefs.searchCancelledFromNotification = true
                    viewModel.cancelSearch()
                }
                NotificationHelper.DISMISS_PROGRESS_ACTION -> {
                    NotificationHelper.dismissedByUser = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        registerNotificationReceiver()
        requestNotificationPermissionIfNeeded()

        if (isStartSearchShortcut(intent)) {
            pendingStartShortcut = true
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isSearching = uiState.searchState is SearchState.InProgress
            val keepScreenOnActive = isSearching && uiState.keepScreenOn
            val amoledOverlayActive = isSearching &&
                uiState.themeMode == AppThemeMode.AMOLED &&
                uiState.amoledOverlay &&
                !uiState.overlayDismissed

            LaunchedEffect(keepScreenOnActive) {
                if (keepScreenOnActive) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(amoledOverlayActive) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (amoledOverlayActive) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            LaunchedEffect(uiState.isLoggedIn, pendingStartShortcut) {
                if (pendingStartShortcut && uiState.isLoggedIn) {
                    pendingStartShortcut = false
                    viewModel.startSearch()
                }
            }

            MicrosoftRewardsTheme(themeMode = uiState.themeMode) {
                if (!uiState.isLoggedIn) {
                    LoginScreen(
                        onLoggedIn = { viewModel.setLoggedIn(true) }
                    )
                } else {
                    SearchScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onExitApp = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegroundChanged(true)
    }

    override fun onStop() {
        viewModel.onAppForegroundChanged(false)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isStartSearchShortcut(intent)) {
            if (viewModel.uiState.value.isLoggedIn) {
                viewModel.startSearch()
            } else {
                pendingStartShortcut = true
            }
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(notificationActionReceiver)
            } catch (_: Throwable) {
            }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun registerNotificationReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(NotificationHelper.STOP_SEARCH_ACTION)
            addAction(NotificationHelper.DISMISS_PROGRESS_ACTION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationActionReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun isStartSearchShortcut(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.action == "com.znarfm.bing.START_SEARCH" ||
            intent.getStringExtra("shortcut_action") == "start_search"
    }
}
