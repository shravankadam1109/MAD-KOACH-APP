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
import com.koach.studio.tasks.ui.TaskDetailViewModel
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

    @Test
    fun list_searchMatchesTitleCaseInsensitively_andClearsBack() = runTest {
        auth.signInAsDemo("u-isha")
        tasks.createTask(draft.copy(title = "Zebra teaser grade")).getOrThrow()
        val vm = listVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val all = vm.uiState.first { !it.isLoading }.rows.size

        vm.onQuery("zEBRa")
        assertEquals(listOf("Zebra teaser grade"), vm.uiState.value.rows.map { it.task.title })

        vm.onQuery("no-such-task-xyz")
        assertTrue(vm.uiState.value.rows.isEmpty())

        vm.onQuery("   ")
        assertEquals(all, vm.uiState.value.rows.size)
    }

    @Test
    fun detail_showsTaskAndAssignee_andTogglesDone() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = TaskDetailViewModel("t-1", auth.currentUser.value!!, tasks, projects, data.users, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val before = vm.uiState.first { !it.isLoading }
        assertEquals("t-1", before.task?.id)
        assertTrue(before.canManage)

        vm.toggleDone()
        assertEquals(TaskStatus.DONE, vm.uiState.value.task?.status)
        vm.toggleDone()
        assertEquals(TaskStatus.TODO, vm.uiState.value.task?.status)
    }

    @Test
    fun detail_unknownTask_hasNoTask() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = TaskDetailViewModel("t-missing", auth.currentUser.value!!, tasks, projects, data.users, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(null, vm.uiState.first { !it.isLoading }.task)
    }

    @Test
    fun validator_enforcesTitleAndDescriptionBoundaries() {
        fun errorsFor(title: String, description: String = "") =
            TaskValidator.validate(draft.copy(title = title, description = description)).keys

        assertTrue(errorsFor("abc").isEmpty())
        assertEquals(setOf(TaskValidator.Field.TITLE), errorsFor("ab"))
        assertEquals(setOf(TaskValidator.Field.TITLE), errorsFor("   a   "))
        assertTrue(errorsFor("x".repeat(TaskValidator.TITLE_MAX)).isEmpty())
        assertEquals(setOf(TaskValidator.Field.TITLE), errorsFor("x".repeat(TaskValidator.TITLE_MAX + 1)))
        assertTrue(errorsFor("abc", "d".repeat(TaskValidator.DESCRIPTION_MAX)).isEmpty())
        assertEquals(
            setOf(TaskValidator.Field.DESCRIPTION),
            errorsFor("abc", "d".repeat(TaskValidator.DESCRIPTION_MAX + 1))
        )
    }

    @Test
    fun form_save_withInvalidInput_doesNotCreateTask() = runTest {
        auth.signInAsDemo("u-isha")
        val before = tasks.observeTasks().first().size
        val vm = TaskFormViewModel(null, auth.currentUser.value!!, tasks, projects, data.users, clock)
        vm.onTitle("ab")
        vm.save()
        assertEquals(setOf(TaskValidator.Field.TITLE, TaskValidator.Field.DUE_DATE), vm.uiState.value.errors.keys)
        assertTrue(!vm.uiState.value.done)
        assertEquals(before, tasks.observeTasks().first().size)

        vm.onTitle("Valid title")
        assertTrue(TaskValidator.Field.TITLE !in vm.uiState.value.errors)
    }

    @Test
    fun form_delete_removesTask_andReportsDone() = runTest {
        auth.signInAsDemo("u-isha")
        val created = tasks.createTask(draft).getOrThrow()
        val vm = TaskFormViewModel(created.id, auth.currentUser.value!!, tasks, projects, data.users, clock)
        vm.delete()
        assertTrue(vm.uiState.value.done)
        assertEquals(null, tasks.observeTask(created.id).first())
    }

    @Test
    fun form_delete_withoutPermission_showsErrorAndKeepsTask() = runTest {
        auth.signInAsDemo("u-isha")
        val created = tasks.createTask(draft).getOrThrow()
        auth.signInAsDemo("u-maya")
        val vm = TaskFormViewModel(created.id, auth.currentUser.value!!, tasks, projects, data.users, clock)
        vm.delete()
        assertTrue(!vm.uiState.value.done)
        assertTrue(vm.uiState.value.errorMessage != null)
        assertTrue(!vm.uiState.value.isSaving)
        auth.signInAsDemo("u-isha")
        assertTrue(tasks.observeTask(created.id).first() != null)
    }

    @Test
    fun list_and_detail_confirmCreateSaveAndDelete_andNeverReportFailedOperationsAsSuccess() = runTest {
        auth.signInAsDemo("u-isha")
        val list = listVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }
        list.uiState.first { !it.isLoading }

        // Failed create (invalid title) must not show a success message on the list.
        assertTrue(tasks.createTask(draft.copy(title = "x")).isFailure)
        assertEquals(null, list.uiState.value.message)

        // Successful create confirms on the list.
        val created = tasks.createTask(draft.copy(title = "Colour grade pass")).getOrThrow()
        assertEquals("Task \"Colour grade pass\" created", list.uiState.value.message)
        assertTrue(!list.uiState.value.messageIsError)
        list.messageShown()
        assertEquals(null, list.uiState.value.message)

        // Open detail for the created task, then update it.
        val detail = TaskDetailViewModel(created.id, auth.currentUser.value!!, tasks, projects, data.users, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { detail.uiState.collect {} }
        detail.uiState.first { !it.isLoading }

        tasks.updateTask(created.id, draft.copy(title = "Final colour grade")).getOrThrow()
        assertEquals("Task \"Final colour grade\" saved", detail.uiState.value.message)
        assertTrue(!detail.uiState.value.messageIsError)
        assertEquals("Task \"Final colour grade\" saved", list.uiState.value.message)

        // Deleting marks detail as deleted so it pops back, and confirms deletion on the list.
        tasks.deleteTask(created.id).getOrThrow()
        assertTrue(detail.uiState.value.isDeleted)
        assertEquals("Task \"Final colour grade\" deleted", list.uiState.value.message)
        assertTrue(!list.uiState.value.messageIsError)
    }

    @Test
    fun toggleDone_reportsSuccessAndFlagsForbiddenToggleAsError() = runTest {
        auth.signInAsDemo("u-zoya")
        val list = listVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }
        val row = list.uiState.first { !it.isLoading }.rows.first { it.task.id == "t-1" }

        list.toggleDone(row.task)
        assertEquals("Marked \"${row.task.title}\" as done", list.uiState.value.message)
        assertTrue(!list.uiState.value.messageIsError)

        // Sign in as a client so setStatus fails with PermissionDeniedException.
        auth.signInAsDemo("u-maya")
        list.toggleDone(row.task.copy(status = TaskStatus.DONE))
        assertTrue(list.uiState.value.messageIsError)
        assertTrue(list.uiState.value.message != null)
        assertTrue(!list.uiState.value.message!!.startsWith("Reopened"))
    }
}
