package com.koach.studio.core

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.auth.can
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.Role
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AccessControlTest {

    private val users = DemoData(LocalDate.of(2026, 10, 22)).users
    private val auth = DemoAuthRepository(users)
    private val access = AccessControl { auth.currentUser.value }
    private val projects = SecuredProjectRepository(
        InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()),
        auth.currentUser,
        access
    )

    @Test
    fun clients_cannotUseAgencyPermissions() {
        val client = users.first { it.role == Role.CLIENT }
        assertFalse(client.can(Permission.EDIT_DELIVERABLES))
        assertFalse(client.can(Permission.MANAGE_CLIENTS))
        assertFalse(client.can(Permission.VIEW_INTERNAL_DOCUMENTS))
        assertTrue(client.can(Permission.REVIEW_DELIVERABLES))
        assertTrue(client.can(Permission.REQUEST_REVISIONS))
    }

    @Test
    fun onlyAdmins_canDeleteClientsAndSeeAllPayments() {
        val team = users.first { it.role == Role.TEAM }
        val admin = users.first { it.role == Role.ADMIN }
        assertFalse(team.can(Permission.DELETE_CLIENTS))
        assertFalse(team.can(Permission.VIEW_ALL_PAYMENTS))
        assertTrue(admin.can(Permission.DELETE_CLIENTS))
        assertTrue(admin.can(Permission.VIEW_ALL_PAYMENTS))
    }

    @Test
    fun signedOut_seesNoProjects() = runTest {
        assertTrue(projects.getProjects().first().isEmpty())
    }

    @Test
    fun client_seesOnlyOwnOrganisationsProjects() = runTest {
        auth.signInAsDemo("u-maya")
        val visible = projects.getProjects().first()
        assertEquals(listOf("proj-1"), visible.map { it.id })
        assertNull(projects.getProjectById("proj-3").first())
    }

    @Test
    fun agency_seesEveryProject() = runTest {
        auth.signInAsDemo("u-zoya")
        assertEquals(4, projects.getProjects().first().size)
    }

    @Test
    fun client_cannotEditDeliverables_evenWithoutTheUi() = runTest {
        auth.signInAsDemo("u-maya")
        val deliverable = projects.getProjectById("proj-1").first()!!.deliverables.first()
        val result = projects.updateDeliverable("proj-1", deliverable.copy(name = "Hacked"))
        assertTrue(result.exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun client_cannotRequestRevisionOnAnotherClientsProject() = runTest {
        auth.signInAsDemo("u-maya")
        val result = projects.requestRevision("proj-3", "Please change everything")
        assertTrue(result.isFailure)
    }

    @Test
    fun agency_cannotRequestRevisionsOnBehalfOfClients() = runTest {
        auth.signInAsDemo("u-zoya")
        val result = projects.requestRevision("proj-1", "Self-requested change")
        assertTrue(result.exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun emailSignIn_isUnavailableInDemo_andSignOutClearsUser() = runTest {
        assertTrue(auth.signInWithEmail("a@b.c", "secret").isFailure)
        auth.signInAsDemo("u-arjun")
        auth.signOut()
        assertNull(auth.currentUser.value)
    }
}
