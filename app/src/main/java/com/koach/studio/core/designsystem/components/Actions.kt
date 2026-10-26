package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachTheme

/** Round icon button with an optional unread badge, e.g. the notification bell. */
@Composable
fun BadgedIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0
) {
    val c = KoachTheme.colors
    Box(modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.outlineSubtle, CircleShape)
        ) {
            Icon(
                icon,
                contentDescription = if (count > 0) "$contentDescription, $count unread" else contentDescription,
                tint = c.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (count > 0) {
            CountBadge(count, Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp))
        }
    }
}

/** Compact quick-action tile used on dashboards. */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Brand
) {
    KoachCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Column {
            IconTile(icon, tone = tone, size = 34.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = KoachTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
