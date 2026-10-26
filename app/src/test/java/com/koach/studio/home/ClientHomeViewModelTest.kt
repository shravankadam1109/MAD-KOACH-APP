package com.koach.studio.home

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.data.DemoData
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.projects.MainDispatcherRule
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
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
class ClientHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Kolkata")
    private val clock = Clock.fixed(LocalDateTime.of(2026, 10, 22, 9, 30).atZone(zone).toInstant(), zone)
    private val data = DemoData(LocalDate.now(clock))
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }

    private suspend fun viewModelFor(userId: String): ClientHomeViewModel {
        auth.signInAsDemo(userId)
        val projects = SecuredProjectRepository(
            InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()), auth.currentUser, access
        )
        val notifications = InMemoryNotificationRepository(data.notifications, auth.currentUser, access)
        return ClientHomeViewModel(auth.currentUser.value!!, projects, notifications, clock)
    }

    @Test
    fun dashboard_showsOnlyTheClientsOwnWork() = runTest {
        val vm = viewModelFor("u-maya")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }

        assertEquals("Good morning", state.greeting)
        assertEquals("Maya", state.firstName)
        assertEquals("proj-1", state.featuredProject?.id)
        assertTrue(state.activeProjects.all { it.clientId == "cl-aetheria" })
        assertEquals(listOf("del-101"), state.awaitingReview.map { it.deliverable.id })
        assertEquals(2, state.unreadNotifications)
    }

    @Test
    fun upcomingMilestones_areIncompleteAndSortedByDate() = runTest {
        val vm = viewModelFor("u-maya")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val milestones = vm.uiState.first { !it.isLoading }.upcomingMilestones.map { it.milestone }

        assertTrue(milestones.none { it.completed })
        assertEquals(milestones.sortedBy { it.dueDate }, milestones)
    }

    @Test
    fun dashboard_computesStatisticsAndStatusBreakdown() = runTest {
        val vm = viewModelFor("u-maya")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }

        assertEquals(1, state.activeProjectsCount)
        assertEquals(1, state.pendingApprovalsCount)
        assertEquals(1, state.statusBreakdown.inProgressCount)
        assertEquals(0, state.statusBreakdown.inReviewCount)
        assertTrue(state.averageProgressPercent >= 0)
    }

    @Test
    fun greeting_followsTimeOfDay() {
        assertEquals("Good afternoon", ClientHomeViewModel.greetingFor(java.time.LocalTime.of(14, 0)))
        assertEquals("Good evening", ClientHomeViewModel.greetingFor(java.time.LocalTime.of(21, 0)))
    }
}
