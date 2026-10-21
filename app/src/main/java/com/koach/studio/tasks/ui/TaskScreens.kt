package com.koach.studio.tasks.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDateField
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.ui.formatDue
import com.koach.studio.tasks.data.TaskValidator
import com.koach.studio.tasks.data.TaskValidator.Field
import java.time.LocalDate

val TaskPriority.tone: Tone
    get() = when (this) {
        TaskPriority.LOW -> Tone.Neutral
        TaskPriority.MEDIUM -> Tone.Info
        TaskPriority.HIGH -> Tone.Warning
        TaskPriority.URGENT -> Tone.Danger
    }

val TaskStatus.tone: Tone
    get() = when (this) {
        TaskStatus.TODO -> Tone.Neutral
        TaskStatus.IN_PROGRESS -> Tone.Info
        TaskStatus.IN_REVIEW -> Tone.Review
        TaskStatus.DONE -> Tone.Success
    }

@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onOpenTask: (String) -> Unit,
    onNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        floatingActionButton = {
            if (state.canManage) {
                ExtendedFloatingActionButton(
                    onClick = onNewTask,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New task") },
                    modifier = Modifier.testTag("new_task")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("task_list")
        ) {
            item {
                Column(Modifier.padding(start = gutter, end = gutter, top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("${state.openCount} open")
                        if (state.overdueCount > 0) {
                            Spacer(Modifier.width(10.dp))
                            Eyebrow("${state.overdueCount} overdue", color = Tone.Danger.color())
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Tasks", style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KoachSegmentedControl(
                            options = listOf("All tasks", "Assigned to me"),
                            selectedIndex = if (state.onlyMine) 1 else 0,
                            onSelect = { viewModel.setOnlyMine(it == 1) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        SortMenu(state.sort, viewModel::setSort)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            item {
                KoachFilterChips(
                    options = TaskStatus.entries,
                    selected = state.statusFilter,
                    onSelected = viewModel::setStatusFilter,
                    label = { it.displayName }
                )
            }
            if (state.rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Checklist,
                        title = "Nothing here",
                        message = if (state.onlyMine) "No tasks assigned to you match this filter." else "No tasks match this filter.",
                        actionLabel = if (state.canManage) "New task" else null,
                        onAction = if (state.canManage) onNewTask else null
                    )
                }
            }
            items(state.rows, key = { it.task.id }) { row ->
                TaskCard(
                    row,
                    today = state.today,
                    canManage = state.canManage,
                    onToggleDone = { viewModel.toggleDone(row.task) },
                    onClick = { onOpenTask(row.task.id) },
                    modifier = Modifier.padding(horizontal = gutter)
                )
            }
        }
    }
}

@Composable
private fun SortMenu(sort: TaskSort, onSort: (TaskSort) -> Unit) {
    val c = KoachTheme.colors
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.testTag("task_sort")) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(sort.label, color = c.textSecondary, style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surfaceRaised) {
            TaskSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("Sort by ${option.label.lowercase()}", color = if (option == sort) c.brand else c.textPrimary) },
                    onClick = { onSort(option); open = false }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    row: TaskRow,
    today: LocalDate,
    canManage: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val task = row.task
    val done = task.status == TaskStatus.DONE
    KoachCard(
        onClick = if (canManage) onClick else null,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth().testTag("task_${task.id}")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (done) c.brand else c.surface)
                    .border(1.5.dp, if (done) c.brand else c.outline, CircleShape)
                    .toggleable(value = done, enabled = canManage, role = Role.Checkbox, onValueChange = { onToggleDone() })
                    .testTag("task_done_${task.id}")
            ) {
                if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = c.onBrand, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) c.textTertiary else c.textPrimary,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    row.projectTitle ?: "Studio",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(task.priority.displayName, task.priority.tone)
                    Spacer(Modifier.width(8.dp))
                    if (!done) StatusPill(task.status.displayName, task.status.tone, showDot = false)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (done) "Done" else formatDue(task.dueDate, today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.isOverdue) Tone.Danger.color() else c.textTertiary,
                        maxLines = 1,
                        modifier = Modifier.testTag("task_due_${task.id}")
                    )
                }
            }
            row.assignee?.let {
                Spacer(Modifier.width(10.dp))
                KoachAvatar(it.initials, size = 30.dp)
            }
        }
    }
}

@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors

    LaunchedEffect(state.done) { if (state.done) onBack() }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = if (state.isEditing) "Edit task" else "New task", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.isEditing && state.errorMessage == "Task not found" -> ErrorState("This task no longer exists.", Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = KoachTheme.spacing.gutter, vertical = 8.dp)
                    .testTag("task_form")
            ) {
                FormSection {
                    KoachTextField(state.title, viewModel::onTitle, "Title", error = state.errors[Field.TITLE], maxLength = TaskValidator.TITLE_MAX, modifier = Modifier.testTag("task_title"))
                    KoachTextField(state.description, viewModel::onDescription, "Description (optional)", error = state.errors[Field.DESCRIPTION], maxLength = TaskValidator.DESCRIPTION_MAX, singleLine = false, minLines = 3)
                    KoachDropdownField(
                        label = "Project",
                        options = listOf<String?>(null) + state.projects.map { it.id },
                        selected = state.projectId,
                        onSelected = viewModel::onProject,
                        optionLabel = { id -> state.projects.find { it.id == id }?.title ?: "No project (studio task)" },
                        placeholder = "No project (studio task)"
                    )
                    KoachDropdownField(
                        label = "Assignee",
                        options = listOf<String?>(null) + state.assignees.map { it.id },
                        selected = state.assigneeId,
                        onSelected = viewModel::onAssignee,
                        optionLabel = { id -> state.assignees.find { it.id == id }?.let { "${it.name} · ${it.title}" } ?: "Unassigned" },
                        placeholder = "Unassigned"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KoachDropdownField(
                            label = "Priority",
                            options = TaskPriority.entries,
                            selected = state.priority,
                            onSelected = viewModel::onPriority,
                            optionLabel = { it.displayName },
                            modifier = Modifier.weight(1f)
                        )
                        KoachDropdownField(
                            label = "Status",
                            options = TaskStatus.entries,
                            selected = state.status,
                            onSelected = viewModel::onStatus,
                            optionLabel = { it.displayName },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    KoachDateField(
                        label = "Due date",
                        value = state.dueDate,
                        onValueChange = viewModel::onDueDate,
                        error = state.errors[Field.DUE_DATE],
                        fieldModifier = Modifier.testTag("task_due_date")
                    )
                }
                state.errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
                KoachPrimaryButton(
                    text = if (state.isEditing) "Save task" else "Create task",
                    onClick = viewModel::save,
                    loading = state.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("save_task")
                )
                if (state.isEditing) {
                    Spacer(Modifier.height(10.dp))
                    KoachSecondaryButton(
                        text = "Delete task",
                        onClick = viewModel::delete,
                        destructive = true,
                        leadingIcon = Icons.Outlined.Delete,
                        modifier = Modifier.fillMaxWidth().testTag("delete_task")
                    )
                }
                Spacer(Modifier.height(16.dp).navigationBarsPadding())
            }
        }
    }
}
