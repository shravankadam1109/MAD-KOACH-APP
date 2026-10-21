package com.koach.studio.projects

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.ui.edit.EditDeliverableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DeliverableReviewTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val repository = SecuredProjectRepository(
        InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects(), clock), auth.currentUser, access
    )

    private suspend fun deliverable(projectId: String, id: String) =
        repository.getProjectById(projectId).first()!!.deliverables.first { it.id == id }

    @Test
    fun client_approvesDeliverableInReview_butNotADraft() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(repository.approveDeliverable("proj-1", "del-101").isSuccess)
        assertEquals(DeliverableStatus.APPROVED, deliverable("proj-1", "del-101").status)
        assertTrue(repository.approveDeliverable("proj-1", "del-101").isFailure)
    }

    @Test
    fun agency_cannotApprove_orSetClientDecisionsThroughEdits() = runTest {
        auth.signInAsDemo("u-zoya")
        assertTrue(repository.approveDeliverable("proj-1", "del-101").exceptionOrNull() is PermissionDeniedException)
        val inReview = deliverable("proj-1", "del-101")
        val approveViaEdit = repository.updateDeliverable("proj-1", inReview.copy(status = DeliverableStatus.APPROVED))
        assertTrue(approveViaEdit.exceptionOrNull() is PermissionDeniedException)
        assertTrue(repository.updateDeliverable("proj-1", inReview.copy(notes = "Updated grade notes")).isSuccess)
    }

    @Test
    fun revisionRequest_isRecordedWithRequester_andFlagsDeliverable() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(repository.requestRevision("proj-1", "   ").isFailure)
        repository.requestRevision("proj-1", "Warm up the skin tones in the rain scene.", deliverableId = "del-101")

        val project = repository.getProjectById("proj-1").first()!!
        val latest = project.revisions.first()
        assertEquals("u-maya", latest.requestedBy)
        assertEquals("del-101", latest.deliverableId)
        assertEquals(today, latest.requestedOn)
        assertFalse(latest.resolved)
        assertEquals(ProjectStatus.REVISION_REQUESTED, project.status)
        assertEquals(DeliverableStatus.REVISION_REQUESTED, deliverable("proj-1", "del-101").status)
    }

    @Test
    fun publishingNewVersion_bumpsVersion_resolvesRevisions_andReturnsProjectToReview() = runTest {
        auth.signInAsDemo("u-zoya")
        assertTrue(repository.publishNewVersion("proj-3", "del-301", "Tightened end card").isSuccess)
        val project = repository.getProjectById("proj-3").first()!!
        val updated = project.deliverables.first { it.id == "del-301" }
        assertEquals("v3.0", updated.version)
        assertEquals(listOf("v2.0"), updated.previousVersions)
        assertEquals(DeliverableStatus.IN_REVIEW, updated.status)
        assertTrue(project.revisions.filter { it.deliverableId == "del-301" }.all { it.resolved })
        assertEquals(ProjectStatus.IN_REVIEW, project.status)

        auth.signInAsDemo("u-dev")
        assertTrue(repository.publishNewVersion("proj-3", "del-301", "").exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun clients_doNotSeeDraftDeliverables() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(repository.getProjectById("proj-1").first()!!.deliverables.none { it.status == DeliverableStatus.DRAFT })
        auth.signInAsDemo("u-zoya")
        assertTrue(repository.getProjectById("proj-1").first()!!.deliverables.any { it.status == DeliverableStatus.DRAFT })
    }

    @Test
    fun editScreen_onlyOffersAgencyStatuses() = runTest {
        auth.signInAsDemo("u-zoya")
        val inReview = EditDeliverableViewModel("proj-1", "del-101", repository)
        assertEquals(listOf(DeliverableStatus.DRAFT, DeliverableStatus.IN_REVIEW), inReview.uiState.value.availableStatuses)
        // A client decision stays visible as the current status, but can't be newly chosen.
        val approved = EditDeliverableViewModel("proj-1", "del-102", repository)
        assertEquals(
            listOf(DeliverableStatus.DRAFT, DeliverableStatus.IN_REVIEW, DeliverableStatus.APPROVED),
            approved.uiState.value.availableStatuses
        )
    }
}
