package com.koach.studio.projects.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color

/**
 * Drop-in replacement for StatusPill that smoothly animates both the
 * accent colour and the label text whenever [tone] or [text] changes.
 *
 * Supports an optional [icon] so color is not the only status signal.
 */
@Composable
fun AnimatedStatusPill(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showDot: Boolean = true
) {
    val accent by animateColorAsState(
        targetValue = tone.color(),
        animationSpec = tween(durationMillis = 380),
        label = "status_pill_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Status: $text"
            }
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(5.dp))
        } else if (showDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(120))
            },
            label = "status_pill_label"
        ) { label ->
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 0.6f
                ),
                color = accent
            )
        }
    }
}
