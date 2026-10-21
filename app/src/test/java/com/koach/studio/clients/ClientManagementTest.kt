package com.koach.studio.clients

import com.koach.studio.clients.data.ClientDraft
import com.koach.studio.clients.data.ClientHasProjectsException
import com.koach.studio.clients.data.ClientValidator
import com.koach.studio.clients.data.ClientValidator.Field
import com.koach.studio.clients.data.InMemoryClientRepository
import com.koach.studio.clients.ui.ClientFormViewModel
import com.koach.studio.clients.ui.ClientListViewModel
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.ClientStatus
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ClientManagementTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val projectStore = InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects())
    private val projects = SecuredProjectRepository(projectStore, auth.currentUser, access)
    private val clients = InMemoryClientRepository(data.clients, auth.currentUser, access, clock) { id ->
        projectStore.getProjects().first().count { it.clientId == id }
    }

    private val validDraft = ClientDraft("Monsoon Films", "Ria Shah", "ria@monsoon.example", "+91 98200 33333", "Film production", "Mumbai", ClientStatus.LEAD, "")

    @Test
    fun validator_catchesEachField() {
        val errors = ClientValidator.validate(ClientDraft("A", "", "bad", "12ab", "", "", ClientStatus.LEAD, "x".repeat(501)))
        assertEquals(Field.entries.toSet(), errors.keys)
        assertTrue(ClientValidator.validate(validDraft).isEmpty())
    }

    @Test
    fun team_canCreateAndEdit_butOnlyAdminCanDelete() = runTest {
        auth.signInAsDemo("u-zoya")
        val created = clients.createClient(validDraft).getOrThrow()
        assertEquals(today, created.since)
        val edited = clients.updateClient(created.id, validDraft.copy(status = ClientStatus.ACTIVE)).getOrThrow()
        assertEquals(ClientStatus.ACTIVE, edited.status)
        assertTrue(clients.deleteClient(created.id).exceptionOrNull() is PermissionDeniedException)

        auth.signInAsDemo("u-arjun")
        assertTrue(clients.deleteClient(created.id).isSuccess)
        assertNull(clients.observeClient(created.id).first())
    }

    @Test
    fun clientsWithProjects_cannotBeDeleted() = runTest {
        auth.signInAsDemo("u-arjun")
        assertTrue(clients.deleteClient("cl-aetheria").exceptionOrNull() is ClientHasProjectsException)
    }

    @Test
    fun clientUsers_seeOnlyTheirOrganisation_andCannotEdit() = runTest {
        auth.signInAsDemo("u-maya")
        assertEquals(listOf("cl-aetheria"), clients.observeClients().first().map { it.id })
        assertTrue(clients.createClient(validDraft).isFailure)
    }

    @Test
    fun list_searchesAndFilters_withProjectCounts() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = ClientListViewModel(auth.currentUser.value!!, clients, projects)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.uiState.first { !it.isLoading }

        vm.onQueryChange("mumbai")
        assertEquals(setOf("cl-aetheria", "cl-lumen"), vm.uiState.value.rows.map { it.client.id }.toSet())
        vm.onQueryChange("")
        vm.onStatusFilter(ClientStatus.LEAD)
        assertEquals(setOf("cl-lumen", "cl-orbit"), vm.uiState.value.rows.map { it.client.id }.toSet())
        vm.onStatusFilter(null)
        assertEquals(1, vm.uiState.value.rows.first { it.client.id == "cl-apex" }.projectCount)
    }

    @Test
    fun form_validatesBeforeSaving_andLoadsExistingForEdit() = runTest {
        auth.signInAsDemo("u-zoya")
        val create = ClientFormViewModel(null, clients)
        create.save()
        assertTrue(create.uiState.value.errors.isNotEmpty())
        create.onName(validDraft.name); create.onContact(validDraft.contactName); create.onEmail(validDraft.email)
        create.onPhone(validDraft.phone); create.onIndustry(validDraft.industry); create.onCity(validDraft.city)
        create.save()
        assertNotNull(create.uiState.value.savedClientId)

        val edit = ClientFormViewModel("cl-apex", clients)
        assertEquals("Apex Athletics", edit.uiState.value.name)
        assertTrue(edit.uiState.value.isEditing)
    }
}
