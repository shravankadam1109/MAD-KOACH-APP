package com.koach.studio.tasks.data

import com.google.firebase.firestore.FirebaseFirestore
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate

/** Agency task board in `tasks/{id}`. Clients never query it (the rules would deny them). */
class FirestoreTaskRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val clock: Clock
) : TaskRepository {

    private val collection get() = firestore.collection(COLLECTION)
    private val _events = MutableSharedFlow<TaskEvent>(extraBufferCapacity = 8)
    override val events: Flow<TaskEvent> = _events.asSharedFlow()

    override fun observeTasks(): Flow<List<TaskItem>> =
        currentUser.whenSignedIn(emptyList()) {
            if (!access.can(Permission.VIEW_TASKS)) flowOf(emptyList())
            else collection.observe { snap -> snap.documents.mapNotNull { FirestoreMappers.task(it.id, it.data.orEmpty()) } }
        }

    override fun observeTask(taskId: String): Flow<TaskItem?> = observeTasks().map { list -> list.find { it.id == taskId } }

    override suspend fun createTask(draft: TaskDraft): Result<TaskItem> = runCatching {
        val user = access.require(Permission.MANAGE_TASKS)
        validate(draft)
        val ref = collection.document()
        val task = TaskItem(
            ref.id, draft.title.trim(), draft.description.trim(), draft.projectId, draft.assigneeId, draft.priority,
            draft.status, draft.dueDate!!, user.id, LocalDate.now(clock)
        )
        ref.set(FirestoreMappers.taskFields(task)).await()
        _events.tryEmit(TaskEvent.Created(task.title))
        task
    }

    override suspend fun updateTask(taskId: String, draft: TaskDraft): Result<TaskItem> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        validate(draft)
        val existing = load(taskId)
        val updated = existing.copy(
            title = draft.title.trim(), description = draft.description.trim(), projectId = draft.projectId,
            assigneeId = draft.assigneeId, priority = draft.priority, status = draft.status, dueDate = draft.dueDate!!
        )
        collection.document(taskId).set(FirestoreMappers.taskFields(updated)).await()
        _events.tryEmit(TaskEvent.Updated(updated.id, updated.title))
        updated
    }

    override suspend fun setStatus(taskId: String, status: TaskStatus): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        load(taskId)
        collection.document(taskId).update("status", status.name).await()
        Unit
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_TASKS)
        val existing = load(taskId)
        collection.document(taskId).delete().await()
        _events.tryEmit(TaskEvent.Deleted(existing.id, existing.title))
        Unit
    }

    private suspend fun load(taskId: String): TaskItem {
        val snap = collection.document(taskId).get().await()
        return FirestoreMappers.task(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Task not found")
    }

    private fun validate(draft: TaskDraft) {
        val errors = TaskValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
    }

    companion object {
        const val COLLECTION = "tasks"
    }
}
