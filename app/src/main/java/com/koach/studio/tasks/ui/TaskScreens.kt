package com.koach.studio.tasks.ui

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachAvatar
import androidx.compose.ui.text.style.TextAlign
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDateField
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSearchField
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachSnackbarHost
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.ui.formatDate
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

// ---------------------------------------------------------------------------
// Task List Screen
// ---------------------------------------------------------------------------

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
        snackbarHost = { KoachSnackbarHost(snackbar, isError = state.messageIsError) },
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tasks",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.textPrimary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(12.dp))
                    // Search bar
                    KoachSearchField(
                        query = state.query,
                        onQueryChange = viewModel::onQuery,
                        placeholder = "Search tasks, projects or assignees",
                        modifier = Modifier.testTag("task_search")
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KoachSegmentedControl(
                            options = listOf("All tasks", "Assigned to me"),
                            selectedIndex = if (state.onlyMine) 1 else 0,
                            onSelect = { viewModel.setOnlyMine(it == 1) },
                            modifier = Modifier.weight(1f)
                        )
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
                        title = if (state.query.isNotBlank()) "No results for \"${state.query}\"" else "Nothing here",
                        message = when {
                            state.query.isNotBlank() -> "Try a different search term or clear the filters."
                            state.onlyMine -> "No tasks assigned to you match this filter."
                            else -> "No tasks match this filter."
                        },
                        actionLabel = if (state.canManage && state.query.isBlank()) "New task" else null,
                        onAction = if (state.canManage && state.query.isBlank()) onNewTask else null
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
        TextButton(
            onClick = { open = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
                .semantics { contentDescription = "Sort tasks, current: ${sort.label}" }
                .testTag("task_sort")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = sort.label,
                color = c.textSecondary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

/**
 * Round done/not-done control. The visible circle stays small, but the touch target is 48dp and the
 * control is announced with the task title (e.g. "Mark Cut teaser as done, checkbox, not checked").
 */
@Composable
private fun TaskDoneToggle(
    done: Boolean,
    enabled: Boolean,
    taskTitle: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    circleSize: Dp = 24.dp
) {
    val c = KoachTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = (-6).dp, y = (-6).dp)
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(value = done, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = "Mark $taskTitle as done" }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(if (done) c.brand else c.surface)
                .border(1.5.dp, if (done) c.brand else c.outline, CircleShape)
        ) {
            if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = c.onBrand, modifier = Modifier.size(15.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth().testTag("task_${task.id}")
    ) {
        Row(verticalAlignment = Alignment.Top) {
            TaskDoneToggle(
                done = done,
                enabled = canManage,
                taskTitle = task.title,
                onToggle = onToggleDone,
                modifier = Modifier.testTag("task_done_${task.id}")
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) c.textTertiary else c.textPrimary,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.projectTitle ?: "Studio",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusPill(task.priority.displayName, task.priority.tone)
                    if (!done) StatusPill(task.status.displayName, task.status.tone, showDot = false)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (done) "Done" else formatDue(task.dueDate, today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.isOverdue) Tone.Danger.color() else c.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("task_due_${task.id}")
                    )
                    row.assignee?.let { assignee ->
                        Spacer(Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = "Assigned to ${assignee.name}"
                            }
                        ) {
                            KoachAvatar(
                                initials = assignee.initials,
                                size = 24.dp
                            )
                            Text(
                                text = assignee.name.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Task Detail Screen — read-only; Edit FAB for agency users
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Task", onBack = onBack) },
        floatingActionButton = {
            if (state.canManage && state.task != null) {
                FloatingActionButton(
                    onClick = onEdit,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    modifier = Modifier.testTag("edit_task_fab")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit task")
                }
            }
        },
        snackbarHost = { KoachSnackbarHost(snackbar, isError = state.messageIsError) },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.task == null -> ErrorState("This task no longer exists.", Modifier.padding(padding))
            else -> {
                val task = state.task!!
                val done = task.status == TaskStatus.DONE
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = 96.dp,
                        start = gutter,
                        end = gutter
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("task_detail")
                ) {
                    // Header card
                    item {
                        KoachCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                TaskDoneToggle(
                                    done = done,
                                    enabled = state.canManage,
                                    taskTitle = task.title,
                                    onToggle = viewModel::toggleDone,
                                    circleSize = 26.dp,
                                    modifier = Modifier.testTag("task_detail_done")
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        task.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (done) c.textTertiary else c.textPrimary,
                                        textDecoration = if (done) TextDecoration.LineThrough else null
                                    )
                                    if (task.description.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            task.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = c.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Status chips row
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(task.priority.displayName, task.priority.tone)
                            StatusPill(task.status.displayName, task.status.tone, showDot = false)
                            if (state.isOverdue) StatusPill("Overdue", Tone.Danger)
                        }
                    }
                    // Details card
                    item {
                        KoachCard(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Eyebrow("Assignment & schedule", color = c.textTertiary)
                                Spacer(Modifier.height(6.dp))
                                InfoRow("Project", state.projectTitle ?: "Studio (no project)")
                                KoachDivider()
                                InfoRow(
                                    "Assignee",
                                    state.assignee?.let { "${it.name} · ${it.title}" } ?: "Unassigned"
                                )
                                KoachDivider()
                                // Due date row — show in danger colour when overdue
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text("Due date", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                                    Text(
                                        formatDate(task.dueDate),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (state.isOverdue) c.danger else c.textPrimary,
                                        textAlign = TextAlign.End
                                    )
                                }
                                KoachDivider()
                                InfoRow("Created", formatDate(task.createdOn))
                            }
                        }
                    }
                    // Mark done / undone button (for non-managers who can't reach the form)
                    item {
                        if (state.canManage) {
                            Spacer(Modifier.height(4.dp))
                            if (done) {
                                KoachSecondaryButton(
                                    text = "Reopen task",
                                    onClick = viewModel::toggleDone,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                KoachPrimaryButton(
                                    text = "Mark as done",
                                    onClick = viewModel::toggleDone,
                                    modifier = Modifier.fillMaxWidth().testTag("mark_done")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Task Form Screen
// ---------------------------------------------------------------------------

@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors

    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onBack() }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surfaceRaised,
            title = { Text("Delete this task?", color = c.textPrimary) },
            text = { Text("\"${state.title}\" will be removed for the whole team. This can't be undone.", color = c.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                    modifier = Modifier.testTag("confirm_delete_task")
                ) { Text("Delete", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = c.textSecondary) } }
        )
    }

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
                    Text(
                        it,
                        color = c.danger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
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
                        onClick = { confirmDelete = true },
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
