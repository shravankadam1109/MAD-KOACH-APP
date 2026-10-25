package com.koach.studio.home

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.koach.studio.core.designsystem.components.KoachHeroCard
import com.koach.studio.core.designsystem.components.KoachGlassPanel
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
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.BadgedIconButton
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.QuickActionTile
import com.koach.studio.core.designsystem.components.SectionHeader
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
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
    val onMessages: () -> Unit
)

@Composable
fun AgencyHomeScreen(viewModel: AgencyHomeViewModel, actions: AgencyHomeActions, modifier: Modifier = Modifier) {
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
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("agency_home")
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = gutter, end = gutter, top = 16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("${state.greeting} · Studio")
                        Spacer(Modifier.height(4.dp))
                        Text(state.firstName, style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                    }
                    BadgedIconButton(Icons.Outlined.Notifications, "Notifications", actions.onNotifications, count = state.unreadNotifications)
                }
            }
            item {
                StudioSummary(state, actions, Modifier.padding(horizontal = gutter))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = gutter)) {
                    QuickActionTile(Icons.Outlined.AddTask, "New\ntask", actions.onNewTask, Modifier.weight(1f))
                    QuickActionTile(Icons.AutoMirrored.Outlined.EventNote, "Booking\ninbox", actions.onBookings, Modifier.weight(1f), Tone.Warning)
                    QuickActionTile(Icons.Outlined.ChatBubbleOutline, "Client\nmessages", actions.onMessages, Modifier.weight(1f), Tone.Info)
                }
            }
            item {
                SectionHeader("My tasks", eyebrow = "Up next", actionLabel = "All tasks", onAction = actions.onAllTasks)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            if (state.needsAttention.isNotEmpty()) {
                item {
                    SectionHeader("Needs attention", eyebrow = "Client feedback")
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.needsAttention.forEach { project ->
                            AttentionRow(project, onClick = { actions.onOpenProject(project.id) })
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
                Text(task.title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    task.projectId?.let { state.projectTitles[it] } ?: "Studio",
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
                    formatDue(task.dueDate, state.today),
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
                Text(project.title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(project.clientName, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            StatusPill(project.status.displayName, project.status.tone)
        }
        Spacer(Modifier.height(12.dp))
        KoachProgressBar(project.progress, height = 4.dp)
    }
}

@Composable
private fun StudioSummary(state: AgencyHomeUiState, actions: AgencyHomeActions, modifier: Modifier = Modifier) {
    val ink = KoachTheme.colors.onBrand
    KoachHeroCard(modifier = modifier.fillMaxWidth().testTag("studio_summary")) {
        Text("STUDIO RIGHT NOW", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.7f))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(state.activeProjects.toString(), style = MaterialTheme.typography.displayLarge, color = ink)
            Spacer(Modifier.width(10.dp))
            Text("active\nprojects", style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryPanel(state.pendingBookings.toString(), "Booking\nrequests", actions.onBookings, Modifier.weight(1f).testTag("summary_bookings"))
            SummaryPanel(state.openTasks.toString(), "Open\ntasks", actions.onAllTasks, Modifier.weight(1f).testTag("summary_tasks"))
            SummaryPanel(state.overdueTasks.toString(), "Overdue\ntasks", actions.onAllTasks, Modifier.weight(1f).testTag("summary_overdue"))
        }
    }
}

@Composable
private fun SummaryPanel(value: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = KoachTheme.colors.onBrand
    KoachGlassPanel(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$value ${label.replace("\n", " ")}" }
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = ink)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.78f))
    }
}

