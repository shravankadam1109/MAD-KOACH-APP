package com.koach.studio.feedback

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Feedback
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class FeedbackDraft(
    val type: FeedbackType,
    val rating: Int?,
    val subject: String,
    val message: String,
    val projectId: String?
)

object FeedbackValidator {
    const val SUBJECT_MAX = 80
    const val MESSAGE_MIN = 10
    const val MESSAGE_MAX = 1000

    enum class Field { RATING, SUBJECT, MESSAGE }

    fun validate(draft: FeedbackDraft): Map<Field, String> = buildMap {
        if (draft.type == FeedbackType.FEEDBACK && (draft.rating == null || draft.rating !in 1..5)) put(Field.RATING, "Choose a rating")
        val subject = draft.subject.trim()
        when {
            subject.length < 3 -> put(Field.SUBJECT, "Add a short subject")
            subject.length > SUBJECT_MAX -> put(Field.SUBJECT, "Keep it under $SUBJECT_MAX characters")
        }
        val message = draft.message.trim()
        when {
            message.length < MESSAGE_MIN -> put(Field.MESSAGE, "Tell us a little more (at least $MESSAGE_MIN characters)")
            message.length > MESSAGE_MAX -> put(Field.MESSAGE, "Keep it under $MESSAGE_MAX characters")
        }
    }
}

interface FeedbackRepository {
    /** Clients see what their organisation submitted; agency users see the whole inbox. */
    fun observeFeedback(): Flow<List<Feedback>>
    suspend fun submit(draft: FeedbackDraft): Result<Feedback>
    suspend fun respond(feedbackId: String, response: String, status: FeedbackStatus): Result<Unit>
}

class InMemoryFeedbackRepository(
    initial: List<Feedback>,
    private val users: List<User>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : FeedbackRepository {

    private val store = MutableStateFlow(initial)

    override fun observeFeedback(): Flow<List<Feedback>> =
        combine(store, currentUser) { all, user ->
            when {
                user == null -> emptyList()
                user.can(Permission.RESPOND_FEEDBACK) -> all
                else -> all.filter { it.clientId == user.clientId }
            }.sortedByDescending { it.createdOn }
        }

    override suspend fun submit(draft: FeedbackDraft): Result<Feedback> = runCatching {
        val user = access.require(Permission.SUBMIT_FEEDBACK)
        val errors = FeedbackValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
        val feedback = Feedback(
            id = "fb-" + UUID.randomUUID().toString().take(8),
            clientId = user.clientId ?: throw IllegalStateException("Only client accounts can send feedback"),
            authorId = user.id,
            projectId = draft.projectId,
            type = draft.type,
            rating = if (draft.type == FeedbackType.FEEDBACK) draft.rating else null,
            subject = draft.subject.trim(),
            message = draft.message.trim(),
            status = FeedbackStatus.OPEN,
            createdOn = LocalDate.now(clock)
        )
        store.update { listOf(feedback) + it }
        users.filter { it.role == Role.ADMIN }.forEach { admin ->
            notifications.push(
                AppNotification(
                    id = "n-" + UUID.randomUUID().toString().take(8),
                    recipientId = admin.id,
                    type = NotificationType.FEEDBACK,
                    title = if (draft.type == FeedbackType.COMPLAINT) "New complaint" else "New feedback",
                    body = "${user.name}: ${feedback.subject}",
                    createdAt = LocalDateTime.now(clock),
                    isRead = false,
                    targetId = feedback.id
                )
            )
        }
        feedback
    }

    override suspend fun respond(feedbackId: String, response: String, status: FeedbackStatus): Result<Unit> = runCatching {
        access.require(Permission.RESPOND_FEEDBACK)
        val existing = store.value.find { it.id == feedbackId } ?: throw IllegalArgumentException("Feedback not found")
        require(response.isNotBlank() || status != existing.status) { "Write a reply or change the status" }
        store.update { list ->
            list.map { if (it.id == feedbackId) it.copy(status = status, response = response.trim().ifBlank { it.response }) else it }
        }
        notifications.push(
            AppNotification(
                id = "n-" + UUID.randomUUID().toString().take(8),
                recipientId = existing.authorId,
                type = NotificationType.FEEDBACK,
                title = "Studio replied: ${existing.subject}",
                body = response.trim().ifBlank { "Status is now ${status.displayName.lowercase()}." }.take(80),
                createdAt = LocalDateTime.now(clock),
                isRead = false,
                targetId = existing.id
            )
        )
    }
}
