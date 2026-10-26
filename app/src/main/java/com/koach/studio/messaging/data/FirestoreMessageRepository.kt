package com.koach.studio.messaging.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.NotSignedInException
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.ThreadType
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDateTime

/**
 * Project conversations: `threads/{id}` with `messages/{id}` underneath.
 * Snapshot listeners deliver new messages in real time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreMessageRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : MessageRepository {

    private val threads get() = firestore.collection(COLLECTION)
    private fun messagesOf(threadId: String) = threads.document(threadId).collection(MESSAGES)

    /**
     * Only queries the security rules can prove safe: threads the user takes part
     * in, plus (for the agency) project threads. Never an unfiltered list, which
     * could include other people's DMs.
     */
    private fun visibleThreads(): Flow<List<MessageThread>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val mine = threads.whereArrayContains("participantIds", user.id)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.thread(it.id, it.data.orEmpty()) } }
            if (!user.role.isAgency) return@whenSignedIn mine
            val projectThreads = threads.whereEqualTo("type", ThreadType.PROJECT.name)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.thread(it.id, it.data.orEmpty()) } }
            combine(mine, projectThreads) { a, b -> (a + b).distinctBy { it.id } }
        }

    override fun observeThreads(): Flow<List<ThreadSummary>> =
        visibleThreads().flatMapLatest { list ->
            if (list.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val uid = currentUser.value?.id
            combine(list.map { thread -> observeMessages(thread.id).map { thread to it } }) { pairs ->
                pairs.map { (thread, messages) ->
                    ThreadSummary(
                        thread = thread,
                        lastMessage = messages.maxByOrNull { it.sentAt },
                        unreadCount = messages.count { uid != null && it.senderId != uid && uid !in it.readBy }
                    )
                }.sortedByDescending { it.lastMessage?.sentAt ?: LocalDateTime.MIN }
            }
        }

    override fun observeThread(threadId: String): Flow<MessageThread?> = visibleThreads().map { list -> list.find { it.id == threadId } }

    override fun observeMessages(threadId: String): Flow<List<Message>> =
        currentUser.whenSignedIn(emptyList()) {
            messagesOf(threadId).orderBy("sentAt", Query.Direction.ASCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.message(it.id, threadId, it.data.orEmpty()) } }
        }

    override fun observeUnreadCount(): Flow<Int> = observeThreads().map { list -> list.sumOf { it.unreadCount } }

    override suspend fun sendMessage(threadId: String, body: String): Result<Message> = runCatching {
        val user = access.require(Permission.SEND_MESSAGES)
        val snap = threads.document(threadId).get().await()
        val thread = FirestoreMappers.thread(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Conversation not found")
        val isParticipant = user.id in thread.participantIds

        val canAccess = if (thread.type == ThreadType.DM) isParticipant else (user.role.isAgency || isParticipant)

        if (!canAccess) throw PermissionDeniedException("You're not part of this conversation")
        val text = body.trim()
        require(text.isNotEmpty()) { "Write a message first" }
        require(text.length <= MessageRules.MAX_LENGTH) { "Messages can be up to ${MessageRules.MAX_LENGTH} characters" }

        val ref = messagesOf(threadId).document()
        val message = Message(ref.id, threadId, user.id, text, LocalDateTime.now(clock), setOf(user.id))
        ref.set(FirestoreMappers.messageFields(message)).await()
        // Agency members who reply join project threads so they hear about future messages.
        if (thread.type != ThreadType.DM && user.id !in thread.participantIds) {
            threads.document(threadId).update("participantIds", FieldValue.arrayUnion(user.id)).await()
        }
        thread.participantIds.filter { it != user.id }.forEach { recipient ->
            notifications.push(
                AppNotification("", recipient, NotificationType.MESSAGE, "New message from ${user.firstName}", text.take(80), message.sentAt, false, threadId)
            )
        }
        message
    }

    override suspend fun markThreadRead(threadId: String): Result<Unit> = runCatching {
        val user = currentUser.value ?: throw NotSignedInException()
        val unread = messagesOf(threadId).get().await().documents.filter { doc ->
            val readBy = (doc.get("readBy") as? List<*>).orEmpty()
            user.id !in readBy
        }
        if (unread.isEmpty()) return@runCatching
        val batch = firestore.batch()
        unread.forEach { batch.update(it.reference, "readBy", FieldValue.arrayUnion(user.id)) }
        batch.commit().await()
        Unit
    }

    companion object {
        const val COLLECTION = "threads"
        const val MESSAGES = "messages"
    }
}
