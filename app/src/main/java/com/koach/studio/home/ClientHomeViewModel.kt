package com.koach.studio.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Milestone
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class UpcomingMilestone(val milestone: Milestone, val project: Project)
data class ReviewItem(val deliverable: Deliverable, val project: Project)

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
    val today: LocalDate = LocalDate.now()
)

class ClientHomeViewModel(
    private val user: User,
    projectRepository: ProjectRepository,
    notificationRepository: NotificationRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<ClientHomeUiState> = combine(
        projectRepository.getProjects(),
        notificationRepository.observeUnreadCount()
    ) { projects, unread ->
        val today = LocalDate.now(clock)
        val active = projects
            .filter { it.status != ProjectStatus.COMPLETED }
            .sortedBy { it.dueDate }
        ClientHomeUiState(
            isLoading = false,
            greeting = greetingFor(LocalTime.now(clock)),
            firstName = user.firstName,
            initials = user.initials,
            unreadNotifications = unread,
            featuredProject = active.firstOrNull { it.status == ProjectStatus.IN_REVIEW || it.status == ProjectStatus.REVISION_REQUESTED }
                ?: active.firstOrNull(),
            activeProjects = active,
            awaitingReview = projects.flatMap { p ->
                p.deliverables.filter { it.status == DeliverableStatus.IN_REVIEW }.map { ReviewItem(it, p) }
            },
            upcomingMilestones = active
                .flatMap { p -> p.milestones.filter { !it.completed }.map { UpcomingMilestone(it, p) } }
                .sortedBy { it.milestone.dueDate }
                .take(4),
            completedCount = projects.count { it.status == ProjectStatus.COMPLETED },
            today = today
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
