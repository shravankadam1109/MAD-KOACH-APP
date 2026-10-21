package com.koach.studio.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachTextButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.ui.formatRelative
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val today: List<AppNotification> = emptyList(),
    val earlier: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val now: LocalDateTime = LocalDateTime.now()
)

class NotificationsViewModel(
    private val notifications: NotificationRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = notifications.observeNotifications().map { list ->
        val now = LocalDateTime.now(clock)
        val (today, earlier) = list.partition { it.createdAt.toLocalDate() == LocalDate.now(clock) }
        NotificationsUiState(false, today, earlier, list.count { !it.isRead }, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun open(notification: AppNotification) {
        viewModelScope.launch { notifications.markRead(notification.id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notifications.markAllRead() }
    }
}

private val NotificationType.icon: ImageVector
    get() = when (this) {
        NotificationType.PROJECT -> Icons.Outlined.Movie
        NotificationType.DELIVERABLE -> Icons.Outlined.PlayCircle
        NotificationType.MESSAGE -> Icons.Outlined.ChatBubbleOutline
        NotificationType.BOOKING -> Icons.AutoMirrored.Outlined.EventNote
        NotificationType.PAYMENT -> Icons.AutoMirrored.Outlined.ReceiptLong
        NotificationType.TASK -> Icons.Outlined.Checklist
        NotificationType.FEEDBACK -> Icons.Outlined.RateReview
    }

private val NotificationType.tone: Tone
    get() = when (this) {
        NotificationType.MESSAGE -> Tone.Info
        NotificationType.DELIVERABLE, NotificationType.PROJECT -> Tone.Review
        NotificationType.PAYMENT, NotificationType.BOOKING -> Tone.Warning
        NotificationType.TASK -> Tone.Brand
        NotificationType.FEEDBACK -> Tone.Danger
    }

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
    onOpenTarget: (AppNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    Scaffold(
        containerColor = c.background,
        topBar = {
            KoachTopBar(
                title = "Notifications",
                subtitle = if (state.unreadCount > 0) "${state.unreadCount} unread" else "You're all caught up",
                onBack = onBack
            ) {
                if (state.unreadCount > 0) {
                    KoachTextButton("Mark all read", onClick = viewModel::markAllRead, modifier = Modifier.testTag("mark_all_read"))
                }
            }
        },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = gutter, end = gutter, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("notification_list")
        ) {
            if (state.today.isEmpty() && state.earlier.isEmpty()) {
                item { EmptyState(Icons.Outlined.NotificationsNone, "No notifications", "Updates about your projects will show up here.") }
            }
            if (state.today.isNotEmpty()) {
                item { Eyebrow("Today", modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) }
                items(state.today, key = { it.id }) { NotificationRow(it, state.now) { viewModel.open(it); onOpenTarget(it) } }
            }
            if (state.earlier.isNotEmpty()) {
                item { Eyebrow("Earlier", color = c.textTertiary, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)) }
                items(state.earlier, key = { it.id }) { NotificationRow(it, state.now) { viewModel.open(it); onOpenTarget(it) } }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, now: LocalDateTime, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_${notification.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = (if (notification.isRead) "" else "Unread. ") + "${notification.title}. ${notification.body}"
            }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(notification.type.icon, tone = notification.type.tone, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (notification.isRead) c.textSecondary else c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatRelative(notification.createdAt, now), style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                }
                Text(notification.body, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(c.brand).testTag("unread_dot_${notification.id}"))
            }
        }
    }
}
