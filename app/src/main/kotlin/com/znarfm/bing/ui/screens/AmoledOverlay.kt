package com.znarfm.bing.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val HOLD_TO_WAKE_MS = 1500
private const val DOUBLE_BACK_WINDOW_MS = 2000L

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AmoledOverlay(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    val holdProgress = remember { Animatable(0f) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val isHolding = touchPosition != null && holdProgress.value > 0f
    val indicatorScale by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0.8f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "holdIndicatorScale"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "holdIndicatorAlpha"
    )

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime <= DOUBLE_BACK_WINDOW_MS) {
            onDismiss()
        } else {
            lastBackPressTime = now
            Toast.makeText(
                context,
                "Press back again or hold screen for 1.5s to wake",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    touchPosition = down.position
                    val animJob = scope.launch {
                        holdProgress.snapTo(0f)
                        val startTimeMs = withFrameMillis { it }
                        while (true) {
                            val elapsed = withFrameMillis { it } - startTimeMs
                            val fraction = (elapsed.toFloat() / HOLD_TO_WAKE_MS).coerceIn(0f, 1f)
                            holdProgress.snapTo(fraction)
                            if (fraction >= 1f) break
                        }
                        onDismiss()
                    }
                    waitForUpOrCancellation()
                    if (animJob.isActive) {
                        animJob.cancel()
                    }
                    touchPosition = null
                    scope.launch {
                        holdProgress.snapTo(0f)
                    }
                }
            }
    ) {
        val pos = touchPosition
        val progress = holdProgress.value
        if (pos != null && progress > 0f) {
            val backdropSize = 132.dp
            val ringSize = 104.dp
            val halfPx = with(density) { (backdropSize / 2).toPx() }
            val burstShape = MaterialShapes.SoftBurst.toShape()

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (pos.x - halfPx).roundToInt(),
                            y = (pos.y - halfPx).roundToInt()
                        )
                    }
                    .size(backdropSize)
                    .graphicsLayer {
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                        alpha = indicatorAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = burstShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = burstShape
                        )
                )
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(ringSize),
                    color = Color.White.copy(alpha = 0.85f),
                    trackColor = Color.White.copy(alpha = 0.16f),
                    amplitude = { 1f }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
