package com.koach.studio.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.core.designsystem.components.BadgedIconButton
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.DividedList
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ExpandableSection
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.JourneyStep
import com.koach.studio.core.designsystem.components.JourneyTracker
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.PriorityRow
import com.koach.studio.core.designsystem.components.QuietHeader
import com.koach.studio.core.designsystem.components.ShortcutButton
import com.koach.studio.core.designsystem.components.StepState
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.entrance
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus

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

/** One line of the studio's work queue: something that needs a person's attention now. */
private data class QueueItem(
    val key: String,
    val title: String,
    val meta: String,
    val trailing: String,
    val tone: QueueTone,
    val progress: Float?,
    val open: () -> Unit
)

private enum class QueueTone { Danger, Warning, Review }

private const val QUEUE_PREVIEW = 4

/**
 * Studio dashboard, built for progressive disclosure: one overview that says
 * how the studio is doing, the project pipeline at a glance, a compact set of
 * actions, and a short prioritised queue. Deadlines and crew load sit below,
 * and the crew detail opens on demand.
 */
@Composable
fun AgencyHomeScreen(
    viewModel: AgencyHomeViewModel,
    actions: AgencyHomeActions,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.xl

    KoachBackground(modifier.fillMaxSize()) {
        if (state.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        val queue = remember(state) { workQueue(state, actions) }
        val comingUp = remember(state) { state.upcomingDeadlines.filterNot { it.isOverdue } }

        LazyColumn(
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = 40.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("agency_home")
        ) {
            item { StudioHeader(state, actions.onNotifications, Modifier.entrance(0)) }
            item { StudioOverview(state, Modifier.entrance(1)) }
            item { Pipeline(state, Modifier.entrance(2)) }
            item { StudioActions(state, actions, Modifier.entrance(3)) }

            item {
                Spacer(Modifier.height(36.dp))
                QuietHeader(
                    "Needs you",
                    actionLabel = if (queue.size > QUEUE_PREVIEW) "View all" else null,
                    onAction = actions.onAllProjects
                )
                if (queue.isEmpty()) {
                    EmptyState(Icons.Outlined.TaskAlt, "Nothing waiting", "No revisions, reviews or overdue tasks right now.")
                } else {
                    DividedList(queue.take(QUEUE_PREVIEW), Modifier.testTag("studio_work_queue")) { item ->
                        PriorityRow(
                            title = item.title,
                            meta = item.meta,
                            trailing = item.trailing,
                            dot = when (item.tone) {
                                QueueTone.Danger -> c.danger
                                QueueTone.Warning -> c.warning
                                QueueTone.Review -> c.review
                            },
                            trailingColor = if (item.tone == QueueTone.Danger) c.danger else c.textTertiary,
                            progress = item.progress,
                            onClick = item.open
                        )
                    }
                }
            }

            if (comingUp.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(28.dp))
                    QuietHeader("Coming up", actionLabel = "All tasks", onAction = actions.onAllTasks)
                    DividedList(comingUp.take(3), Modifier.testTag("studio_upcoming_deadlines")) { deadline ->
                        PriorityRow(
                            title = deadline.title,
                            meta = deadline.subtitle,
                            trailing = formatDue(deadline.dueDate, state.today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                            dot = if (deadline.isProject) c.brand else c.textTertiary,
                            onClick = { if (deadline.isProject) actions.onOpenProject(deadline.id) else actions.onOpenTask(deadline.id) }
                        )
                    }
                }
            }

            if (state.teamWorkload.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    KoachDivider()
                    Spacer(Modifier.height(8.dp))
                    val busiest = state.teamWorkload.first()
                    ExpandableSection(
                        title = "Crew",
                        summary = "${state.teamWorkload.size} people · ${busiest.user.firstName} has the most open tasks (${busiest.openTasks})",
                        modifier = Modifier.testTag("studio_team_workload")
                    ) {
                        state.teamWorkload.forEachIndexed { index, member ->
                            CrewRow(member)
                            if (index < state.teamWorkload.lastIndex) KoachDivider(Modifier.padding(start = 50.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Revisions first, then overdue tasks, then work waiting on a client's review. */
private fun workQueue(state: AgencyHomeUiState, actions: AgencyHomeActions): List<QueueItem> {
    val revisions = state.needsAttention
        .filter { it.status == ProjectStatus.REVISION_REQUESTED }
        .map { it.toQueueItem(state, QueueTone.Warning, "Revision requested", actions) }
    val overdue = state.upcomingDeadlines.filter { it.isOverdue && !it.isProject }.map {
        QueueItem("task-${it.id}", it.title, it.subtitle, formatDue(it.dueDate, state.today), QueueTone.Danger, null) { actions.onOpenTask(it.id) }
    }
    val reviews = state.needsAttention
        .filter { it.status == ProjectStatus.IN_REVIEW }
        .map { it.toQueueItem(state, QueueTone.Review, "With the client for review", actions) }
    return revisions + overdue + reviews
}

private fun Project.toQueueItem(state: AgencyHomeUiState, tone: QueueTone, why: String, actions: AgencyHomeActions) = QueueItem(
    key = "project-$id",
    title = title,
    meta = "$clientName · $why",
    trailing = formatDue(dueDate, state.today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
    tone = tone,
    progress = progress,
    open = { actions.onOpenProject(id) }
)

@Composable
private fun StudioHeader(state: AgencyHomeUiState, onNotifications: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow("Studio")
                if (state.isDemo) DemoTag(text = "Demo data")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.greeting}, ${state.firstName}",
                style = MaterialTheme.typography.headlineLarge,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BadgedIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            onClick = onNotifications,
            count = state.unreadNotifications,
            modifier = Modifier.testTag("studio_notifications")
        )
    }
}

/**
 * The single focal point: active projects as one large figure, average
 * progress as a hairline, and the one other fact most worth acting on.
 */
@Composable
private fun StudioOverview(state: AgencyHomeUiState, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    val shown by animateIntAsState(state.activeProjects, label = "active-count")
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${state.activeProjects} active projects, ${state.averageProgressPercent} percent average progress"
            }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                shown.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp, lineHeight = 84.sp, letterSpacing = (-3).sp),
                color = c.textPrimary
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.padding(bottom = 12.dp)) {
                Text("active", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Text("projects", style = MaterialTheme.typography.titleLarge, color = c.textSecondary)
            }
        }
        Spacer(Modifier.height(18.dp))
        KoachProgressBar(state.averageProgressPercent / 100f, height = 3.dp)
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.brand)) { append("${state.averageProgressPercent}%") }
                    append(" average progress")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.weight(1f)
            )
            val (fact, factColor) = when {
                state.overdueTasks > 0 -> "${state.overdueTasks} overdue ${if (state.overdueTasks == 1) "task" else "tasks"}" to c.danger
                state.pendingBookings > 0 -> "${state.pendingBookings} new ${if (state.pendingBookings == 1) "booking" else "bookings"}" to c.warning
                else -> "${state.openTasks} open tasks" to c.textSecondary
            }
            Text(fact, style = MaterialTheme.typography.bodyMedium, color = factColor)
        }
    }
}

