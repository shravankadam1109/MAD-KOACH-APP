package com.koach.studio.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * KOACH motion language. Everything moves on the same few curves and durations
 * so screens feel choreographed rather than individually animated:
 * content arrives on [EmphasizedDecelerate], leaves on [EmphasizedAccelerate],
 * and touch feedback uses springs so it never blocks rapid taps.
 */
object KoachMotion {
    const val Short = 160
    const val Medium = 280
    const val Long = 420
    const val Ambient = 22_000

    /** Gap between items in a staggered entrance; capped so long lists don't wait. */
    const val StaggerStep = 45
    const val MaxStaggerItems = 6

    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> enter(durationMillis: Int = Long, delayMillis: Int = 0) =
        tween<T>(durationMillis, delayMillis, EmphasizedDecelerate)

    fun <T> exit(durationMillis: Int = Short) = tween<T>(durationMillis, easing = EmphasizedAccelerate)

    fun <T> press() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
}

/** True when the user has turned animations off in system settings; motion then snaps. */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
            .getOrDefault(1f) == 0f
    }
}

/**
 * Tactile press feedback: the element settles slightly under the finger and
 * springs back on release. Pass the same [interactionSource] the clickable uses.
 */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, KoachMotion.press(), label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * One-time entrance: fades in and rises a few dp. [index] staggers siblings.
 * Use for the leading blocks of a screen, not for every row of a scrolling list.
 */
fun Modifier.entrance(index: Int = 0): Modifier = composed {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay((index.coerceAtMost(KoachMotion.MaxStaggerItems) * KoachMotion.StaggerStep).toLong())
            progress.animateTo(1f, KoachMotion.enter(KoachMotion.Long))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}
