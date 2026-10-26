package com.koach.studio.feedback

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Feedback
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** Feedback and complaints in `feedback/{id}`. */
class FirestoreFeedbackRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : FeedbackRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeFeedback(): Flow<List<Feedback>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val query = if (access.can(Permission.RESPOND_FEEDBACK)) collection else collection.whereEqualTo("clientId", user.clientId)
            query.orderBy("createdOn", Query.Direction.DESCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.feedback(it.id, it.data.orEmpty()) } }
        }

    override suspend fun submit(draft: FeedbackDraft): Result<Feedback> = runCatching {
        val user = access.require(Permission.SUBMIT_FEEDBACK)
        val errors = FeedbackValidator.validate(draft)
        require(errors.isEmpty()) { errors.values.first() }
        val ref = collection.document()
        val feedback = Feedback(
            id = ref.id,
            clientId = user.clientId ?: throw IllegalStateException("Only client accounts can send feedback"),
            authorId = user.id, projectId = draft.projectId, type = draft.type,
            rating = if (draft.type == FeedbackType.FEEDBACK) draft.rating else null,
            subject = draft.subject.trim(), message = draft.message.trim(), status = FeedbackStatus.OPEN,
            createdOn = LocalDate.now(clock)
        )
        ref.set(FirestoreMappers.feedbackFields(feedback)).await()
        val admins = runCatching { firestore.collection("users").whereEqualTo("role", "admin").get().await() }.getOrNull()
        admins?.documents?.forEach { admin ->
            notifications.push(
                AppNotification(
                    "", admin.id, NotificationType.FEEDBACK,
                    if (draft.type == FeedbackType.COMPLAINT) "New complaint" else "New feedback",
                    "${user.name}: ${feedback.subject}", LocalDateTime.now(clock), false, feedback.id
                )
            )
        }
        feedback
    }

    override suspend fun respond(feedbackId: String, response: String, status: FeedbackStatus): Result<Unit> = runCatching {
        access.require(Permission.RESPOND_FEEDBACK)
        val snap = collection.document(feedbackId).get().await()
        val existing = FirestoreMappers.feedback(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Feedback not found")
        require(response.isNotBlank() || status != existing.status) { "Write a reply or change the status" }
        collection.document(feedbackId).update(
            mapOf("status" to status.name, "response" to (response.trim().ifBlank { existing.response }))
        ).await()
        notifications.push(
            AppNotification(
                "", existing.authorId, NotificationType.FEEDBACK, "Studio replied: ${existing.subject}",
                response.trim().ifBlank { "Status is now ${status.displayName.lowercase()}." }.take(80),
                LocalDateTime.now(clock), false, existing.id
            )
        )
    }

    companion object {
        const val COLLECTION = "feedback"
    }
}
