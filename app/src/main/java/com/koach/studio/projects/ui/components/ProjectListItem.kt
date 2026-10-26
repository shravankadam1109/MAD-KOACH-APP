package com.koach.studio.projects.ui.components

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.ui.icon
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
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.title}, ${project.clientName}, Status: ${project.status.displayName}, ${(project.progress * 100).toInt()}% complete"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedStatusPill(project.status.displayName, project.status.tone, icon = project.status.icon)
            Spacer(Modifier.weight(1f))
            Text(
                text = project.serviceType.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = project.title,
            style = MaterialTheme.typography.titleMedium,
            color = c.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = project.clientName,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        KoachProgressBar(project.progress, height = 4.dp)
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${(project.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = c.brand
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Due date",
                tint = c.textTertiary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (project.status == ProjectStatus.COMPLETED) "Delivered" else formatDue(due, today),
                style = MaterialTheme.typography.labelMedium,
                color = if (overdue) Tone.Danger.color() else c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = "Deliverable files",
                tint = c.textTertiary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${project.deliverables.size} files",
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
                maxLines = 1
            )
        }
    }
}

/** Project status as a themed pill with icon. Kept for screens that show status on its own. */
@Composable
fun ProjectStatusBadge(status: ProjectStatus, modifier: Modifier = Modifier) {
    AnimatedStatusPill(
        text = status.displayName,
        tone = status.tone,
        icon = status.icon,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Project status: ${status.displayName}"
        }
    )
}
