package com.spin311.microsoft_automatic_rewards.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
                        holdProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = HOLD_TO_WAKE_MS,
                                easing = LinearEasing
                            )
                        )
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
            val indicatorSize = 110.dp
            val halfPx = with(density) { (indicatorSize / 2).toPx() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (pos.x - halfPx).roundToInt(),
                            y = (pos.y - halfPx).roundToInt()
                        )
                    }
                    .size(indicatorSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.85f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
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
