package com.koach.studio.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.PortfolioItem
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Milestone
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class UpcomingMilestone(val milestone: Milestone, val project: Project)
data class ReviewItem(val deliverable: Deliverable, val project: Project)

data class ProjectStatusBreakdown(
    val inProgressCount: Int = 0,
    val inReviewCount: Int = 0,
    val revisionCount: Int = 0,
    val completedCount: Int = 0
) {
    val total: Int get() = inProgressCount + inReviewCount + revisionCount + completedCount
}

data class ClientHomeUiState(
    val isLoading: Boolean = true,
    val greeting: String = "Hello",
    val firstName: String = "",
    val initials: String = "",
    val unreadNotifications: Int = 0,
    val featuredProject: Project? = null,
    val activeProjects: List<Project> = emptyList(),
    val awaitingReview: List<ReviewItem> = emptyList(),
    val upcomingMilestones: List<UpcomingMilestone> = emptyList(),
    val completedCount: Int = 0,
    val today: LocalDate = LocalDate.now(),
    // Statistics
    val activeProjectsCount: Int = 0,
    val pendingApprovalsCount: Int = 0,
    val readyDeliverablesCount: Int = 0,
    val upcomingDeadlinesCount: Int = 0,
    val pendingChangeRequestsCount: Int = 0,
    val statusBreakdown: ProjectStatusBreakdown = ProjectStatusBreakdown(),
    val averageProgressPercent: Int = 0,
    // Explore section
    val services: List<CreativeService> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList()
)

class ClientHomeViewModel(
    private val user: User,
    projectRepository: ProjectRepository,
    notificationRepository: NotificationRepository,
    private val clock: Clock,
    catalogRepository: CatalogRepository? = null
) : ViewModel() {

    private val servicesFlow: Flow<List<CreativeService>> = catalogRepository?.observeServices() ?: flowOf(emptyList())
    private val portfolioFlow: Flow<List<PortfolioItem>> = catalogRepository?.observePortfolio() ?: flowOf(emptyList())

    val uiState: StateFlow<ClientHomeUiState> = combine(
        projectRepository.getProjects(),
        notificationRepository.observeUnreadCount(),
        servicesFlow,
        portfolioFlow
    ) { projects, unread, services, portfolio ->
        val today = LocalDate.now(clock)
        val active = projects
            .filter { it.status != ProjectStatus.COMPLETED }
            .sortedBy { it.dueDate }

        val awaitingReview = projects.flatMap { p ->
            p.deliverables.filter { it.status == DeliverableStatus.IN_REVIEW }.map { ReviewItem(it, p) }
        }

        val readyDeliverables = projects.flatMap { p ->
            p.deliverables.filter { it.status == DeliverableStatus.APPROVED }
        }.size

        val upcomingDeadlines = active.count {
            !it.dueDate.isBefore(today) && it.dueDate.isBefore(today.plusDays(15))
        }

        val pendingChangeRequests = projects.count { it.status == ProjectStatus.REVISION_REQUESTED }

        val breakdown = ProjectStatusBreakdown(
            inProgressCount = projects.count { it.status == ProjectStatus.IN_PROGRESS },
            inReviewCount = projects.count { it.status == ProjectStatus.IN_REVIEW },
            revisionCount = projects.count { it.status == ProjectStatus.REVISION_REQUESTED },
            completedCount = projects.count { it.status == ProjectStatus.COMPLETED }
        )

        val totalMilestones = active.sumOf { it.milestones.size }
        val completedMilestones = active.sumOf { p -> p.milestones.count { it.completed } }
        val avgProgress = if (totalMilestones > 0) (completedMilestones * 100) / totalMilestones else 0

        ClientHomeUiState(
            isLoading = false,
            greeting = greetingFor(LocalTime.now(clock)),
            firstName = user.firstName,
            initials = user.initials,
            unreadNotifications = unread,
            featuredProject = active.firstOrNull { it.status == ProjectStatus.IN_REVIEW || it.status == ProjectStatus.REVISION_REQUESTED }
                ?: active.firstOrNull(),
            activeProjects = active,
            awaitingReview = awaitingReview,
            upcomingMilestones = active
                .flatMap { p -> p.milestones.filter { !it.completed }.map { UpcomingMilestone(it, p) } }
                .sortedBy { it.milestone.dueDate }
                .take(4),
            completedCount = projects.count { it.status == ProjectStatus.COMPLETED },
            today = today,
            activeProjectsCount = active.size,
            pendingApprovalsCount = awaitingReview.size,
            readyDeliverablesCount = readyDeliverables,
            upcomingDeadlinesCount = upcomingDeadlines,
            pendingChangeRequestsCount = pendingChangeRequests,
            statusBreakdown = breakdown,
            averageProgressPercent = avgProgress,
            services = services,
            portfolio = portfolio
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientHomeUiState())

    companion object {
        fun greetingFor(time: LocalTime): String = when (time.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
