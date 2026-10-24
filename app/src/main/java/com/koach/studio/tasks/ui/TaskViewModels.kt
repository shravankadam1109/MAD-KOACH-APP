package com.koach.studio.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.model.User
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import com.koach.studio.tasks.data.TaskDraft
import com.koach.studio.tasks.data.TaskEvent
import com.koach.studio.tasks.data.TaskRepository
import com.koach.studio.tasks.data.TaskValidator
import com.koach.studio.tasks.data.TaskValidator.Field
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

enum class TaskSort(val label: String) {
    DUE_DATE("Due date"),
    PRIORITY("Priority"),
    NEWEST("Newest")
}

data class TaskRow(
    val task: TaskItem,
    val projectTitle: String?,
    val assignee: User?,
    val isOverdue: Boolean
)

data class TaskListUiState(
    val isLoading: Boolean = true,
    val rows: List<TaskRow> = emptyList(),
    val query: String = "",
    val statusFilter: TaskStatus? = null,
    val onlyMine: Boolean = false,
    val sort: TaskSort = TaskSort.DUE_DATE,
    val openCount: Int = 0,
    val overdueCount: Int = 0,
    val canManage: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val message: String? = null,
    val messageIsError: Boolean = false
)

private data class TaskFeedback(val text: String, val isError: Boolean = false)

private data class TaskFilters(val query: String, val status: TaskStatus?, val onlyMine: Boolean, val sort: TaskSort)

/** Sorts and filters tasks; pure so it can be unit-tested directly. */
fun List<TaskRow>.arranged(
    query: String,
    status: TaskStatus?,
    onlyMineId: String?,
    sort: TaskSort
): List<TaskRow> =
    filter { status == null || it.task.status == status }
        .filter { onlyMineId == null || it.task.assigneeId == onlyMineId }
        .filter { row ->
            query.isBlank() ||
                row.task.title.contains(query, ignoreCase = true) ||
                row.task.description.contains(query, ignoreCase = true) ||
                row.projectTitle?.contains(query, ignoreCase = true) == true ||
                row.assignee?.name?.contains(query, ignoreCase = true) == true
        }
        .let { rows ->
            when (sort) {
                // Done tasks sink to the bottom in every sort.
                TaskSort.DUE_DATE -> rows.sortedWith(compareBy<TaskRow>({ it.task.status == TaskStatus.DONE }, { it.task.dueDate }))
                TaskSort.PRIORITY -> rows.sortedWith(compareBy<TaskRow>({ it.task.status == TaskStatus.DONE }, { -it.task.priority.rank }, { it.task.dueDate }))
                TaskSort.NEWEST -> rows.sortedWith(compareBy<TaskRow>({ it.task.status == TaskStatus.DONE }).thenByDescending { it.task.createdOn })
            }
        }

