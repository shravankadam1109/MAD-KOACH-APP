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

enum class MessageTab(val label: String) {
    DM("DM"),
    PROJECT("Team / Project")
}

data class ThreadListUiState(
    val isLoading: Boolean = true,
    val selectedTab: MessageTab = MessageTab.DM,
    val threads: List<ThreadSummary> = emptyList(),
    val senderNames: Map<String, String> = emptyMap(),
    val currentUserId: String = "",
    val errorMessage: String? = null,
    val now: LocalDateTime = LocalDateTime.now()
) {
    val filteredThreads: List<ThreadSummary>
        get() = threads.filter { summary ->
            if (selectedTab == MessageTab.DM) summary.thread.type == com.koach.studio.core.model.ThreadType.DM
            else summary.thread.type == com.koach.studio.core.model.ThreadType.PROJECT
        }
    val dmUnreadCount: Int
        get() = threads.filter { it.thread.type == com.koach.studio.core.model.ThreadType.DM }.sumOf { it.unreadCount }
    val projectUnreadCount: Int
        get() = threads.filter { it.thread.type == com.koach.studio.core.model.ThreadType.PROJECT }.sumOf { it.unreadCount }
}

class ThreadListViewModel(
    messages: MessageRepository,
    val people: List<User>,
    private val clock: Clock,
    currentUser: User? = null
) : ViewModel() {
    private val names = people.associate { it.id to it.firstName }
    private val currentUid = currentUser?.id.orEmpty()
    private val _selectedTab = MutableStateFlow(MessageTab.DM)

    val uiState: StateFlow<ThreadListUiState> = combine(
        messages.observeThreads(),
        _selectedTab
    ) { threads, tab ->
        ThreadListUiState(
            isLoading = false,
            selectedTab = tab,
            threads = threads,
            senderNames = names,
            currentUserId = currentUid,
            now = LocalDateTime.now(clock)
        )
    }.catch {
        emit(ThreadListUiState(isLoading = false, errorMessage = it.message ?: "Couldn't load conversations"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadListUiState())

    fun selectTab(tab: MessageTab) {
        _selectedTab.value = tab
    }
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
