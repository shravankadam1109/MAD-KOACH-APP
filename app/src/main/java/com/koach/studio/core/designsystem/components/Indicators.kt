package com.koach.studio.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.rememberReducedMotion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

/** Semantic tone shared by pills, badges and icons so status colour stays consistent. */
enum class Tone { Neutral, Brand, Success, Warning, Danger, Info, Review }

@Composable
fun Tone.color(): Color {
    val c = KoachTheme.colors
    return when (this) {
        Tone.Neutral -> c.textSecondary
        Tone.Brand, Tone.Success -> c.brand
        Tone.Warning -> c.warning
        Tone.Danger -> c.danger
        Tone.Info -> c.info
        Tone.Review -> c.review
    }
}

/** Compact status label: subtle tinted fill, coloured dot, uppercase text. */
@Composable
fun StatusPill(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    val accent = tone.color()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(KoachRadius.xs))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(KoachRadius.xs))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (showDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 0.55f
            ),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Small counter badge, e.g. unread messages. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = KoachTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.brand)
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .semantics { contentDescription = "$count unread" }
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onBrand
        )
    }
}

/** Initials avatar with an optional green ring for the signed-in user or online state. */
@Composable
fun KoachAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    highlighted: Boolean = false
) {
    val colors = KoachTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (highlighted) colors.brandContainer else colors.surfaceRaised)
            .border(1.5.dp, if (highlighted) colors.brand else colors.outline, CircleShape)
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            color = if (highlighted) colors.brand else colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Brand-green progress track that fills from empty when it first appears and
 * glides to new values after that. [progress] is clamped to 0..1.
 */
@Composable
fun KoachProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val colors = KoachTheme.colors
    val target = progress.coerceIn(0f, 1f)
    val reduced = rememberReducedMotion()
    val fill = remember { Animatable(if (reduced) target else 0f) }
    LaunchedEffect(target) { fill.animateTo(target, KoachMotion.enter(900)) }
    val animated = fill.value
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceHigh)
            .semantics { contentDescription = "${(progress.coerceIn(0f, 1f) * 100).toInt()} percent complete" }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(colors.brandDeep, colors.brand, colors.brandBright)))
        )
    }
}

/** Rounded square holding an icon, tinted by [tone]. Used in list rows and tiles. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Brand,
    size: Dp = 40.dp
) {
    val accent = tone.color()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(KoachRadius.sm))
            .background(accent.copy(alpha = 0.12f))
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(size * 0.5f))
    }
}

/** Labels content that is sample/demo data so it is never mistaken for real records. */
@Composable
fun DemoTag(modifier: Modifier = Modifier, text: String = "Demo") {
    StatusPill(text = text, tone = Tone.Info, modifier = modifier, showDot = false)
}
