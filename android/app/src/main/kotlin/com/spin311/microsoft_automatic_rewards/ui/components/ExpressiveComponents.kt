package com.spin311.microsoft_automatic_rewards.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

object ExpressiveMotion {
    fun <T> spatialSpring() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = 500f
    )

    fun <T> effectsSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f
    )
}

@Composable
fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetCorner: Dp = when {
        isPressed -> 10.dp
        isActive -> 14.dp
        else -> 28.dp
    }
    val cornerRadius by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "buttonCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "buttonScale"
    )

    val scheme = MaterialTheme.colorScheme
    val targetContainerColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.12f)
        isDestructive -> scheme.error
        else -> scheme.primary
    }
    val targetContentColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> scheme.onError
        else -> scheme.onPrimary
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = ExpressiveMotion.effectsSpring(),
        label = "buttonContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = ExpressiveMotion.effectsSpring(),
        label = "buttonContentColor"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (enabled) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun ExpressiveChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetCorner: Dp = when {
        isPressed -> 8.dp
        selected -> 12.dp
        else -> 28.dp
    }
    val cornerRadius by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "chipCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "chipScale"
    )

    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = ExpressiveMotion.effectsSpring(),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        animationSpec = ExpressiveMotion.effectsSpring(),
        label = "chipFg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = ExpressiveMotion.effectsSpring(),
        label = "chipBorder"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .height(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 10.dp
            isActive -> 12.dp
            else -> 20.dp
        },
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "iconBtnCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "iconBtnScale"
    )

    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = shape,
        color = if (isActive) scheme.primaryContainer else scheme.surfaceContainerHighest,
        contentColor = if (isActive) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun WavyLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavyProgress")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = if (indeterminate) 0.65f else progress.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.spatialSpring(),
        label = "progressValue"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val strokeWidth = 4.dp.toPx()
        val amplitude = 3.2.dp.toPx()
        val wavelength = 24.dp.toPx()

        val activeEnd = (width * animatedProgress).coerceIn(0f, width)

        if (activeEnd > 2f) {
            val wavePath = Path()
            var x = 0f
            val step = 2f
            while (x <= activeEnd) {
                val angle = (x / wavelength) * (2f * PI.toFloat()) - phase
                val y = centerY + sin(angle) * amplitude
                if (x == 0f) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
                x += step
            }
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        val trackStart = (activeEnd + 8.dp.toPx()).coerceAtMost(width)
        if (trackStart < width) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun ExpressiveFloatingNavBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(36.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val corner by animateDpAsState(
                    targetValue = when {
                        isPressed -> 14.dp
                        selected -> 20.dp
                        else -> 28.dp
                    },
                    animationSpec = ExpressiveMotion.spatialSpring(),
                    label = "navItemCorner"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = ExpressiveMotion.spatialSpring(),
                    label = "navItemScale"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
                    animationSpec = ExpressiveMotion.effectsSpring(),
                    label = "navItemBg"
                )
                val fgColor by animateColorAsState(
                    targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                    animationSpec = ExpressiveMotion.effectsSpring(),
                    label = "navItemFg"
                )

                val itemShape = RoundedCornerShape(corner)
                Surface(
                    modifier = Modifier
                        .height(52.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(itemShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    shape = itemShape,
                    color = bgColor,
                    contentColor = fgColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
