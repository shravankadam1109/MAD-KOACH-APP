package com.koach.studio.home

import com.koach.studio.bookings.data.InMemoryBookingRepository
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.data.DemoData
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.projects.MainDispatcherRule
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.tasks.data.InMemoryTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AgencyHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Kolkata")
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 22, 10, 15).atZone(zone).toInstant(), zone)
    private val data = DemoData(LocalDate.now(clock))
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }

    private suspend fun viewModelFor(userId: String): AgencyHomeViewModel {
        auth.signInAsDemo(userId)
        val projects = SecuredProjectRepository(
            InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()),
            auth.currentUser,
            access
        )
        val tasks = InMemoryTaskRepository(data.tasks, auth.currentUser, access, clock)
        val notifications = InMemoryNotificationRepository(data.notifications, auth.currentUser, access)
        val bookings = InMemoryBookingRepository(data.bookings, data.users, auth.currentUser, access, notifications, clock)
        return AgencyHomeViewModel(
            user = auth.currentUser.value!!,
            projects = projects,
            tasks = tasks,
            bookings = bookings,
            notifications = notifications,
            clock = clock,
            people = data.users,
            isDemo = true
        )
    }

    @Test
    fun studioDashboard_computesProjectBreakdownProgressAndOverdueCounts() = runTest {
        val vm = viewModelFor("u-zoya")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }

        assertEquals("Good morning", state.greeting)
        assertEquals("Zoya", state.firstName)
        assertTrue(state.isDemo)
        assertTrue(state.activeProjects > 0)
        assertEquals(
            state.activeProjects,
            state.inProgressProjects + state.inReviewProjects + state.revisionRequestedProjects
        )
        assertTrue(state.averageProgressPercent in 1..100)
        assertTrue(state.openTasks > 0)
    }

    @Test
    fun studioDashboard_populatesSortedUpcomingDeadlinesAndTeamWorkload() = runTest {
        val vm = viewModelFor("u-zoya")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }

        assertTrue(state.upcomingDeadlines.isNotEmpty())
        val dates = state.upcomingDeadlines.map { it.dueDate }
        assertEquals(dates.sorted(), dates)

        // Team workload includes only agency members sorted by open task count descending
        assertTrue(state.teamWorkload.isNotEmpty())
        assertTrue(state.teamWorkload.all { it.user.role.isAgency })
        val openCounts = state.teamWorkload.map { it.openTasks }
        assertEquals(openCounts.sortedDescending(), openCounts)
    }
}
