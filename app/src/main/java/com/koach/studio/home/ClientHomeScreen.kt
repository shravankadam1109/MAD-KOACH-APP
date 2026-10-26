package com.koach.studio.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Approval
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.PortfolioItem
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
    val onContact: () -> Unit = {},
    val onOpenService: (String) -> Unit = {},
    val onOpenPortfolioItem: (String) -> Unit = {}
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
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("client_home")
        ) {
            // 1. Compact welcome & header area
            item { Header(state, actions.onNotifications) }

            // 2. Key project statistics cards (Section 3.1)
            item {
                ClientStatisticsRow(state, modifier = Modifier.padding(horizontal = gutter))
            }

            // 3. Current or active work (Featured project card)
            item {
                val featured = state.featuredProject
                if (featured != null) {
                    FeaturedProjectCard(featured, state, actions, modifier = Modifier.padding(horizontal = gutter))
                } else {
                    KoachCard(highlighted = true, modifier = Modifier.padding(horizontal = gutter).fillMaxWidth()) {
                        EmptyState(
                            icon = Icons.Outlined.Movie,
                            title = "No active projects",
                            message = "Ready to create something great? Explore services below and start a project.",
                            actionLabel = "Explore services",
                            onAction = actions.onBook
                        )
                    }
                }
            }

            // 4. Pending approvals or actions
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

            // 5. Project progress overview & visual status breakdown graph (Section 3.2)
            if (state.statusBreakdown.total > 0) {
                item {
                    SectionHeader("Project Progress", eyebrow = "Status Breakdown")
                    Spacer(Modifier.height(10.dp))
                    ProjectProgressOverviewCard(
                        breakdown = state.statusBreakdown,
                        avgProgress = state.averageProgressPercent,
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                }
            }

            // 6. Explore KOACH services and portfolio (Section 3.3)
            if (state.services.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Explore KOACH services",
                        eyebrow = "Studio Capabilities",
                        actionLabel = "All services",
                        onAction = actions.onBook
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("home_explore_services")
                    ) {
                        items(state.services, key = { it.id }) { service ->
                            ExploreServiceCard(
                                service = service,
                                onClick = { actions.onOpenService(service.id) }
                            )
                        }
                    }
                }
            }

            if (state.portfolio.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Featured portfolio",
                        eyebrow = "Recent Work",
                        actionLabel = "Browse all",
                        onAction = actions.onBook
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("home_explore_portfolio")
                    ) {
                        items(state.portfolio.take(4), key = { it.id }) { item ->
                            FeaturedPortfolioCard(
                                item = item,
                                onClick = { actions.onOpenPortfolioItem(item.id) }
                            )
                        }
                    }
                }
            }

            // 7. Relevant recent activity / Upcoming milestones
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
                    SectionHeader(
                        title = "Your projects",
                        eyebrow = "All active",
                        actionLabel = "View all",
                        onAction = actions.onAllProjects
                    )
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
        }
    }
}

/** 4 Compact readable metric cards displaying actual data. */
@Composable
private fun ClientStatisticsRow(state: ClientHomeUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().testTag("client_stat_cards"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Active",
            value = state.activeProjectsCount.toString(),
            subtitle = "Projects",
            tone = Tone.Brand,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Approvals",
            value = state.pendingApprovalsCount.toString(),
            subtitle = "Waiting",
            tone = Tone.Review,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Ready",
            value = state.readyDeliverablesCount.toString(),
            subtitle = "Downloads",
            tone = Tone.Info,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Deadlines",
            value = state.upcomingDeadlinesCount.toString(),
            subtitle = "Next 14d",
            tone = Tone.Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    tone: Tone,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    KoachCard(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = tone.color(),
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = c.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                maxLines = 1
            )
        }
    }
}

/** Visual summary graph of project status breakdown using KOACH theme palette. */
@Composable
private fun ProjectProgressOverviewCard(
    breakdown: ProjectStatusBreakdown,
    avgProgress: Int,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val total = breakdown.total.coerceAtLeast(1)

    KoachCard(
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth().testTag("project_status_overview")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Overall Progress", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text("$avgProgress% avg completion", style = MaterialTheme.typography.labelMedium, color = c.brand)
        }
        Spacer(Modifier.height(12.dp))

        // Segmented visual bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.surfaceRaised)
        ) {
            if (breakdown.completedCount > 0) {
                Box(Modifier.weight(breakdown.completedCount.toFloat() / total).height(8.dp).background(c.brand))
            }
            if (breakdown.inReviewCount > 0) {
                Box(Modifier.weight(breakdown.inReviewCount.toFloat() / total).height(8.dp).background(Tone.Review.color()))
            }
            if (breakdown.revisionCount > 0) {
                Box(Modifier.weight(breakdown.revisionCount.toFloat() / total).height(8.dp).background(Tone.Warning.color()))
            }
            if (breakdown.inProgressCount > 0) {
                Box(Modifier.weight(breakdown.inProgressCount.toFloat() / total).height(8.dp).background(Tone.Info.color()))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusLegendItem(label = "Done", count = breakdown.completedCount, color = c.brand)
            StatusLegendItem(label = "Review", count = breakdown.inReviewCount, color = Tone.Review.color())
            StatusLegendItem(label = "Revisions", count = breakdown.revisionCount, color = Tone.Warning.color())
            StatusLegendItem(label = "In Progress", count = breakdown.inProgressCount, color = Tone.Info.color())
        }
    }
}

@Composable
private fun StatusLegendItem(label: String, count: Int, color: Color) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text("$label: $count", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}

@Composable
private fun ExploreServiceCard(service: CreativeService, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        modifier = Modifier.width(220.dp).testTag("service_${service.id}")
    ) {
        StatusPill(service.category.displayName.uppercase(), Tone.Info)
        Spacer(Modifier.height(10.dp))
        Text(service.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(service.tagline, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(36.dp))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("From ₹${service.startingPriceInr}", style = MaterialTheme.typography.labelMedium, color = c.brand)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FeaturedPortfolioCard(item: PortfolioItem, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        modifier = Modifier.width(220.dp).testTag("portfolio_${item.id}")
    ) {
        StatusPill(item.category.displayName.uppercase(), Tone.Brand)
        Spacer(Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(item.summary, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(36.dp))
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
                    .background(c.brandContainer)
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
