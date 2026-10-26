package com.koach.studio.projects.ui.components

import com.koach.studio.core.designsystem.components.KoachDivider
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    // Open editorial row: title leads, everything else is quiet and secondary.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_item_${project.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = "${project.title}, ${project.clientName}, Status: ${project.status.displayName}, ${(project.progress * 100).toInt()}% complete"
            }
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 18.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(project.status.tone.color()))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${project.status.displayName} · ${project.serviceType.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = project.status.tone.color(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${project.clientName} · ${project.deliverables.size} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(project.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (project.status == ProjectStatus.COMPLETED) c.textSecondary else c.brand
                )
                Text(
                    text = if (project.status == ProjectStatus.COMPLETED) "Delivered" else formatDue(due, today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) Tone.Danger.color() else c.textTertiary,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        KoachProgressBar(project.progress, height = 2.dp)
        Spacer(Modifier.height(18.dp))
        KoachDivider()
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
