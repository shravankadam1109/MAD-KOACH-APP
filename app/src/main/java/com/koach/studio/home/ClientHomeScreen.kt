package com.koach.studio.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.catalog.ui.icon
import com.koach.studio.core.designsystem.components.BadgedIconButton
import com.koach.studio.core.designsystem.components.DividedList
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.JourneyStep
import com.koach.studio.core.designsystem.components.JourneyTracker
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.PriorityRow
import com.koach.studio.core.designsystem.components.QuietHeader
import com.koach.studio.core.designsystem.components.ShortcutButton
import com.koach.studio.core.designsystem.components.StepState
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.entrance
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.PortfolioItem
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatDue
import com.koach.studio.core.ui.formatInr
import com.koach.studio.portfolio.PortfolioCover
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

/**
 * Client home, ordered by what a client wants to know: how their current
 * project is going, what's waiting on them, then — clearly set apart —
 * what else KOACH makes. Totals and breakdowns live on the project screens.
 */
@Composable
fun ClientHomeScreen(viewModel: ClientHomeViewModel, actions: ClientHomeActions, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.xl

    KoachBackground(modifier.fillMaxSize()) {
        if (state.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        val featured = state.featuredProject
        val otherProjects = state.activeProjects.filter { it.id != featured?.id }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 40.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("client_home")
        ) {
            item { Header(state, actions.onNotifications, Modifier.padding(horizontal = gutter).entrance(0)) }

            item {
                Column(Modifier.padding(horizontal = gutter).entrance(1)) {
                    if (featured != null) {
                        CurrentProject(featured, state, actions)
                    } else {
                        Spacer(Modifier.height(28.dp))
                        EmptyState(
                            icon = Icons.Outlined.Movie,
                            title = "No active projects",
                            message = "Ready to create something? Explore what KOACH makes below and start a project.",
                            actionLabel = "Book a project",
                            onAction = actions.onBook
                        )
                    }
                }
            }

            item {
                Row(Modifier.padding(horizontal = gutter, vertical = 8.dp).padding(top = 20.dp).entrance(2)) {
                    ShortcutButton(Icons.Outlined.ChatBubbleOutline, "Message", actions.onMessages, Modifier.weight(1f).testTag("hero_message"))
                    ShortcutButton(Icons.Outlined.VideoCall, "Book", actions.onBook, Modifier.weight(1f).testTag("hero_book"))
                    ShortcutButton(Icons.AutoMirrored.Outlined.ReceiptLong, "Invoices", actions.onPayments, Modifier.weight(1f).testTag("hero_invoices"))
                }
            }

            if (state.awaitingReview.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = gutter).padding(top = 28.dp)) {
                        QuietHeader(
                            if (state.awaitingReview.size == 1) "Ready for your review" else "${state.awaitingReview.size} ready for your review",
                            actionLabel = if (state.awaitingReview.size > 2) "View all" else null,
                            onAction = actions.onAllProjects
                        )
                        DividedList(state.awaitingReview.take(2)) { review ->
                            PriorityRow(
                                title = review.deliverable.name,
                                meta = "${review.project.title} · ${review.deliverable.version}",
                                trailing = "Review",
                                trailingColor = c.review,
                                dot = c.review,
                                onClick = { actions.onOpenProject(review.project.id) }
                            )
                        }
                    }
                }
            }

            if (otherProjects.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = gutter).padding(top = 28.dp)) {
                        QuietHeader("Your other projects", actionLabel = "All projects", onAction = actions.onAllProjects)
                        DividedList(otherProjects.take(3)) { project ->
                            PriorityRow(
                                title = project.title,
                                meta = "${project.status.displayName} · ${project.serviceType.displayName}",
                                trailing = formatDue(project.dueDate, state.today).removePrefix("Due ").replaceFirstChar { it.uppercase() },
                                dot = project.status.tone.color(),
                                progress = project.progress,
                                onClick = { actions.onOpenProject(project.id) }
                            )
                        }
                    }
                }
            }

            // Explore: a deliberate change of register from "your work" to "our work".
            if (state.portfolio.isNotEmpty() || state.services.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = gutter).padding(top = 44.dp, bottom = 18.dp)) {
                        KoachDivider()
                        Spacer(Modifier.height(32.dp))
                        Eyebrow("Explore KOACH")
                        Spacer(Modifier.height(8.dp))
                        Text("Work that moves.", style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Recent client work, and the services behind it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary
                        )
                    }
                }
            }

            if (state.portfolio.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = gutter),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("home_explore_portfolio")
                    ) {
                        items(state.portfolio.take(6), key = { it.id }) { item ->
                            PortfolioPoster(item, onClick = { actions.onOpenPortfolioItem(item.id) })
                        }
                    }
                }
            }

            if (state.services.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = gutter).padding(top = 32.dp).testTag("home_explore_services")) {
                        QuietHeader("Services", actionLabel = "All services", onAction = actions.onBook)
                        DividedList(state.services.take(4)) { service ->
                            ServiceRow(service, onClick = { actions.onOpenService(service.id) })
                        }
                        Spacer(Modifier.height(20.dp))
                        KoachPrimaryButton("Start a project", onClick = actions.onBook, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: ClientHomeUiState, onNotifications: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.weight(1f)) {
            Eyebrow(state.greeting)
            Spacer(Modifier.height(6.dp))
            Text(state.firstName, style = MaterialTheme.typography.headlineLarge, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/**
 * The client's current project as an open editorial block: what it is, how
 * far along, and its real milestones as a journey — not a stack of cards.
 */
@Composable
private fun CurrentProject(project: Project, state: ClientHomeUiState, actions: ClientHomeActions) {
    val c = KoachTheme.colors
    val percent = (project.progress * 100).toInt()
    val milestones = project.milestones.sortedBy { it.dueDate }
    val current = milestones.indexOfFirst { !it.completed }
    val next = milestones.getOrNull(current)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .clickable(onClickLabel = "Open ${project.title}") { actions.onOpenProject(project.id) }
            .testTag("featured_project")
            .semantics(mergeDescendants = true) { contentDescription = "${project.title}, $percent percent complete, ${project.status.displayName}" }
    ) {
        Text(
            "${project.status.displayName} · ${project.serviceType.displayName}",
            style = MaterialTheme.typography.labelLarge,
            color = project.status.tone.color()
        )
        Spacer(Modifier.height(8.dp))
        Text(project.title, style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$percent%", style = MaterialTheme.typography.displayMedium, color = c.brand)
            Spacer(Modifier.width(10.dp))
            Text(
                "complete · ${formatDue(project.dueDate, state.today).replaceFirstChar { it.lowercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = 7.dp)
            )
        }
        if (milestones.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            JourneyTracker(
                steps = milestones.mapIndexed { index, m ->
                    JourneyStep(
                        label = m.title,
                        state = when {
                            m.completed -> StepState.Complete
                            index == current -> StepState.Active
                            else -> StepState.Upcoming
                        }
                    )
                },
                // The green runs to the current milestone.
                fill = if (milestones.size < 2) 0f else (if (current < 0) milestones.lastIndex else current) / milestones.lastIndex.toFloat(),
                description = "Milestones: " + milestones.joinToString { "${it.title} ${if (it.completed) "done" else "to do"}" },
                modifier = Modifier.testTag("project_journey")
            )
        }
        if (next != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Next: ${next.title} · ${formatDate(next.dueDate, state.today)}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
        }
    }
}

/** Portrait artwork card for the Explore carousel. */
@Composable
private fun PortfolioPoster(item: PortfolioItem, onClick: () -> Unit) {
    val c = KoachTheme.colors
    Column(
        Modifier
            .width(188.dp)
            .clickable(onClickLabel = "View ${item.title}", onClick = onClick)
            .testTag("portfolio_${item.id}")
    ) {
        PortfolioCover(item, aspect = 0.8f, iconSize = 40.dp)
        Spacer(Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.category.displayName, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
    }
}

@Composable
private fun ServiceRow(service: CreativeService, onClick: () -> Unit) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)
    ) {
        IconTile(service.category.icon, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(service.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(service.tagline, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Text("From ${formatInr(service.startingPriceInr)}", style = MaterialTheme.typography.labelMedium, color = c.brand)
    }
}
