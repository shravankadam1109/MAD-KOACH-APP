package com.koach.studio.tasks.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

data class TaskDraft(
    val title: String,
    val description: String,
    val projectId: String?,
    val assigneeId: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val dueDate: LocalDate?
)

object TaskValidator {
    const val TITLE_MAX = 100
    const val DESCRIPTION_MAX = 500

    enum class Field { TITLE, DESCRIPTION, DUE_DATE }

    fun validate(draft: TaskDraft): Map<Field, String> = buildMap {
        val title = draft.title.trim()
        when {
            title.length < 3 -> put(Field.TITLE, "Give the task a short title (3+ characters)")
            title.length > TITLE_MAX -> put(Field.TITLE, "Keep the title under $TITLE_MAX characters")
        }
        if (draft.description.length > DESCRIPTION_MAX) put(Field.DESCRIPTION, "Keep it under $DESCRIPTION_MAX characters")
        if (draft.dueDate == null) put(Field.DUE_DATE, "Pick a due date")
    }
}

/** Agency-internal task board. Clients never see tasks. */
interface TaskRepository {
    fun observeTasks(): Flow<List<TaskItem>>
    fun observeTask(taskId: String): Flow<TaskItem?>
    suspend fun createTask(draft: TaskDraft): Result<TaskItem>
    suspend fun updateTask(taskId: String, draft: TaskDraft): Result<TaskItem>
    suspend fun setStatus(taskId: String, status: TaskStatus): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
}

class InMemoryTaskRepository(
    initial: List<TaskItem>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock
) : TaskRepository {

    private val store = MutableStateFlow(initial)

    override fun observeTasks(): Flow<List<TaskItem>> =
        combine(store, currentUser) { all, _ -> if (access.can(Permission.VIEW_TASKS)) all else emptyList() }

    override fun observeTask(taskId: String): Flow<TaskItem?> = observeTasks().map { list -> list.find { it.id == taskId } }

    override suspend fun createTask(draft: TaskDraft): Result<TaskItem> = runCatching {
        val user = access.require(Permission.MANAGE_TASKS)
        validate(draft)
        val task = TaskItem(
            id = "t-" + UUID.randomUUID().toString().take(8),
            title = draft.title.trim(),
            description = draft.description.trim(),
            projectId = draft.projectId,
            assigneeId = draft.assigneeId,
            priority = draft.priority,
            status = draft.status,
            dueDate = draft.dueDate!!,
            createdBy = user.id,
            createdOn = LocalDate.now(clock)
        )
        store.update { listOf(task) + it }
        task
    }

    override suspend fun updateTask(taskId: String, draft: TaskDraft): Result<TaskItem> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        validate(draft)
        val existing = store.value.find { it.id == taskId } ?: throw IllegalArgumentException("Task not found")
        val updated = existing.copy(
            title = draft.title.trim(),
            description = draft.description.trim(),
            projectId = draft.projectId,
            assigneeId = draft.assigneeId,
            priority = draft.priority,
            status = draft.status,
            dueDate = draft.dueDate!!
        )
        store.update { list -> list.map { if (it.id == taskId) updated else it } }
        updated
    }

    override suspend fun setStatus(taskId: String, status: TaskStatus): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        if (store.value.none { it.id == taskId }) throw IllegalArgumentException("Task not found")
        store.update { list -> list.map { if (it.id == taskId) it.copy(status = status) else it } }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        if (store.value.none { it.id == taskId }) throw IllegalArgumentException("Task not found")
        store.update { list -> list.filterNot { it.id == taskId } }
    }

    private fun validate(draft: TaskDraft) {
        val errors = TaskValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
    }
}