class TaskListViewModel(
    private val user: User,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    private val people: List<User>,
    private val clock: Clock
) : ViewModel() {

    private val filters = MutableStateFlow(TaskFilters("", null, false, TaskSort.DUE_DATE))
    private val message = MutableStateFlow<TaskFeedback?>(null)

    init {
        viewModelScope.launch {
            tasks.events.collect { event ->
                message.value = when (event) {
                    is TaskEvent.Created -> TaskFeedback("Task \"${event.title}\" created")
                    is TaskEvent.Updated -> TaskFeedback("Task \"${event.title}\" saved")
                    is TaskEvent.Deleted -> TaskFeedback("Task \"${event.title}\" deleted")
                }
            }
        }
    }

    val uiState: StateFlow<TaskListUiState> =
        combine(tasks.observeTasks(), projects.getProjects(), filters, message) { all, projectList, f, msg ->
            val today = LocalDate.now(clock)
            val rows = all.map { it.toRow(projectList, today) }
            TaskListUiState(
                isLoading = false,
                rows = rows.arranged(f.query, f.status, if (f.onlyMine) user.id else null, f.sort),
                query = f.query,
                statusFilter = f.status,
                onlyMine = f.onlyMine,
                sort = f.sort,
                openCount = all.count { it.status != TaskStatus.DONE },
                overdueCount = all.count { it.isOverdue(today) },
                canManage = user.can(Permission.MANAGE_TASKS),
                today = today,
                message = msg?.text,
                messageIsError = msg?.isError == true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun onQuery(query: String) = filters.update { it.copy(query = query) }
    fun setStatusFilter(status: TaskStatus?) = filters.update { it.copy(status = status) }
    fun setOnlyMine(onlyMine: Boolean) = filters.update { it.copy(onlyMine = onlyMine) }
    fun setSort(sort: TaskSort) = filters.update { it.copy(sort = sort) }

    fun toggleDone(task: TaskItem) {
        viewModelScope.launch {
            val next = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
            message.value = tasks.setStatus(task.id, next).fold(
                { TaskFeedback(if (next == TaskStatus.DONE) "Marked \"${task.title}\" as done" else "Reopened \"${task.title}\"") },
                { TaskFeedback(it.message ?: "Couldn't update task", isError = true) }
            )
        }
    }

    fun messageShown() = message.update { null }

    private fun TaskItem.toRow(projectList: List<Project>, today: LocalDate) = TaskRow(
        task = this,
        projectTitle = projectList.find { it.id == projectId }?.title,
        assignee = people.find { it.id == assigneeId },
        isOverdue = isOverdue(today)
    )
}

// ---------------------------------------------------------------------------
// Task detail ViewModel — read-only summary of a single task
// ---------------------------------------------------------------------------

data class TaskDetailUiState(
    val isLoading: Boolean = true,
    val task: TaskItem? = null,
    val projectTitle: String? = null,
    val assignee: User? = null,
    val isOverdue: Boolean = false,
    val canManage: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val message: String? = null,
    val messageIsError: Boolean = false,
    val isDeleted: Boolean = false
)

class TaskDetailViewModel(
    private val taskId: String,
    private val user: User,
    private val tasks: TaskRepository,
    projects: ProjectRepository,
    private val people: List<User>,
    private val clock: Clock
) : ViewModel() {

    private val message = MutableStateFlow<TaskFeedback?>(null)
    private val deleted = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            tasks.events.collect { event ->
                when (event) {
                    is TaskEvent.Updated -> if (event.taskId == taskId) {
                        message.value = TaskFeedback("Task \"${event.title}\" saved")
                    }
                    is TaskEvent.Deleted -> if (event.taskId == taskId) {
                        deleted.value = true
                    }
                    is TaskEvent.Created -> Unit
                }
            }
        }
    }

    val uiState: StateFlow<TaskDetailUiState> =
        combine(tasks.observeTask(taskId), projects.getProjects(), message, deleted) { task, projectList, msg, isDeleted ->
            val today = LocalDate.now(clock)
            TaskDetailUiState(
                isLoading = false,
                task = task,
                projectTitle = task?.projectId?.let { id -> projectList.find { it.id == id }?.title },
                assignee = task?.assigneeId?.let { id -> people.find { it.id == id } },
                isOverdue = task?.isOverdue(today) == true,
                canManage = user.can(Permission.MANAGE_TASKS),
                today = today,
                message = msg?.text,
                messageIsError = msg?.isError == true,
                isDeleted = isDeleted
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    fun toggleDone() {
        val task = uiState.value.task ?: return
        viewModelScope.launch {
            val next = if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
            message.value = tasks.setStatus(task.id, next).fold(
                { TaskFeedback(if (next == TaskStatus.DONE) "Marked \"${task.title}\" as done" else "Reopened \"${task.title}\"") },
                { TaskFeedback(it.message ?: "Couldn't update task", isError = true) }
            )
        }
    }

    fun messageShown() = message.update { null }
}

// ---------------------------------------------------------------------------
// Task form ViewModel
// ---------------------------------------------------------------------------

data class TaskFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val projectId: String? = null,
    val assigneeId: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: LocalDate? = null,
    val projects: List<Project> = emptyList(),
    val assignees: List<User> = emptyList(),
    val errors: Map<Field, String> = emptyMap(),
    val isSaving: Boolean = false,
    val done: Boolean = false,
    val errorMessage: String? = null,
    val today: LocalDate = LocalDate.now()
) {
    fun toDraft() = TaskDraft(title, description, projectId, assigneeId, priority, status, dueDate)
}

class TaskFormViewModel(
    private val taskId: String?,
    user: User,
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    people: List<User>,
    clock: Clock,
    prefillProjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TaskFormUiState(
            isEditing = taskId != null,
            isLoading = true,
            assigneeId = user.id.takeIf { user.role.isAgency },
            projectId = prefillProjectId,
            assignees = people.filter { it.role.isAgency },
            today = LocalDate.now(clock)
        )
    )
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val projectList = projects.getProjects().first()
            val existing = taskId?.let { tasks.observeTask(it).first() }
            _uiState.update { state ->
                val base = state.copy(isLoading = false, projects = projectList)
                when {
                    taskId == null -> base
                    existing == null -> base.copy(errorMessage = "Task not found")
                    else -> base.copy(
                        title = existing.title, description = existing.description, projectId = existing.projectId,
                        assigneeId = existing.assigneeId, priority = existing.priority, status = existing.status,
                        dueDate = existing.dueDate
                    )
                }
            }
        }
    }

    fun onTitle(v: String) = _uiState.update { it.copy(title = v, errors = it.errors - Field.TITLE) }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v, errors = it.errors - Field.DESCRIPTION) }
    fun onProject(id: String?) = _uiState.update { it.copy(projectId = id) }
    fun onAssignee(id: String?) = _uiState.update { it.copy(assigneeId = id) }
    fun onPriority(p: TaskPriority) = _uiState.update { it.copy(priority = p) }
    fun onStatus(s: TaskStatus) = _uiState.update { it.copy(status = s) }
    fun onDueDate(d: LocalDate) = _uiState.update { it.copy(dueDate = d, errors = it.errors - Field.DUE_DATE) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val errors = TaskValidator.validate(state.toDraft())
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (taskId == null) tasks.createTask(state.toDraft()) else tasks.updateTask(taskId, state.toDraft())
            _uiState.update { it.copy(isSaving = false, done = result.isSuccess, errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    fun delete() {
        val id = taskId ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = tasks.deleteTask(id)
            _uiState.update {
                it.copy(isSaving = false, done = result.isSuccess, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
}
