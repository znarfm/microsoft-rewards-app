package com.spin311.microsoft_automatic_rewards.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.spin311.microsoft_automatic_rewards.ui.AppUiState
import com.spin311.microsoft_automatic_rewards.ui.SearchState
import com.spin311.microsoft_automatic_rewards.ui.SearchViewModel
import com.spin311.microsoft_automatic_rewards.ui.components.ExpressiveButton
import com.spin311.microsoft_automatic_rewards.ui.components.ExpressiveChoiceChip
import com.spin311.microsoft_automatic_rewards.ui.components.ExpressiveFloatingNavBar
import com.spin311.microsoft_automatic_rewards.ui.components.ExpressiveIconButton
import com.spin311.microsoft_automatic_rewards.ui.components.NavTabItem
import com.spin311.microsoft_automatic_rewards.ui.components.WavyLinearProgressIndicator
import com.spin311.microsoft_automatic_rewards.ui.theme.AppThemeMode
import kotlin.math.roundToInt

private val PRESET_COUNTS = listOf(10, 20, 30, 50)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SearchScreen(
    uiState: AppUiState,
    viewModel: SearchViewModel,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Persistent WebView kept alive across tab switches
    val persistentWebView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.blockNetworkImage = uiState.dataSaver
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    CookieManager.getInstance().flush()
                }
            }
            loadUrl("https://www.bing.com")
        }
    }

    LaunchedEffect(uiState.dataSaver) {
        persistentWebView.settings.blockNetworkImage = uiState.dataSaver
    }

    LaunchedEffect(viewModel) {
        viewModel.webViewUrlCommands.collect { url ->
            persistentWebView.loadUrl(url)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            persistentWebView.destroy()
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (uiState.showLoginReminderPopup && !uiState.loginReminderShownThisSession) {
        var dontShowAgain by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { viewModel.markLoginReminderShown(dontShowAgain) },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Verify Bing Sign-In",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Before starting your automated searches, please verify that your Microsoft account is signed in inside the Bing browser below so your points are credited."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontShowAgain = !dontShowAgain }
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Don't show this reminder again",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.markLoginReminderShown(dontShowAgain) }
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val isSearching = uiState.searchState is SearchState.InProgress
    val showAmoledOverlay = isSearching &&
        uiState.themeMode == AppThemeMode.AMOLED &&
        uiState.amoledOverlay &&
        !uiState.overlayDismissed

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 84.dp)
                )
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Reward Search Automator",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            StatusSubtitle(uiState = uiState)
                        }
                    },
                    actions = {
                        StreakBadge(
                            currentStreak = uiState.currentStreak,
                            bestStreak = uiState.bestStreak
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ExpressiveIconButton(
                            icon = Icons.Rounded.EmojiEvents,
                            contentDescription = "Open Rewards Dashboard",
                            onClick = { viewModel.openRewardsDashboard() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.selectedTab == 0) {
                    SearchTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        persistentWebView = persistentWebView
                    )
                } else {
                    ConfigScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onExitApp = onExitApp
                    )
                }

                // Floating Pill Navigation Bar at bottom center
                ExpressiveFloatingNavBar(
                    items = listOf(
                        NavTabItem(
                            label = "Search",
                            selectedIcon = Icons.Rounded.Search,
                            unselectedIcon = Icons.Outlined.Search
                        ),
                        NavTabItem(
                            label = "Config",
                            selectedIcon = Icons.Rounded.Settings,
                            unselectedIcon = Icons.Outlined.Settings
                        )
                    ),
                    selectedIndex = uiState.selectedTab,
                    onSelect = { viewModel.selectTab(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                )
            }
        }

        if (showAmoledOverlay) {
            AmoledOverlay(
                onDismiss = { viewModel.dismissAmoledOverlay() }
            )
        }
    }
}

@Composable
private fun StatusSubtitle(uiState: AppUiState) {
    if (uiState.selectedTab == 1) {
        Text(
            text = "Theme, automation & reminders",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val searchState = uiState.searchState
    val (dotColor, statusText) = when {
        searchState is SearchState.InProgress -> {
            val text = if (searchState.remainingSeconds > 0) {
                "${searchState.current}/${searchState.total} • Next in ${searchState.remainingSeconds}s"
            } else {
                "Searching (${searchState.current}/${searchState.total})…"
            }
            MaterialTheme.colorScheme.primary to text
        }
        uiState.isCompletedToday -> Color(0xFF2E7D32) to "Completed today"
        else -> Color(0xFFEF6C00) to "Pending today"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakBadge(
    currentStreak: Int,
    bestStreak: Int
) {
    val label = when {
        currentStreak > 0 -> "🔥 $currentStreak"
        bestStreak > 0 -> "🏆 Best $bestStreak"
        else -> "🔥 0"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SearchTabContent(
    uiState: AppUiState,
    viewModel: SearchViewModel,
    persistentWebView: WebView
) {
    val isSearching = uiState.searchState is SearchState.InProgress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Preset Count Chips (10, 20, 30, 50)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentCount = uiState.searchCountText.trim().toIntOrNull()
                    PRESET_COUNTS.forEach { preset ->
                        ExpressiveChoiceChip(
                            label = "$preset",
                            selected = currentCount == preset,
                            enabled = !isSearching,
                            onClick = { viewModel.updateSearchCount(preset.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Search Count & Delay inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchCountText,
                        onValueChange = { viewModel.updateSearchCount(it) },
                        enabled = !isSearching,
                        label = { Text("Search Count") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                        isError = uiState.countError != null,
                        supportingText = uiState.countError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.searchDelayText,
                        onValueChange = { viewModel.updateSearchDelay(it) },
                        enabled = !isSearching,
                        label = { Text("Delay (s)") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Timer, contentDescription = null)
                        },
                        isError = uiState.delayError != null,
                        supportingText = uiState.delayError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Active Search Progress Card
                AnimatedVisibility(
                    visible = isSearching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val progressState = uiState.searchState as? SearchState.InProgress
                    if (progressState != null) {
                        val frac = if (progressState.total > 0) {
                            progressState.current.toFloat() / progressState.total.toFloat()
                        } else {
                            0f
                        }
                        val percent = (frac * 100).roundToInt().coerceIn(0, 100)
                        val delaySec = uiState.searchDelayText.toDoubleOrNull() ?: 15.0
                        val remainingSearches = (progressState.total - progressState.current).coerceAtLeast(0)
                        val estTotalSeconds = (remainingSearches * (delaySec + 0.5)).roundToInt() +
                            progressState.remainingSeconds
                        val estMin = estTotalSeconds / 60
                        val estSec = estTotalSeconds % 60
                        val estFormatted = if (estMin > 0) "~${estMin}m ${estSec}s left" else "~${estSec}s left"

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (progressState.remainingSeconds > 0) {
                                            "Next search in ${progressState.remainingSeconds}s…"
                                        } else {
                                            "Searching…"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(
                                            text = "$percent%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                WavyLinearProgressIndicator(progress = frac)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${progressState.current}/${progressState.total} completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = estFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Dual-morphing Start / Cancel Button
                ExpressiveButton(
                    text = if (isSearching) "Cancel Search" else "Start Search",
                    icon = if (isSearching) Icons.Rounded.Cancel else Icons.Rounded.PlayArrow,
                    isActive = isSearching,
                    isDestructive = isSearching,
                    onClick = {
                        if (isSearching) {
                            viewModel.cancelSearch()
                        } else {
                            viewModel.startSearch()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Embedded Bing WebView Container
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 86.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    (persistentWebView.parent as? ViewGroup)?.removeView(persistentWebView)
                    persistentWebView
                }
            )
        }
    }
}