/** Where the studio's projects stand right now, as connected stages (counts, not a timeline). */
@Composable
private fun Pipeline(state: AgencyHomeUiState, modifier: Modifier = Modifier) {
    val stages = listOf(
        "In progress" to state.inProgressProjects,
        "In review" to state.inReviewProjects,
        "Revisions" to state.revisionRequestedProjects,
        "Delivered" to state.completedProjects
    )
    Column(modifier.fillMaxWidth().padding(top = 32.dp)) {
        Text("Pipeline", style = MaterialTheme.typography.labelLarge, color = KoachTheme.colors.textTertiary)
        Spacer(Modifier.height(14.dp))
        JourneyTracker(
            steps = stages.mapIndexed { index, (label, count) ->
                JourneyStep(
                    label = label,
                    value = count.toString(),
                    state = when {
                        count == 0 -> StepState.Upcoming
                        index == stages.lastIndex -> StepState.Complete
                        else -> StepState.Active
                    }
                )
            },
            // Counts per stage, not a sequence: the line stays a neutral track.
            fill = 0f,
            description = stages.joinToString(prefix = "Project pipeline: ") { (label, count) -> "$count $label" },
            modifier = Modifier.testTag("studio_pipeline")
        )
    }
}

/** One clear primary action, then compact shortcuts for everything else. */
@Composable
private fun StudioActions(state: AgencyHomeUiState, actions: AgencyHomeActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 32.dp)) {
        KoachPrimaryButton(
            text = "New task",
            onClick = actions.onNewTask,
            leadingIcon = Icons.Outlined.Add,
            modifier = Modifier.fillMaxWidth().testTag("studio_new_task")
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            ShortcutButton(Icons.AutoMirrored.Outlined.EventNote, "Bookings", actions.onBookings, Modifier.weight(1f), badge = state.pendingBookings)
            ShortcutButton(Icons.Outlined.Checklist, "Tasks", actions.onAllTasks, Modifier.weight(1f))
            ShortcutButton(Icons.Outlined.Folder, "Projects", actions.onAllProjects, Modifier.weight(1f))
            ShortcutButton(Icons.Outlined.Groups, "Clients", actions.onClients, Modifier.weight(1f))
            ShortcutButton(Icons.Outlined.Description, "Docs", actions.onDocuments, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CrewRow(workload: TeamMemberWorkload) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        KoachAvatar(initials = workload.user.initials, size = 36.dp, highlighted = workload.inProgressTasks > 0)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(workload.user.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(workload.user.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${workload.openTasks} open",
                style = MaterialTheme.typography.labelLarge,
                color = if (workload.openTasks > 0) c.textPrimary else c.textTertiary
            )
            if (workload.overdueTasks > 0) {
                Text("${workload.overdueTasks} overdue", style = MaterialTheme.typography.labelMedium, color = c.danger)
            }
        }
    }
}
