package com.znarfm.bing.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.znarfm.bing.BuildConfig
import com.znarfm.bing.ui.AppUiState
import com.znarfm.bing.ui.SearchState
import com.znarfm.bing.ui.SearchViewModel
import com.znarfm.bing.ui.components.ExitConfirmDialog
import com.znarfm.bing.ui.components.ExpressiveSwitch
import com.znarfm.bing.ui.components.ReminderTimePickerDialog
import com.znarfm.bing.ui.theme.AppThemeMode

private val ThemeOptionRows = listOf(
    listOf(
        AppThemeMode.SYSTEM to Icons.Rounded.BrightnessAuto,
        AppThemeMode.LIGHT to Icons.Rounded.LightMode
    ),
    listOf(
        AppThemeMode.DARK to Icons.Rounded.DarkMode,
        AppThemeMode.AMOLED to Icons.Rounded.Contrast
    )
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConfigScreen(
    uiState: AppUiState,
    viewModel: SearchViewModel,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminder(true)
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onSave = { hour, minute ->
                viewModel.updateReminderTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onConfirm = {
                showExitConfirm = false
                viewModel.cancelSearch()
                onExitApp()
            },
            onDismiss = { showExitConfirm = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Display & Behavior Card
        ConfigSectionCard(
            title = "Display & Behavior",
            icon = Icons.Rounded.Palette
        ) {
            Text(
                text = "App Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowItems.forEachIndexed { idx, (mode, icon) ->
                            val shapes = if (idx == 0) {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            } else {
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            }
                            ToggleButton(
                                checked = uiState.themeMode == mode,
                                onCheckedChange = { viewModel.setThemeMode(mode) },
                                shapes = shapes,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton }
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                                )
                                Spacer(modifier = Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = mode.label,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isAmoled = uiState.themeMode == AppThemeMode.AMOLED
            val displaySwitchCount = if (isAmoled) 2 else 1

            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                AnimatedVisibility(
                    visible = isAmoled,
                    enter = expandVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ) + fadeIn(
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                    ),
                    exit = shrinkVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                    ) + fadeOut(
                        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
                    )
                ) {
                    ConfigSwitchRow(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "Simulate screen off (AMOLED)",
                        subtitle = "Black out screen while search runs (hold 1.5s to wake)",
                        checked = uiState.amoledOverlay,
                        onCheckedChange = { viewModel.setAmoledOverlay(it) },
                        index = 0,
                        count = 2
                    )
                }

                ConfigSwitchRow(
                    icon = Icons.Rounded.ScreenLockPortrait,
                    title = "Keep screen on during search",
                    subtitle = "Prevents phone from sleeping while automating",
                    checked = uiState.keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                    index = if (isAmoled) 1 else 0,
                    count = displaySwitchCount
                )
            }
        }

        // 2. Streak & Activity Card
        ConfigSectionCard(
            title = "Streak & Activity",
            icon = Icons.Rounded.LocalFireDepartment
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${uiState.currentStreak} days",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${uiState.bestStreak} days",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Text(
                            text = "Best Streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Optimization & Automation Card
        ConfigSectionCard(
            title = "Optimization & Automation",
            icon = Icons.Rounded.Speed
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ConfigSwitchRow(
                    icon = Icons.Rounded.DataSaverOn,
                    title = "Data Saver Mode",
                    subtitle = "Blocks WebView images & media for faster searches (turn off if points fail to credit)",
                    checked = uiState.dataSaver,
                    onCheckedChange = { viewModel.setDataSaver(it) },
                    index = 0,
                    count = 2
                )

                ConfigSwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = "Completion Haptics",
                    subtitle = "Vibrate with celebratory pattern when daily searches finish",
                    checked = uiState.hapticFeedback,
                    onCheckedChange = { viewModel.setHapticFeedback(it) },
                    index = 1,
                    count = 2
                )
            }
        }

        // 4. Reminders Card
        ConfigSectionCard(
            title = "Reminders",
            icon = Icons.Rounded.NotificationsActive
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ConfigSegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = { showTimePicker = true },
                    icon = Icons.Rounded.AccessTime,
                    supportingText = "Tap time badge to customize schedule",
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = uiState.sendDailyReminder,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@ExpressiveSwitch
                                    }
                                }
                                viewModel.setDailyReminder(enabled)
                            }
                        )
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daily reminder at ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "%02d:%02d".format(uiState.reminderHour, uiState.reminderMinute),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                ConfigSwitchRow(
                    icon = Icons.Rounded.Info,
                    title = "Show login reminder on launch",
                    subtitle = "Prompt to verify Bing sign-in when app opens",
                    checked = uiState.showLoginReminderPopup,
                    onCheckedChange = { viewModel.setShowLoginReminderPopup(it) },
                    index = 1,
                    count = 2
                )
            }
        }

        // 5. App Info Card
        ConfigSectionCard(
            title = "App",
            icon = Icons.Rounded.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ConfigSegmentedItem(
                    index = 0,
                    count = 5,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/znarfm"))
                        context.startActivity(intent)
                    },
                    icon = Icons.Rounded.Code,
                    supportingText = "@znarfm",
                    supportingColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                ConfigSegmentedItem(
                    index = 1,
                    count = 5,
                    onClick = {
                        val versionText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("App Version", versionText))
                        Toast.makeText(context, "Version copied", Toast.LENGTH_SHORT).show()
                    },
                    icon = Icons.Rounded.Info,
                    supportingText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                ConfigSwitchRow(
                    icon = Icons.Rounded.Update,
                    title = "Auto-check for updates",
                    subtitle = "Check GitHub Releases automatically on app launch",
                    checked = uiState.autoCheckUpdates,
                    onCheckedChange = { viewModel.setAutoCheckUpdates(it) },
                    index = 2,
                    count = 5
                )

                ConfigSegmentedItem(
                    index = 3,
                    count = 5,
                    onClick = { viewModel.checkForUpdates(isManual = true) },
                    icon = Icons.Rounded.SystemUpdate,
                    supportingText = if (uiState.isCheckingUpdate) {
                        "Checking GitHub Releases…"
                    } else {
                        "Current: v${BuildConfig.VERSION_NAME} • Tap to check GitHub Releases"
                    },
                    trailingContent = if (uiState.isCheckingUpdate) {
                        { LoadingIndicator(modifier = Modifier.size(24.dp)) }
                    } else {
                        null
                    }
                ) {
                    Text(
                        text = "Check for Updates",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                ConfigSegmentedItem(
                    index = 4,
                    count = 5,
                    onClick = {
                        if (uiState.searchState is SearchState.InProgress) {
                            showExitConfirm = true
                        } else {
                            onExitApp()
                        }
                    },
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    iconTint = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "Exit App",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ConfigSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfigSegmentedItem(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    supportingText: String? = null,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: (@Composable () -> Unit)? = null,
    titleContent: @Composable () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                titleContent()
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = supportingColor
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailingContent()
            }
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int
) {
    ConfigSegmentedItem(
        index = index,
        count = count,
        onClick = { onCheckedChange(!checked) },
        icon = icon,
        supportingText = subtitle,
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
