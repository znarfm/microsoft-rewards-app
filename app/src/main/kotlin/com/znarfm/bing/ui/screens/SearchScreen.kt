package com.znarfm.bing.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.znarfm.bing.ui.AppUiState
import com.znarfm.bing.ui.SearchState
import com.znarfm.bing.ui.SearchViewModel
import com.znarfm.bing.ui.components.ExpressiveWavyProgress
import com.znarfm.bing.ui.components.LoginReminderDialog
import com.znarfm.bing.ui.components.UpdateAvailableDialog
import com.znarfm.bing.ui.theme.AppThemeMode
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SearchScreen(
    uiState: AppUiState,
    viewModel: SearchViewModel,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var canGoBackInWebView by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    // Persistent WebView kept alive across tab switches
    val persistentWebView = remember {
        var mainFrameFailed = false
        var mainFrameErrorDesc: String? = null

        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.userAgentString = SearchViewModel.sanitizeUserAgent(settings.userAgentString)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.blockNetworkImage = uiState.dataSaver
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    canGoBackInWebView = view?.canGoBack() == true
                    CookieManager.getInstance().flush()

                    if (mainFrameFailed) {
                        viewModel.onPageResult(url, success = false, error = mainFrameErrorDesc)
                    } else {
                        viewModel.onPageResult(url, success = true, error = null)
                    }
                    // Reset flags for subsequent loads
                    mainFrameFailed = false
                    mainFrameErrorDesc = null
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        mainFrameFailed = true
                        val desc = error?.description?.toString() ?: "Network error"
                        mainFrameErrorDesc = desc
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        val statusCode = errorResponse?.statusCode ?: 200
                        if (statusCode >= 400) {
                            mainFrameFailed = true
                            mainFrameErrorDesc = "HTTP error $statusCode"
                        }
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    canGoBackInWebView = view?.canGoBack() == true
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
            persistentWebView.loadUrl(url, mapOf("Referer" to "https://www.bing.com/"))
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
        LoginReminderDialog(
            onConfirm = { dontShowAgain -> viewModel.markLoginReminderShown(dontShowAgain) }
        )
    }

    val release = uiState.availableUpdate
    if (release != null) {
        UpdateAvailableDialog(
            release = release,
            isDownloading = uiState.isDownloadingUpdate,
            downloadProgress = uiState.updateDownloadProgress,
            onConfirm = { viewModel.downloadAndInstallUpdate() },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    val isSearching = uiState.searchState is SearchState.InProgress
    val showAmoledOverlay = isSearching &&
        uiState.themeMode == AppThemeMode.AMOLED &&
        uiState.amoledOverlay &&
        !uiState.overlayDismissed

    PredictiveBackHandler(
        enabled = !showAmoledOverlay && (uiState.selectedTab == 1 || (uiState.selectedTab == 0 && canGoBackInWebView))
    ) { progress ->
        try {
            progress.collect { backEvent ->
                backProgress = backEvent.progress
            }
            val wasTab1 = uiState.selectedTab == 1
            backProgress = 0f
            if (wasTab1) {
                viewModel.selectTab(0)
            } else if (persistentWebView.canGoBack()) {
                persistentWebView.goBack()
                canGoBackInWebView = persistentWebView.canGoBack()
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            backProgress = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Bing Rewards Automator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    },
                    subtitle = {
                        StatusSubtitle(uiState = uiState)
                    },
                    actions = {
                        StreakBadge(
                            currentStreak = uiState.currentStreak,
                            bestStreak = uiState.bestStreak
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { viewModel.openRewardsDashboard() },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Open Rewards Dashboard"
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                )
            },
            bottomBar = {
                val searchSelected = uiState.selectedTab == 0
                val configSelected = uiState.selectedTab == 1
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = searchSelected,
                        onClick = { if (!searchSelected) viewModel.selectTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (searchSelected) Icons.Rounded.Search else Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        label = { Text("Search") }
                    )
                    ShortNavigationBarItem(
                        selected = configSelected,
                        onClick = { if (!configSelected) viewModel.selectTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (configSelected) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text("Config") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                if (uiState.selectedTab == 0) {
                    SearchTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        persistentWebView = persistentWebView,
                        backProgress = backProgress
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1f - (backProgress * 0.06f)
                                scaleY = 1f - (backProgress * 0.06f)
                                alpha = 1f - (backProgress * 0.25f)
                            }
                    ) {
                        ConfigScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onExitApp = onExitApp
                        )
                    }
                }
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
        uiState.isCompletedToday -> MaterialTheme.colorScheme.primary to "Completed today"
        else -> MaterialTheme.colorScheme.tertiary to "Pending today"
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
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
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
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchTabContent(
    uiState: AppUiState,
    viewModel: SearchViewModel,
    persistentWebView: WebView,
    backProgress: Float = 0f
) {
    val isSearching = uiState.searchState is SearchState.InProgress
    val bottomCardPadding = 12.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Embedded Bing WebView Container (Top)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp)
                .graphicsLayer {
                    if (backProgress > 0f) {
                        scaleX = 1f - (backProgress * 0.06f)
                        scaleY = 1f - (backProgress * 0.06f)
                        alpha = 1f - (backProgress * 0.25f)
                    }
                },
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

        // Search Controls Card (Bottom)
        Card(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(bottom = bottomCardPadding),
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
                    enter = expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = shrinkVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) +
                        fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    val progressState = uiState.searchState as? SearchState.InProgress
                    if (progressState != null) {
                        val frac = progressState.fraction
                        val percent = progressState.percent
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (progressState.remainingSeconds == 0) {
                                            LoadingIndicator(modifier = Modifier.size(18.dp))
                                        }
                                        Text(
                                            text = if (progressState.remainingSeconds > 0) {
                                                "Next search in ${progressState.remainingSeconds}s…"
                                            } else {
                                                "Searching…"
                                            },
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(
                                            text = "$percent%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                ExpressiveWavyProgress(
                                    progress = frac,
                                    indeterminate = progressState.current == 0,
                                    contentDescription = "Search progress: ${progressState.current} of ${progressState.total}",
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${progressState.current}/${progressState.total} completed",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = estFormatted,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Start / Cancel Button
                Button(
                    onClick = {
                        if (isSearching) {
                            viewModel.cancelSearch()
                        } else {
                            viewModel.startSearch()
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = if (isSearching) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = if (isSearching) Icons.Rounded.Cancel else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = if (isSearching) "Cancel Search" else "Start Search",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
