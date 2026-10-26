package com.koach.studio.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.BadgedIconButton
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachGlassPanel
import com.koach.studio.core.designsystem.components.KoachHeroCard
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.QuickActionTile
import com.koach.studio.core.designsystem.components.SectionHeader
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatDue
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.ui.tone

data class ClientHomeActions(
    val onOpenProject: (String) -> Unit,
    val onAllProjects: () -> Unit,
    val onBook: () -> Unit,
    val onMessages: () -> Unit,
    val onPayments: () -> Unit,
    val onNotifications: () -> Unit,
    val onContact: () -> Unit = {}
)

@Composable
fun ClientHomeScreen(viewModel: ClientHomeViewModel, actions: ClientHomeActions, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val gutter = KoachTheme.spacing.gutter

    KoachBackground(modifier.fillMaxSize()) {
        if (state.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("client_home")
        ) {
            item { Header(state, actions.onNotifications) }

            item {
                val featured = state.featuredProject
                if (featured != null) {
                    FeaturedProjectCard(featured, state, actions, modifier = Modifier.padding(horizontal = gutter))
                } else {
                    KoachCard(highlighted = true, modifier = Modifier.padding(horizontal = gutter).fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Outlined.Movie,
                            title = "No active projects",
                            message = "Book a service and your project will appear here.",
                            actionLabel = "Explore services",
                            onAction = actions.onBook
                        )
                    }
                }
            }

            if (state.featuredProject == null) item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = gutter)
                ) {
                    QuickActionTile(Icons.Outlined.VideoCall, "Book a\nservice", actions.onBook, Modifier.weight(1f))
                    QuickActionTile(Icons.Outlined.ChatBubbleOutline, "Message\nyour crew", actions.onMessages, Modifier.weight(1f), tone = Tone.Info)
                    QuickActionTile(Icons.AutoMirrored.Outlined.ReceiptLong, "Invoices &\npayments", actions.onPayments, Modifier.weight(1f), tone = Tone.Review)
                }
            }

            if (state.awaitingReview.isNotEmpty()) {
                item {
                    SectionHeader("Ready for your review", eyebrow = "${state.awaitingReview.size} waiting")
                    Spacer(Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = gutter)
                    ) {
                        state.awaitingReview.take(3).forEach { item ->
                            ReviewRow(item, onClick = { actions.onOpenProject(item.project.id) })
                        }
                    }
                }
            }

            if (state.upcomingMilestones.isNotEmpty()) {
                item {
                    SectionHeader("Coming up", eyebrow = "Milestones")
                    Spacer(Modifier.height(10.dp))
                    KoachCard(modifier = Modifier.padding(horizontal = gutter).fillMaxWidth()) {
                        state.upcomingMilestones.forEachIndexed { index, upcoming ->
                            MilestoneRow(upcoming, state, isLast = index == state.upcomingMilestones.lastIndex)
                        }
                    }
                }
            }

            if (state.activeProjects.size > 1) {
                item {
                    SectionHeader("Your projects", eyebrow = "${state.activeProjects.size} active", actionLabel = "See all", onAction = actions.onAllProjects)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.activeProjects, key = { it.id }) { project ->
                            CompactProjectCard(project, onClick = { actions.onOpenProject(project.id) })
                        }
                    }
                }
            }

            item {
                val c = KoachTheme.colors
                KoachCard(
                    onClick = actions.onContact,
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .fillMaxWidth()
                        .testTag("home_contact_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Phone, tone = Tone.Brand, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Need bespoke production?", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                            Text("Call or email our studio desk in Bandra West", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Contact studio", tint = c.textTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: ClientHomeUiState, onNotifications: () -> Unit) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = KoachTheme.spacing.gutter, end = KoachTheme.spacing.gutter, top = 16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow(state.greeting)
            Spacer(Modifier.height(4.dp))
            Text(state.firstName, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
        }
        BadgedIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            onClick = onNotifications,
            count = state.unreadNotifications,
            modifier = Modifier.testTag("home_notifications")
        )
    }
}

@Composable
private fun FeaturedProjectCard(
    project: Project,
    state: ClientHomeUiState,
    actions: ClientHomeActions,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val next = project.milestones.firstOrNull { !it.completed }
    val pillShape = RoundedCornerShape(KoachRadius.pill)
    KoachHeroCard(
        onClick = { actions.onOpenProject(project.id) },
        modifier = modifier
            .fillMaxWidth()
            .testTag("featured_project")
            .semantics { contentDescription = "Open ${project.title}, ${(project.progress * 100).toInt()} percent complete" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.status.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.brand,
                modifier = Modifier
                    .clip(pillShape)
                    .background(c.brandSoft)
                    .border(1.dp, c.brand.copy(alpha = 0.35f), pillShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = project.serviceType.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = project.title,
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDue(project.dueDate, state.today),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${(project.progress * 100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                color = c.brand
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "complete",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        KoachProgressBar(progress = project.progress, height = 5.dp)
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Next: ${next.title} · ${formatDate(next.dueDate, state.today)}",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroAction(Icons.Outlined.VideoCall, "Book", actions.onBook, Modifier.weight(1f).testTag("hero_book"))
            HeroAction(Icons.Outlined.ChatBubbleOutline, "Message", actions.onMessages, Modifier.weight(1f).testTag("hero_message"))
            HeroAction(Icons.AutoMirrored.Outlined.ReceiptLong, "Invoices", actions.onPayments, Modifier.weight(1f).testTag("hero_invoices"))
        }
    }
}

@Composable
private fun HeroAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    KoachGlassPanel(onClick = onClick, modifier = modifier.semantics(mergeDescendants = true) { role = Role.Button }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = c.brand, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
        }
    }
}

@Composable
private fun ReviewRow(item: ReviewItem, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(onClick = onClick, contentPadding = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.PlayCircle, tone = Tone.Review)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.deliverable.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.project.title} · ${item.deliverable.version}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            StatusPill("Review", Tone.Review, showDot = false)
        }
    }
}

@Composable
private fun MilestoneRow(upcoming: UpcomingMilestone, state: ClientHomeUiState, isLast: Boolean) {
    val c = KoachTheme.colors
    val overdue = upcoming.milestone.dueDate.isBefore(state.today)
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(18.dp)) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (overdue) Tone.Warning.color() else c.brand)
            )
            if (!isLast) {
                Box(Modifier.width(1.dp).height(44.dp).background(c.outline))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 14.dp)) {
            Text(upcoming.milestone.title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(upcoming.project.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            formatDue(upcoming.milestone.dueDate, state.today).removePrefix("Due "),
            style = MaterialTheme.typography.labelMedium,
            color = if (overdue) Tone.Warning.color() else c.textSecondary
        )
    }
}

@Composable
private fun CompactProjectCard(project: Project, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(onClick = onClick, modifier = Modifier.width(240.dp)) {
        StatusPill(project.status.displayName, project.status.tone)
        Spacer(Modifier.height(12.dp))
        Text(project.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(10.dp))
        KoachProgressBar(project.progress, height = 4.dp)
        Spacer(Modifier.height(8.dp))
        Text("${(project.progress * 100).toInt()}% complete", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
    }
}
