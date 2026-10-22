package com.koach.studio.projects.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.ui.tone
import java.time.LocalDate

@Composable
fun ProjectListItem(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    val c = KoachTheme.colors
    val due = project.dueDate
    val overdue = due.isBefore(today) && project.status != ProjectStatus.COMPLETED

    KoachCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(project.status.displayName, project.status.tone)
            Spacer(Modifier.weight(1f))
            Text(project.serviceType.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
        }
        Spacer(Modifier.height(14.dp))
        Text(project.title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(project.clientName, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))
        KoachProgressBar(project.progress, height = 5.dp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${(project.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = c.brand)
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (project.status == ProjectStatus.COMPLETED) "Delivered" else formatDue(due, today),
                style = MaterialTheme.typography.labelMedium,
                color = if (overdue) Tone.Danger.color() else c.textSecondary
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("${project.deliverables.size} files", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        }
    }
}

/** Project status as a themed pill. Kept for screens that show status on its own. */
@Composable
fun ProjectStatusBadge(status: ProjectStatus, modifier: Modifier = Modifier) {
    StatusPill(status.displayName, status.tone, modifier = modifier)
}
