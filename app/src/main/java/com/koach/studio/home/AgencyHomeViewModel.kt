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

data class AgencyHomeUiState(
    val isLoading: Boolean = true,
    val greeting: String = "Hello",
    val firstName: String = "",
    val unreadNotifications: Int = 0,
    val activeProjects: Int = 0,
    val pendingBookings: Int = 0,
    val openTasks: Int = 0,
    val overdueTasks: Int = 0,
    val myTasks: List<TaskItem> = emptyList(),
    /** Projects waiting on the studio: revisions requested first, then in review. */
    val needsAttention: List<Project> = emptyList(),
    val projectTitles: Map<String, String> = emptyMap(),
    val today: LocalDate = LocalDate.now()
)

class AgencyHomeViewModel(
    private val user: User,
    projects: ProjectRepository,
    tasks: TaskRepository,
    bookings: BookingRepository,
    notifications: NotificationRepository,
    private val clock: Clock
) : ViewModel() {

    val uiState: StateFlow<AgencyHomeUiState> = combine(
        projects.getProjects(),
        tasks.observeTasks(),
        bookings.observeBookings(),
        notifications.observeUnreadCount()
    ) { projectList, taskList, bookingList, unread ->
        val today = LocalDate.now(clock)
        AgencyHomeUiState(
            isLoading = false,
            greeting = ClientHomeViewModel.greetingFor(LocalTime.now(clock)),
            firstName = user.firstName,
            unreadNotifications = unread,
            activeProjects = projectList.count { it.status != ProjectStatus.COMPLETED },
            pendingBookings = bookingList.count { it.status == BookingStatus.PENDING },
            openTasks = taskList.count { it.status != TaskStatus.DONE },
            overdueTasks = taskList.count { it.isOverdue(today) },
            myTasks = taskList
                .filter { it.assigneeId == user.id && it.status != TaskStatus.DONE }
                .sortedWith(compareBy<TaskItem>({ it.dueDate }, { -it.priority.rank }))
                .take(4),
            needsAttention = projectList
                .filter { it.status == ProjectStatus.REVISION_REQUESTED || it.status == ProjectStatus.IN_REVIEW }
                .sortedBy { if (it.status == ProjectStatus.REVISION_REQUESTED) 0 else 1 },
            projectTitles = projectList.associate { it.id to it.title },
            today = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgencyHomeUiState())
}
