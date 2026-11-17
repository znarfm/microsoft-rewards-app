package com.znarfm.bing.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveWavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    if (indeterminate) {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    } else {
        val coerced = progress.coerceIn(0f, 1f)
        LinearWavyProgressIndicator(
            progress = { coerced },
            modifier = modifier,
            color = color,
            trackColor = trackColor
        )
    }
}
