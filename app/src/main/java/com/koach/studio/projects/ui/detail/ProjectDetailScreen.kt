package com.koach.studio.projects.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Milestone
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.dueLocalDate
import com.koach.studio.projects.ui.components.DeliverableRow
import com.koach.studio.projects.ui.components.ProjectDetailShimmer
import com.koach.studio.projects.ui.tone
import java.time.LocalDate

@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onDeliverableClick: (deliverableId: String) -> Unit,
    onRequestRevisionClick: (deliverableId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDocuments: (() -> Unit)? = null,
    viewModel: ProjectDetailViewModel = koachViewModel(key = "project-$projectId") {
        ProjectDetailViewModel(projectId, it.projectRepository, it.authRepository.currentUser.value, it.people)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val today = remember { LocalDate.now() }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.actionMessageShown()
        }
    }

    val project = uiState.project
    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Project", subtitle = project?.clientName, onBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> ProjectDetailShimmer(Modifier.padding(innerPadding))
            uiState.errorMessage != null || project == null ->
                ErrorState(uiState.errorMessage ?: "Project not found", Modifier.padding(innerPadding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = gutter, end = gutter, top = innerPadding.calculateTopPadding() + 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.testTag("project_detail_content")
            ) {
                item {
                    KoachCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(project.status.displayName, project.status.tone, modifier = Modifier.testTag("project_status"))
                            Spacer(Modifier.weight(1f))
                            Text(project.serviceType.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            project.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.textPrimary,
                            modifier = Modifier.testTag("project_detail_title")
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(project.clientName, style = MaterialTheme.typography.bodyLarge, color = c.brand)
                        project.dueLocalDate?.let { due ->
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = "Due date", tint = c.textTertiary, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (project.status == ProjectStatus.COMPLETED) "Delivered · ${formatDate(due, today)}"
                                    else formatDue(due, today).let { rel ->
                                        val abs = formatDate(due, today)
                                        if (rel.endsWith(abs)) rel else "$rel · $abs"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = c.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${(project.progress * 100).toInt()}%", style = MaterialTheme.typography.headlineLarge, color = c.brand)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${project.milestones.count { it.completed }} of ${project.milestones.size} milestones",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        KoachProgressBar(project.progress)
                    }
                }

                if (uiState.canManage) {
                    item {
                        KoachDropdownField(
                            label = "Project status",
                            options = ProjectStatus.entries,
                            selected = project.status,
                            onSelected = viewModel::updateStatus,
                            optionLabel = { it.displayName },
                            modifier = Modifier.testTag("project_status_picker")
                        )
                    }
                }

                if (project.milestones.isNotEmpty()) {
                    item {
                        KoachCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Milestones", style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                                if (uiState.canManage) Text("Tap to update", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                            }
                            Spacer(Modifier.height(10.dp))
                            project.milestones.forEach { milestone ->
                                MilestoneItem(
                                    milestone = milestone,
                                    today = today,
                                    editable = uiState.canManage,
                                    onToggle = { viewModel.setMilestoneCompleted(milestone.id, it) }
                                )
                            }
                        }
                    }
                }

                if (uiState.team.isNotEmpty()) {
                    item {
                        Eyebrow("Your crew", color = c.textTertiary)
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(uiState.team, key = { it.id }) { person ->
                                KoachCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        KoachAvatar(person.initials, size = 32.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(person.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                                            Text(person.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Text("About this project", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                            modifier = Modifier.testTag("project_detail_description")
                        )
                    }
                }

                if (onOpenDocuments != null) {
                    item {
                        KoachSecondaryButton(
                            text = "Project documents",
                            onClick = onOpenDocuments,
                            leadingIcon = Icons.Outlined.FolderOpen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("project_documents_button")
                        )
                    }
                }

                if (uiState.canRequestRevision) {
                    item {
                        KoachSecondaryButton(
                            text = "Request a revision",
                            onClick = { onRequestRevisionClick(null) },
                            leadingIcon = Icons.Outlined.RateReview,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_revision_button")
                        )
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Deliverables", style = MaterialTheme.typography.titleLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                        Text("${project.deliverables.size} files", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                    }
                }
                items(project.deliverables, key = { it.id }) { deliverable ->
                    DeliverableRow(
                        deliverable = deliverable,
                        onEditClick = if (uiState.canEditDeliverables) ({ onDeliverableClick(deliverable.id) }) else null,
                        onDownloadClick = { viewModel.downloadDeliverable(deliverable.id) },
                        onApprove = if (uiState.canReviewDeliverables) ({ viewModel.approveDeliverable(deliverable.id) }) else null,
                        onRequestChanges = if (uiState.canRequestRevision) ({ onRequestRevisionClick(deliverable.id) }) else null
                    )
                }

                if (uiState.revisionHistory.isNotEmpty()) {
                    item {
                        Text(
                            "Revision history",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.textPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(uiState.revisionHistory, key = { it.revision.id }) { entry ->
                        RevisionHistoryItem(entry, today)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneItem(milestone: Milestone, today: LocalDate, editable: Boolean, onToggle: (Boolean) -> Unit) {
    val c = KoachTheme.colors
    val overdue = !milestone.completed && milestone.dueDate.isBefore(today)
    val rowModifier = if (editable) {
        Modifier.toggleable(value = milestone.completed, role = Role.Checkbox, onValueChange = onToggle)
    } else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(rowModifier)
            .padding(vertical = 10.dp)
            .testTag("milestone_${milestone.id}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (milestone.completed) c.brand else c.surface)
                .border(1.5.dp, if (milestone.completed) c.brand else if (overdue) Tone.Warning.color() else c.outline, CircleShape)
        ) {
            if (milestone.completed) Icon(Icons.Filled.Check, contentDescription = "Milestone completed", tint = c.onBrand, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            milestone.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (milestone.completed) c.textSecondary else c.textPrimary,
            textDecoration = if (milestone.completed) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (milestone.completed) formatDate(milestone.dueDate, today) else formatDue(milestone.dueDate, today).removePrefix("Due "),
            style = MaterialTheme.typography.labelMedium,
            color = if (overdue) Tone.Warning.color() else c.textTertiary
        )
    }
}

@Composable
private fun RevisionHistoryItem(entry: RevisionEntry, today: LocalDate) {
    val c = KoachTheme.colors
    KoachCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth().testTag("revision_${entry.revision.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KoachAvatar(entry.requesterName.split(" ").take(2).joinToString("") { it.take(1) }, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.requesterName, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                Text(
                    listOfNotNull(entry.deliverableName, formatDate(entry.revision.requestedOn, today)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )
            }
            StatusPill(if (entry.revision.resolved) "Resolved" else "Open", if (entry.revision.resolved) Tone.Success else Tone.Warning)
        }
        Spacer(Modifier.height(10.dp))
        Text("\u201C${entry.revision.reason}\u201D", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
}
