package com.koach.studio.projects.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Applies a staggered fade-and-slide entrance animation based on item [index].
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 45,
    maxDelayMs: Int = 360,
    slideDistancePx: Float = 36f
): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val delay = (index * baseDelayMs).coerceAtMost(maxDelayMs)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 320, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "staggered_alpha_$index"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else slideDistancePx,
        animationSpec = tween(durationMillis = 320, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "staggered_translate_$index"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translateY
    }
}
