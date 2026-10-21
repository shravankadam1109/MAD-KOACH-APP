package com.koach.studio.tasks

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.TaskPriority
import com.koach.studio.core.model.TaskStatus
import com.koach.studio.projects.MainDispatcherRule
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.tasks.data.InMemoryTaskRepository
import com.koach.studio.tasks.data.TaskDraft
import com.koach.studio.tasks.data.TaskValidator
import com.koach.studio.tasks.ui.TaskFormViewModel
import com.koach.studio.tasks.ui.TaskListViewModel
import com.koach.studio.tasks.ui.TaskSort
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TaskManagementTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val tasks = InMemoryTaskRepository(data.tasks, auth.currentUser, access, clock)
    private val projects = SecuredProjectRepository(InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()), auth.currentUser, access)

    private val draft = TaskDraft("Cut teaser", "15s vertical teaser", "proj-1", "u-zoya", TaskPriority.HIGH, TaskStatus.TODO, today.plusDays(3))

    private fun listVm() = TaskListViewModel(auth.currentUser.value!!, tasks, projects, data.users, clock)

    @Test
    fun validator_requiresTitleAndDueDate() {
        val errors = TaskValidator.validate(draft.copy(title = "a", dueDate = null))
        assertEquals(setOf(TaskValidator.Field.TITLE, TaskValidator.Field.DUE_DATE), errors.keys)
    }

    @Test
    fun clients_seeNoTasks_andCannotCreate() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(tasks.observeTasks().first().isEmpty())
        assertTrue(tasks.createTask(draft).exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun agency_createsEditsCompletesAndDeletes() = runTest {
        auth.signInAsDemo("u-isha")
        val created = tasks.createTask(draft).getOrThrow()
        assertEquals("u-isha", created.createdBy)
        tasks.updateTask(created.id, draft.copy(priority = TaskPriority.URGENT))
        tasks.setStatus(created.id, TaskStatus.DONE)
        val stored = tasks.observeTask(created.id).first()!!
        assertEquals(TaskPriority.URGENT, stored.priority)
        assertEquals(TaskStatus.DONE, stored.status)
        assertTrue(tasks.deleteTask(created.id).isSuccess)
    }

    @Test
    fun list_flagsOverdue_filtersMine_andSortsByPriority() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = listVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        // t-5 (due yesterday) and t-10 (4 days ago) are overdue and not done.
        assertEquals(2, state.overdueCount)
        assertTrue(state.rows.first { it.task.id == "t-5" }.isOverdue)

        vm.setOnlyMine(true)
        assertTrue(vm.uiState.value.rows.all { it.task.assigneeId == "u-zoya" })

        vm.setOnlyMine(false)
        vm.setSort(TaskSort.PRIORITY)
        val open = vm.uiState.value.rows.filter { it.task.status != TaskStatus.DONE }
        assertEquals(TaskPriority.URGENT, open.first().task.priority)
        assertEquals(TaskStatus.DONE, vm.uiState.value.rows.last().task.status)
    }

    @Test
    fun list_toggleDone_marksTaskDone() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = listVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val task = vm.uiState.first { !it.isLoading }.rows.first { it.task.id == "t-1" }.task
        vm.toggleDone(task)
        assertEquals(TaskStatus.DONE, tasks.observeTask("t-1").first()!!.status)
    }

    @Test
    fun form_defaultsAssigneeToMe_andValidatesBeforeSave() = runTest {
        auth.signInAsDemo("u-rohan")
        val vm = TaskFormViewModel(null, auth.currentUser.value!!, tasks, projects, data.users, clock)
        assertEquals("u-rohan", vm.uiState.value.assigneeId)
        vm.save()
        assertTrue(vm.uiState.value.errors.isNotEmpty())
        vm.onTitle("Light test for rooftop"); vm.onDueDate(today.plusDays(2))
        vm.save()
        assertTrue(vm.uiState.value.done)
    }
}
