package com.znarfm.bing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
    val target = if (indeterminate) 0f else progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "wavyProgress"
    )
    if (indeterminate) {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            amplitude = 1f,
            wavelength = 20.dp,
            waveSpeed = 20.dp
        )
    } else {
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            amplitude = { 1f },
            wavelength = 20.dp,
            waveSpeed = 20.dp
        )
    }
}
