package com.koach.studio.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.bookings.data.BookingRepository
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.TaskItem
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.tasks.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

data class TeamMemberWorkload(
    val user: User,
    val openTasks: Int,
    val inProgressTasks: Int,
    val overdueTasks: Int
)

data class StudioDeadlineItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val dueDate: LocalDate,
    val isProject: Boolean,
    val isOverdue: Boolean
)

data class AgencyHomeUiState(
    val isLoading: Boolean = true,
    val greeting: String = "Hello",
    val firstName: String = "",
    val unreadNotifications: Int = 0,
    val activeProjects: Int = 0,
    val completedProjects: Int = 0,
    val inProgressProjects: Int = 0,
    val inReviewProjects: Int = 0,
    val revisionRequestedProjects: Int = 0,
    val averageProgressPercent: Int = 0,
    val pendingBookings: Int = 0,
    val openTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completedTasks: Int = 0,
    val myTasks: List<TaskItem> = emptyList(),
    /** Projects waiting on the studio: revisions requested first, then in review. */
    val needsAttention: List<Project> = emptyList(),
    /** Upcoming project and task deadlines sorted chronologically. */
    val upcomingDeadlines: List<StudioDeadlineItem> = emptyList(),
    /** Task distribution across agency crew members. */
    val teamWorkload: List<TeamMemberWorkload> = emptyList(),
    val projectTitles: Map<String, String> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val isDemo: Boolean = false
)

class AgencyHomeViewModel(
    private val user: User,
    projects: ProjectRepository,
    tasks: TaskRepository,
    bookings: BookingRepository,
    notifications: NotificationRepository,
    private val clock: Clock,
    private val people: List<User> = emptyList(),
    private val isDemo: Boolean = false
) : ViewModel() {

    val uiState: StateFlow<AgencyHomeUiState> = combine(
        projects.getProjects(),
        tasks.observeTasks(),
        bookings.observeBookings(),
        notifications.observeUnreadCount()
    ) { projectList, taskList, bookingList, unread ->
        val today = LocalDate.now(clock)
        val activeProjectList = projectList.filter { it.status != ProjectStatus.COMPLETED }
        val avgProgress = if (activeProjectList.isNotEmpty()) {
            (activeProjectList.map { it.progress }.average() * 100.0).roundToInt()
        } else {
            0
        }
        val projectTitleMap = projectList.associate { it.id to it.title }
        val horizon = today.plusDays(14)

        val projectDeadlines = activeProjectList
            .filter { !it.dueDate.isAfter(horizon) }
            .map { project ->
                StudioDeadlineItem(
                    id = project.id,
                    title = project.title,
                    subtitle = "${project.clientName} · ${project.serviceType.displayName}",
                    dueDate = project.dueDate,
                    isProject = true,
                    isOverdue = project.dueDate.isBefore(today)
                )
            }

        val taskDeadlines = taskList
            .filter { it.status != TaskStatus.DONE && !it.dueDate.isAfter(horizon) }
            .map { task ->
                val projectLabel = task.projectId?.let { projectTitleMap[it] } ?: "Studio"
                StudioDeadlineItem(
                    id = task.id,
                    title = task.title,
                    subtitle = "$projectLabel · ${task.priority.displayName} priority",
                    dueDate = task.dueDate,
                    isProject = false,
                    isOverdue = task.isOverdue(today)
                )
            }

        val combinedDeadlines = (projectDeadlines + taskDeadlines)
            .sortedWith(compareBy<StudioDeadlineItem>({ it.dueDate }, { !it.isProject }))
            .take(5)

        val agencyCrew = people.filter { it.role.isAgency }
        val workload = agencyCrew.map { member ->
            val memberOpen = taskList.filter { it.assigneeId == member.id && it.status != TaskStatus.DONE }
            TeamMemberWorkload(
                user = member,
                openTasks = memberOpen.size,
                inProgressTasks = memberOpen.count { it.status == TaskStatus.IN_PROGRESS || it.status == TaskStatus.IN_REVIEW },
                overdueTasks = memberOpen.count { it.isOverdue(today) }
            )
        }.sortedByDescending { it.openTasks }

        AgencyHomeUiState(
            isLoading = false,
            greeting = ClientHomeViewModel.greetingFor(LocalTime.now(clock)),
            firstName = user.firstName,
            unreadNotifications = unread,
            activeProjects = activeProjectList.size,
            completedProjects = projectList.count { it.status == ProjectStatus.COMPLETED },
            inProgressProjects = projectList.count { it.status == ProjectStatus.IN_PROGRESS },
            inReviewProjects = projectList.count { it.status == ProjectStatus.IN_REVIEW },
            revisionRequestedProjects = projectList.count { it.status == ProjectStatus.REVISION_REQUESTED },
            averageProgressPercent = avgProgress,
            pendingBookings = bookingList.count { it.status == BookingStatus.PENDING },
            openTasks = taskList.count { it.status != TaskStatus.DONE },
            overdueTasks = taskList.count { it.isOverdue(today) },
            completedTasks = taskList.count { it.status == TaskStatus.DONE },
            myTasks = taskList
                .filter { it.assigneeId == user.id && it.status != TaskStatus.DONE }
                .sortedWith(compareBy<TaskItem>({ it.dueDate }, { -it.priority.rank }))
                .take(4),
            needsAttention = projectList
                .filter { it.status == ProjectStatus.REVISION_REQUESTED || it.status == ProjectStatus.IN_REVIEW }
                .sortedBy { if (it.status == ProjectStatus.REVISION_REQUESTED) 0 else 1 },
            upcomingDeadlines = combinedDeadlines,
            teamWorkload = workload,
            projectTitles = projectTitleMap,
            today = today,
            isDemo = isDemo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgencyHomeUiState())
}
