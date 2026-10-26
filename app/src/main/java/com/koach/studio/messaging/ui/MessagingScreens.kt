package com.koach.studio.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.CountBadge
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.koachTextFieldColors
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Message
import com.koach.studio.core.model.User
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatRelative
import com.koach.studio.core.ui.formatTime
import com.koach.studio.messaging.data.ThreadSummary
import java.time.LocalDate

/** Conversation list with DM and Team / Project tabs. [onBack] is null when shown as a top-level tab. */
@Composable
fun ThreadListScreen(
    viewModel: ThreadListViewModel,
    onOpenThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    KoachBackground(modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding()) {
            if (onBack != null) KoachTopBar(title = "Messages", onBack = onBack)
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> ErrorState(state.errorMessage!!)
                else -> {
                    val dmTabLabel = if (state.dmUnreadCount > 0) "DM (${state.dmUnreadCount})" else "DM"
                    val projTabLabel = if (state.projectUnreadCount > 0) "Team / Project (${state.projectUnreadCount})" else "Team / Project"
                    val displayedList = state.filteredThreads

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("thread_list")
                    ) {
                        if (onBack == null) {
                            item {
                                Column(Modifier.padding(start = gutter, end = gutter, top = 16.dp, bottom = 4.dp)) {
                                    Eyebrow("Studio & Client Messages")
                                    Spacer(Modifier.height(6.dp))
                                    Text("Messages", style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                                }
                            }
                        }

                        item {
                            KoachSegmentedControl(
                                options = listOf(dmTabLabel, projTabLabel),
                                selectedIndex = if (state.selectedTab == MessageTab.DM) 0 else 1,
                                onSelect = { idx -> viewModel.selectTab(if (idx == 0) MessageTab.DM else MessageTab.PROJECT) },
                                modifier = Modifier
                                    .padding(horizontal = gutter, vertical = 6.dp)
                                    .fillMaxWidth()
                                    .testTag("messages_segmented_tabs")
                            )
                        }

                        if (displayedList.isEmpty()) {
                            item {
                                if (state.selectedTab == MessageTab.DM) {
                                    EmptyState(
                                        Icons.Outlined.Person,
                                        "No direct messages",
                                        "Private 1-on-1 conversations between you and your studio lead appear here."
                                    )
                                } else {
                                    EmptyState(
                                        Icons.Outlined.ChatBubbleOutline,
                                        "No project conversations",
                                        "Project discussions and team milestone threads appear here."
                                    )
                                }
                            }
                        }
                        items(displayedList, key = { it.thread.id }) { summary ->
                            ThreadCard(
                                summary = summary,
                                state = state,
                                people = viewModel.people,
                                onClick = { onOpenThread(summary.thread.id) },
                                modifier = Modifier.padding(horizontal = gutter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(
    summary: ThreadSummary,
    state: ThreadListUiState,
    people: List<User>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val unread = summary.unreadCount > 0
    val isDm = summary.thread.type == com.koach.studio.core.model.ThreadType.DM
    val otherId = summary.thread.participantIds.firstOrNull { it != state.currentUserId } ?: summary.thread.participantIds.firstOrNull()
    val otherUser: User? = if (isDm) people.find { it.id == otherId } else null
    val displayTitle = if (isDm) (otherUser?.name ?: summary.thread.title) else summary.thread.title
    val initials = if (isDm && otherUser != null) otherUser.initials else summary.thread.title.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }

    KoachCard(
        onClick = onClick,
        highlighted = unread,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth().testTag("thread_${summary.thread.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KoachAvatar(initials, size = 46.dp, highlighted = unread)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    summary.lastMessage?.let {
                        Text(
                            formatRelative(it.sentAt, state.now),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (unread) c.brand else c.textTertiary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val previewText = summary.lastMessage?.let {
                        if (isDm) it.body else "${state.senderNames[it.senderId] ?: "Someone"}: ${it.body}"
                    } ?: "No messages yet"
                    Text(
                        previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unread) c.textPrimary else c.textSecondary,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unread) {
                        Spacer(Modifier.width(8.dp))
                        CountBadge(summary.unreadCount)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(viewModel: ConversationViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val listState = rememberLazyListState()

    // Keep the newest message in view and mark incoming messages as read.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
            viewModel.markRead()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = {
            val isDm = state.thread?.type == com.koach.studio.core.model.ThreadType.DM
            val other = state.participants.find { it.id != state.currentUserId } ?: state.participants.firstOrNull()
            val convTitle = if (isDm) (other?.name ?: (state.thread?.title ?: "Direct Message")) else (state.thread?.title ?: "Conversation")
            val convSub = if (isDm) other?.title?.ifBlank { "Creative Crew" } else state.participants.joinToString(", ") { it.firstName }.ifBlank { null }
            KoachTopBar(
                title = convTitle,
                subtitle = convSub,
                onBack = onBack
            )
        },
        bottomBar = {
            if (state.thread != null) {
                Composer(
                    draft = state.draft,
                    canSend = state.canSend,
                    error = state.errorMessage,
                    onDraftChange = viewModel::onDraftChange,
                    onSend = viewModel::send
                )
            }
        },
        modifier = modifier.imePadding()
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.thread == null -> ErrorState("This conversation isn't available.", Modifier.padding(padding))
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = KoachTheme.spacing.gutter,
                    end = KoachTheme.spacing.gutter,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize().testTag("message_list")
            ) {
                if (state.messages.isEmpty()) {
                    item { EmptyState(Icons.Outlined.ChatBubbleOutline, "Say hello", "Start the conversation about this project.") }
                }
                items(state.messages.size, key = { state.messages[it].id }) { index ->
                    val message = state.messages[index]
                    val previous = state.messages.getOrNull(index - 1)
                    val newDay = previous == null || previous.sentAt.toLocalDate() != message.sentAt.toLocalDate()
                    if (newDay) DaySeparator(message.sentAt.toLocalDate())
                    val mine = message.senderId == state.currentUserId
                    val showName = !mine && (newDay || previous?.senderId != message.senderId)
                    MessageBubble(message, mine, if (showName) viewModel.senderName(message.senderId) else null)
                }
            }
        }
    }
}

@Composable
private fun DaySeparator(date: LocalDate) {
    val today = LocalDate.now()
    Text(
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> formatDate(date, today)
        },
        style = MaterialTheme.typography.labelMedium,
        color = KoachTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
    )
}

@Composable
private fun MessageBubble(message: Message, mine: Boolean, senderName: String?) {
    val c = KoachTheme.colors
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (mine) 18.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 18.dp
    )
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${if (mine) "You" else senderName ?: "Message"}: ${message.body}, ${formatTime(message.sentAt)}"
            }
    ) {
        if (senderName != null) {
            Text(senderName, style = MaterialTheme.typography.labelMedium, color = c.brand, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp, top = 6.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (mine) c.brand else c.surfaceRaised)
                .then(if (mine) Modifier else Modifier.border(1.dp, c.outlineSubtle, shape))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.body, style = MaterialTheme.typography.bodyMedium, color = if (mine) c.onBrand else c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                formatTime(message.sentAt),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing),
                color = if (mine) c.onBrand.copy(alpha = 0.6f) else c.textTertiary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun Composer(draft: String, canSend: Boolean, error: String?, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    val c = KoachTheme.colors
    Column(
        Modifier
            .background(c.background)
            .navigationBarsPadding()
            .padding(horizontal = KoachTheme.spacing.gutter, vertical = 10.dp)
    ) {
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = c.danger, modifier = Modifier.padding(bottom = 6.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("Write a message") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                colors = koachTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("message_input")
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.padding(bottom = 4.dp)) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = c.brand,
                        contentColor = c.onBrand,
                        disabledContainerColor = c.surfaceHigh,
                        disabledContentColor = c.textTertiary
                    ),
                    modifier = Modifier.size(48.dp).clip(CircleShape).testTag("send_message")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
