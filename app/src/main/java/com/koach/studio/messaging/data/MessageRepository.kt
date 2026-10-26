package com.koach.studio.messaging.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.NotSignedInException
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

data class ThreadSummary(
    val thread: MessageThread,
    val lastMessage: Message?,
    val unreadCount: Int
)

object MessageRules {
    const val MAX_LENGTH = 1000
}

/**
 * Project conversations. Every read is a Flow so a realtime backend (e.g.
 * Firestore snapshot listeners) can push new messages without UI changes.
 */
interface MessageRepository {
    /** Threads the user can see, most recently active first. */
    fun observeThreads(): Flow<List<ThreadSummary>>
    fun observeThread(threadId: String): Flow<MessageThread?>
    /** Oldest first. */
    fun observeMessages(threadId: String): Flow<List<Message>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun sendMessage(threadId: String, body: String): Result<Message>
    suspend fun markThreadRead(threadId: String): Result<Unit>
}

class InMemoryMessageRepository(
    threads: List<MessageThread>,
    messages: List<Message>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : MessageRepository {

    private val threadStore = MutableStateFlow(threads)
    private val messageStore = MutableStateFlow(messages)

    /** Agency users can open project threads; clients only threads they belong to. Direct messages are strictly private to their participants. */
    private fun canSee(thread: MessageThread, user: User?): Boolean {
        if (user == null) return false
        return if (thread.type == com.koach.studio.core.model.ThreadType.DM) {
            user.id in thread.participantIds
        } else {
            user.role.isAgency || user.id in thread.participantIds
        }
    }

    override fun observeThreads(): Flow<List<ThreadSummary>> =
        combine(threadStore, messageStore, currentUser) { threads, messages, user ->
            threads.filter { canSee(it, user) }.map { thread ->
                val inThread = messages.filter { it.threadId == thread.id }
                ThreadSummary(
                    thread = thread,
                    lastMessage = inThread.maxByOrNull { it.sentAt },
                    unreadCount = inThread.count { user!!.id !in it.readBy && it.senderId != user.id }
                )
            }.sortedByDescending { it.lastMessage?.sentAt ?: LocalDateTime.MIN }
        }

    override fun observeThread(threadId: String): Flow<MessageThread?> =
        combine(threadStore, currentUser) { threads, user -> threads.find { it.id == threadId }?.takeIf { canSee(it, user) } }

    override fun observeMessages(threadId: String): Flow<List<Message>> =
        combine(threadStore, messageStore, currentUser) { threads, messages, user ->
            val thread = threads.find { it.id == threadId }
            if (thread == null || !canSee(thread, user)) emptyList()
            else messages.filter { it.threadId == threadId }.sortedBy { it.sentAt }
        }

    override fun observeUnreadCount(): Flow<Int> = observeThreads().map { list -> list.sumOf { it.unreadCount } }

    override suspend fun sendMessage(threadId: String, body: String): Result<Message> = runCatching {
        val user = access.require(Permission.SEND_MESSAGES)
        val thread = threadStore.value.find { it.id == threadId } ?: throw IllegalArgumentException("Conversation not found")
        if (!canSee(thread, user)) throw PermissionDeniedException("You're not part of this conversation")
        val text = body.trim()
        require(text.isNotEmpty()) { "Write a message first" }
        require(text.length <= MessageRules.MAX_LENGTH) { "Messages can be up to ${MessageRules.MAX_LENGTH} characters" }

        val message = Message(
            id = "m-" + UUID.randomUUID().toString().take(8),
            threadId = threadId,
            senderId = user.id,
            body = text,
            sentAt = LocalDateTime.now(clock),
            readBy = setOf(user.id)
        )
        messageStore.update { it + message }
        // Agency members who reply join project threads so they hear about future messages.
        if (thread.type != com.koach.studio.core.model.ThreadType.DM && user.id !in thread.participantIds) {
            threadStore.update { list -> list.map { if (it.id == threadId) it.copy(participantIds = it.participantIds + user.id) else it } }
        }
        thread.participantIds.filter { it != user.id }.forEach { recipient ->
            notifications.push(
                AppNotification(
                    id = "n-" + UUID.randomUUID().toString().take(8),
                    recipientId = recipient,
                    type = NotificationType.MESSAGE,
                    title = "New message from ${user.firstName}",
                    body = text.take(80),
                    createdAt = message.sentAt,
                    isRead = false,
                    targetId = threadId
                )
            )
        }
        message
    }

    override suspend fun markThreadRead(threadId: String): Result<Unit> = runCatching {
        val user = access.user() ?: throw NotSignedInException()
        messageStore.update { list ->
            list.map { if (it.threadId == threadId && user.id !in it.readBy) it.copy(readBy = it.readBy + user.id) else it }
        }
    }
}
