package com.koach.studio.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.pressScale
import com.koach.studio.core.designsystem.theme.rememberReducedMotion
import kotlinx.coroutines.delay

/** Where a step of a journey stands. */
enum class StepState { Complete, Active, Upcoming }

/** One stop on a [JourneyTracker]: a short label and an optional value shown inside or under the dot. */
data class JourneyStep(val label: String, val state: StepState, val value: String? = null)

/**
 * Connected milestone dots on a thin line. On first appearance the line draws
 * from left to right and each dot lights as the line reaches it. [fill] is how
 * far along the line the green runs (0..1); the rest stays a quiet track.
 * Values are shown inside the dots, labels underneath, so the row stays compact.
 */
@Composable
fun JourneyTracker(
    steps: List<JourneyStep>,
    fill: Float,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    if (steps.isEmpty()) return
    val c = KoachTheme.colors
    val reduced = rememberReducedMotion()
    var played by rememberSaveable { mutableStateOf(false) }
    val draw = remember { Animatable(if (reduced || played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (draw.value < 1f) {
            delay(180)
            draw.animateTo(1f, KoachMotion.enter(1100))
        }
        played = true
    }
    val track = c.textTertiary.copy(alpha = 0.28f)
    val green = c.brand
    val target = fill.coerceIn(0f, 1f)
    Column(modifier.semantics(mergeDescendants = true) { if (description != null) contentDescription = description }) {
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.CenterStart) {
            // The line sits between the first and last dot centres.
            Canvas(Modifier.fillMaxWidth().height(34.dp)) {
                val inset = size.width / (steps.size * 2f)
                val y = size.height / 2f
                val start = Offset(inset, y)
                val end = Offset(size.width - inset, y)
                val drawn = draw.value
                drawLine(track, start, Offset(start.x + (end.x - start.x) * drawn, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(green, start, Offset(start.x + (end.x - start.x) * minOf(drawn, target), y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            Row(Modifier.fillMaxWidth()) {
                steps.forEachIndexed { index, step ->
                    val reachedAt = if (steps.size == 1) 0f else index / (steps.size - 1f)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        JourneyDot(step, lit = draw.value >= reachedAt - 0.001f)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            steps.forEach { step ->
                Text(
                    step.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (step.state == StepState.Upcoming) c.textTertiary else c.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun JourneyDot(step: JourneyStep, lit: Boolean) {
    val c = KoachTheme.colors
    val on = lit && step.state != StepState.Upcoming
    val scale by animateFloatAsState(if (lit) 1f else 0.6f, KoachMotion.press(), label = "dot-scale")
    val fillColor by animateColorAsState(
        when {
            !on -> c.background
            step.state == StepState.Complete -> c.brand
            else -> c.brandContainer
        },
        KoachMotion.enter(KoachMotion.Medium), label = "dot-fill"
    )
    val edge by animateColorAsState(if (on) c.brand else c.textTertiary.copy(alpha = 0.5f), KoachMotion.enter(KoachMotion.Medium), label = "dot-edge")
    val size = if (step.value != null) 30.dp else if (step.state == StepState.Active) 16.dp else 12.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size)
            .then(if (on && step.state == StepState.Active) Modifier.koachGlow(c.glow, 0.5f) else Modifier)
            .clip(CircleShape)
            .background(fillColor)
            .border(1.5.dp, edge, CircleShape)
            .semantics { stateDescription = step.state.name.lowercase() }
    ) {
        if (step.value != null) {
            Text(
                step.value,
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    !on -> c.textTertiary
                    step.state == StepState.Complete -> c.onBrand
                    else -> c.brandBright
                }
            )
        }
    }
}

/** A compact round shortcut with a short label; replaces large equal-weight action tiles. */
@Composable
fun ShortcutButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    val c = KoachTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = if (badge > 0) "$label, $badge new" else label }
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(c.glassRaised)
                    .border(1.dp, c.glassBorder, CircleShape)
            ) {
                Icon(icon, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(20.dp))
            }
            if (badge > 0) CountBadge(badge, Modifier.align(Alignment.TopEnd))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A quiet section heading: one title and an optional text action, no eyebrow stack. */
@Composable
fun QuietHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) KoachTextButton(actionLabel, onAction)
    }
}

/**
 * One line of a prioritised list: a small status dot, a title that leads, a
 * quiet meta line, and a trailing note. An optional hairline shows progress.
 */
@Composable
fun PriorityRow(
    title: String,
    meta: String,
    trailing: String,
    dot: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingColor: Color = KoachTheme.colors.textTertiary,
    progress: Float? = null
) {
    val c = KoachTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = trailingColor, maxLines = 1)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            KoachProgressBar(progress, Modifier.padding(start = 22.dp), height = 2.dp)
        }
    }
}

/** A section whose body opens and closes in place; the summary line stays visible. */
@Composable
fun ExpandableSection(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = KoachTheme.colors
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, KoachMotion.press(), label = "chevron")
    Column(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = if (expanded) "Collapse $title" else "Expand $title") { expanded = !expanded }
                .padding(vertical = 10.dp)
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(KoachMotion.press()) + fadeIn(KoachMotion.enter(KoachMotion.Medium, delayMillis = 60)),
            exit = shrinkVertically(KoachMotion.press()) + fadeOut(KoachMotion.exit())
        ) {
            Column(content = content)
        }
    }
}

/** Hairline-separated group without a card around it. */
@Composable
fun <T> DividedList(items: List<T>, modifier: Modifier = Modifier, row: @Composable (T) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.Top) {
        items.forEachIndexed { index, item ->
            row(item)
            if (index < items.lastIndex) KoachDivider(Modifier.padding(start = 22.dp))
        }
    }
}
