package com.koach.studio.home

import com.koach.studio.core.designsystem.theme.entrance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.BadgedIconButton
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachGlassPanel
import com.koach.studio.core.designsystem.components.KoachHeroCard
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.QuickActionTile
import com.koach.studio.core.designsystem.components.SectionHeader
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.ui.tone
import com.koach.studio.tasks.ui.tone

data class AgencyHomeActions(
    val onNotifications: () -> Unit,
    val onOpenProject: (String) -> Unit,
    val onOpenTask: (String) -> Unit,
    val onAllTasks: () -> Unit,
    val onNewTask: () -> Unit,
    val onBookings: () -> Unit,
    val onMessages: () -> Unit,
    val onAllProjects: () -> Unit = {},
    val onClients: () -> Unit = {},
    val onDocuments: () -> Unit = {}
)

@Composable
fun AgencyHomeScreen(
    viewModel: AgencyHomeViewModel,
    actions: AgencyHomeActions,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    KoachBackground(modifier.fillMaxSize()) {
        if (state.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("agency_home")
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = gutter, end = gutter, top = 16.dp).entrance(0)
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Eyebrow("${state.greeting} · Studio")
                            if (state.isDemo) {
                                DemoTag(text = "Demo data")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.firstName,
                            style = MaterialTheme.typography.displaySmall,
                            color = c.textPrimary
                        )
                    }
                    BadgedIconButton(
                        icon = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        onClick = actions.onNotifications,
                        count = state.unreadNotifications
                    )
                }
            }

            item {
                StudioSummary(state, actions, Modifier.padding(horizontal = gutter).entrance(1))
            }

            // Studio workflow shortcuts (2 rows of 3 compact tiles)
            item {
                Column(
                    modifier = Modifier.padding(horizontal = gutter).entrance(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionTile(Icons.Outlined.AddTask, "New task", actions.onNewTask, Modifier.weight(1f))
                        QuickActionTile(Icons.AutoMirrored.Outlined.EventNote, "Bookings", actions.onBookings, Modifier.weight(1f), Tone.Warning)
                        QuickActionTile(Icons.Outlined.Checklist, "Tasks", actions.onAllTasks, Modifier.weight(1f), Tone.Info)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionTile(Icons.Outlined.Folder, "Projects", actions.onAllProjects, Modifier.weight(1f), Tone.Neutral)
                        QuickActionTile(Icons.Outlined.Groups, "Clients", actions.onClients, Modifier.weight(1f), Tone.Neutral)
                        QuickActionTile(Icons.Outlined.Description, "Documents", actions.onDocuments, Modifier.weight(1f), Tone.Neutral)
                    }
                }
            }

            if (state.needsAttention.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Needs attention",
                        eyebrow = "Client feedback & reviews",
                        actionLabel = "All projects",
                        onAction = actions.onAllProjects
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = gutter),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.needsAttention.forEach { project ->
                            AttentionRow(project, onClick = { actions.onOpenProject(project.id) })
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "My tasks",
                    eyebrow = "Assigned to you",
                    actionLabel = "All tasks",
                    onAction = actions.onAllTasks
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(horizontal = gutter),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.myTasks.isEmpty()) {
                        KoachCard(Modifier.fillMaxWidth()) {
                            EmptyState(Icons.Outlined.TaskAlt, "All clear", "Nothing assigned to you right now.")
                        }
                    }
                    state.myTasks.forEach { task ->
                        MyTaskRow(task, state, onClick = { actions.onOpenTask(task.id) })
                    }
                }
            }

            if (state.upcomingDeadlines.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Upcoming deadlines",
                        eyebrow = "Next 14 days"
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = gutter)
                            .testTag("studio_upcoming_deadlines"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.upcomingDeadlines.forEach { deadline ->
                            DeadlineRow(
                                item = deadline,
                                today = state.today,
                                onClick = {
                                    if (deadline.isProject) {
                                        actions.onOpenProject(deadline.id)
                                    } else {
                                        actions.onOpenTask(deadline.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (state.teamWorkload.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Team workload",
                        eyebrow = "Task distribution",
                        actionLabel = "Manage",
                        onAction = actions.onAllTasks
                    )
                    Spacer(Modifier.height(10.dp))
                    KoachCard(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .padding(horizontal = gutter)
                            .fillMaxWidth()
                            .testTag("studio_team_workload")
                    ) {
                        state.teamWorkload.forEachIndexed { index, memberWorkload ->
                            TeamWorkloadRow(memberWorkload)
                            if (index < state.teamWorkload.lastIndex) {
                                KoachDivider(modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyTaskRow(task: TaskItem, state: AgencyHomeUiState, onClick: () -> Unit) {
    val c = KoachTheme.colors
    val overdue = task.isOverdue(state.today)
    KoachCard(onClick = onClick, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.projectId?.let { state.projectTitles[it] } ?: "Studio",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(task.priority.displayName, task.priority.tone)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatDue(task.dueDate, state.today),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) Tone.Danger.color() else c.textTertiary
                )
            }
        }
    }
}

@Composable
private fun AttentionRow(project: Project, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(onClick = onClick, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = project.clientName,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(project.status.displayName, project.status.tone)
        }
        Spacer(Modifier.height(10.dp))
        KoachProgressBar(project.progress, height = 4.dp)
    }
}

@Composable
private fun DeadlineRow(
    item: StudioDeadlineItem,
    today: java.time.LocalDate,
    onClick: () -> Unit
) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(KoachRadius.sm))
                    .background(if (item.isOverdue) c.danger.copy(alpha = 0.14f) else c.brandContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = if (item.isOverdue) c.danger else c.brand,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(
                    text = if (item.isProject) "Project" else "Task",
                    tone = if (item.isProject) Tone.Brand else Tone.Info,
                    showDot = false
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDue(item.dueDate, today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isOverdue) c.danger else c.textTertiary
                )
            }
        }
    }
}

@Composable
private fun TeamWorkloadRow(workload: TeamMemberWorkload) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        KoachAvatar(
            initials = workload.user.initials,
            size = 38.dp,
            highlighted = workload.inProgressTasks > 0
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = workload.user.name,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = workload.user.title,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (workload.overdueTasks > 0) {
                StatusPill("${workload.overdueTasks} overdue", Tone.Danger, showDot = false)
            }
            StatusPill(
                text = "${workload.openTasks} open",
                tone = if (workload.openTasks > 0) Tone.Brand else Tone.Neutral,
                showDot = false
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudioSummary(
    state: AgencyHomeUiState,
    actions: AgencyHomeActions,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    KoachHeroCard(modifier = modifier.fillMaxWidth().testTag("studio_summary")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Eyebrow("STUDIO RIGHT NOW", color = c.brand)
            Text(
                text = "${state.averageProgressPercent}% avg completion",
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.activeProjects.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = c.textPrimary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "active\nprojects",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${state.completedProjects} delivered",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textTertiary
                )
                Text(
                    text = "${state.completedTasks} tasks done",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        KoachProgressBar(progress = state.averageProgressPercent / 100f, height = 5.dp)

        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatusPill("${state.inProgressProjects} In Progress", Tone.Info)
            StatusPill("${state.inReviewProjects} In Review", Tone.Review)
            if (state.revisionRequestedProjects > 0) {
                StatusPill("${state.revisionRequestedProjects} Revisions", Tone.Warning)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryPanel(
                value = state.pendingBookings.toString(),
                label = "Booking\nrequests",
                onClick = actions.onBookings,
                modifier = Modifier.weight(1f).testTag("summary_bookings")
            )
            SummaryPanel(
                value = state.openTasks.toString(),
                label = "Open\ntasks",
                onClick = actions.onAllTasks,
                modifier = Modifier.weight(1f).testTag("summary_tasks")
            )
            SummaryPanel(
                value = state.overdueTasks.toString(),
                label = "Overdue\ntasks",
                onClick = actions.onAllTasks,
                highlightDanger = state.overdueTasks > 0,
                modifier = Modifier.weight(1f).testTag("summary_overdue")
            )
        }
    }
}

@Composable
private fun SummaryPanel(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightDanger: Boolean = false
) {
    val c = KoachTheme.colors
    KoachGlassPanel(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$value ${label.replace("\n", " ")}"
        }
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (highlightDanger) c.danger else c.brand,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.textSecondary
        )
    }
}
