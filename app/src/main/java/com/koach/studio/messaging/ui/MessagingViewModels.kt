package com.koach.studio.messaging.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.User
import com.koach.studio.messaging.data.MessageRepository
import com.koach.studio.messaging.data.MessageRules
import com.koach.studio.messaging.data.ThreadSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

data class ThreadListUiState(
    val isLoading: Boolean = true,
    val threads: List<ThreadSummary> = emptyList(),
    val senderNames: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val now: LocalDateTime = LocalDateTime.now()
)

class ThreadListViewModel(
    messages: MessageRepository,
    people: List<User>,
    private val clock: Clock
) : ViewModel() {
    private val names = people.associate { it.id to it.firstName }

    val uiState: StateFlow<ThreadListUiState> = messages.observeThreads()
        .map { ThreadListUiState(isLoading = false, threads = it, senderNames = names, now = LocalDateTime.now(clock)) }
        .catch { emit(ThreadListUiState(isLoading = false, errorMessage = it.message ?: "Couldn't load conversations")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadListUiState())
}

data class ConversationUiState(
    val isLoading: Boolean = true,
    val thread: MessageThread? = null,
    val messages: List<Message> = emptyList(),
    val participants: List<User> = emptyList(),
    val currentUserId: String = "",
    val draft: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    val canSend: Boolean get() = draft.isNotBlank() && draft.length <= MessageRules.MAX_LENGTH && !isSending
}

class ConversationViewModel(
    private val threadId: String,
    private val user: User,
    private val messages: MessageRepository,
    private val people: List<User>
) : ViewModel() {

    private val compose = MutableStateFlow(Triple("", false, null as String?))

    val uiState: StateFlow<ConversationUiState> =
        combine(messages.observeThread(threadId), messages.observeMessages(threadId), compose) { thread, list, (draft, sending, error) ->
            ConversationUiState(
                isLoading = false,
                thread = thread,
                messages = list,
                participants = thread?.participantIds.orEmpty().mapNotNull { id -> people.find { it.id == id } },
                currentUserId = user.id,
                draft = draft,
                isSending = sending,
                errorMessage = error
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationUiState())

    init {
        markRead()
    }

    fun onDraftChange(value: String) = compose.update { it.copy(first = value.take(MessageRules.MAX_LENGTH), third = null) }

    fun send() {
        val (draft, sending, _) = compose.value
        if (sending || draft.isBlank()) return
        viewModelScope.launch {
            compose.update { it.copy(second = true) }
            val result = messages.sendMessage(threadId, draft)
            compose.value = if (result.isSuccess) Triple("", false, null) else Triple(draft, false, result.exceptionOrNull()?.message)
        }
    }

    /** Called on open and whenever new messages arrive while the screen is visible. */
    fun markRead() {
        viewModelScope.launch { messages.markThreadRead(threadId) }
    }

    fun senderName(senderId: String): String = people.find { it.id == senderId }?.name ?: "Unknown"
}
