package com.znarfm.bing.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.znarfm.bing.ui.components.ExpressiveSwitch
import com.znarfm.bing.ui.theme.AppThemeMode

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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
        val pickerState = rememberTimePickerState(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Daily Reminder Time",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateReminderTime(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Exit App?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("A search session is currently running. Exiting will stop the ongoing automation.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        viewModel.cancelSearch()
                        onExitApp()
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = "Exit",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

            val themeOptions = listOf(
                AppThemeMode.SYSTEM to Icons.Rounded.BrightnessAuto,
                AppThemeMode.LIGHT to Icons.Rounded.LightMode,
                AppThemeMode.DARK to Icons.Rounded.DarkMode,
                AppThemeMode.AMOLED to Icons.Rounded.Contrast
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themeOptions.chunked(2).forEach { rowItems ->
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
                SegmentedListItem(
                    onClick = { showTimePicker = true },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Tap time badge to customize schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
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
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.clickable { showTimePicker = true }
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
                SegmentedListItem(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/znarfm"))
                        context.startActivity(intent)
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 5),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "@znarfm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 5),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                ConfigSwitchRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Auto-check for updates",
                    subtitle = "Check GitHub Releases automatically on app launch",
                    checked = uiState.autoCheckUpdates,
                    onCheckedChange = { viewModel.setAutoCheckUpdates(it) },
                    index = 2,
                    count = 5
                )

                SegmentedListItem(
                    onClick = { viewModel.checkForUpdates(isManual = true) },
                    shapes = ListItemDefaults.segmentedShapes(index = 3, count = 5),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (uiState.isCheckingUpdate) {
                                "Checking GitHub Releases…"
                            } else {
                                "Current: v${BuildConfig.VERSION_NAME} • Tap to check GitHub Releases"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                SegmentedListItem(
                    onClick = {
                        if (uiState.searchState is SearchState.InProgress) {
                            showExitConfirm = true
                        } else {
                            onExitApp()
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 4, count = 5),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
        modifier = Modifier.fillMaxWidth(),
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
private fun ConfigSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
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
