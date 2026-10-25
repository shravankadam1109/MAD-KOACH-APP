package com.koach.studio.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.bookings.data.BookingRequest
import com.koach.studio.bookings.data.FirestoreBookingRepository
import com.koach.studio.core.FirebaseEmulator.auth
import com.koach.studio.core.FirebaseEmulator.createAccount
import com.koach.studio.core.FirebaseEmulator.email
import com.koach.studio.core.FirebaseEmulator.enabled
import com.koach.studio.core.FirebaseEmulator.firestore
import com.koach.studio.core.FirebaseEmulator.writeProfile
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.firebase.FirebaseAuthRepository
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.MessageThread
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.messaging.data.FirestoreMessageRepository
import com.koach.studio.notifications.data.FirestoreNotificationRepository
import com.koach.studio.payments.FirestorePaymentRepository
import com.koach.studio.projects.data.FirestoreProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * The Firestore repositories against the LOCAL emulators and the real
 * `firebase/firestore.rules`: every client query must be one the rules allow,
 * and every write must pass them. Skipped unless run with `firebaseEmulator=true`
 * (see [FirebaseAuthEmulatorTest] for the command).
 */
@RunWith(AndroidJUnit4::class)
class FirestoreRepositoriesEmulatorTest {

    private val clock = Clock.systemDefaultZone()
    private lateinit var authRepository: FirebaseAuthRepository
    private lateinit var access: AccessControl
    private lateinit var adminEmail: String
    private lateinit var clientEmail: String
    private lateinit var clientUid: String
    private lateinit var adminUid: String
    private val clientId = "cl-" + UUID.randomUUID().toString().take(8)

    private val user get() = authRepository.currentUser
    private val projects by lazy { SecuredProjectRepository(FirestoreProjectRepository(firestore, user, clock), user, access) }
    private val notifications by lazy { FirestoreNotificationRepository(firestore, user) }

    @Before
    fun setUp() {
        assumeTrue("Firebase emulator tests not requested", enabled)
        auth.signOut()
        authRepository = FirebaseAuthRepository(auth, firestore)
        access = AccessControl { authRepository.currentUser.value }
        adminEmail = email("admin")
        clientEmail = email("client")
        adminUid = createAccount(adminEmail).also { writeProfile(it, role = "admin", name = "Ava Admin") }
        clientUid = createAccount(clientEmail).also { writeProfile(it, role = "client", clientId = clientId, name = "Cleo Client") }
    }

    @Test
    fun clientSeesOwnProjectsWithoutDrafts_andCanApproveAndRequestChanges() = runBlocking<Unit> {
        signIn(adminEmail)
        val mine = seedProject(clientId)
        val theirs = seedProject("cl-other-" + UUID.randomUUID().toString().take(6))

        signIn(clientEmail)
        val visible = await(projects.getProjects()) { list -> list.any { it.id == mine } }
        assertTrue("another client's project leaked", visible.none { it.id == theirs })
        val project = visible.first { it.id == mine }
        assertEquals(listOf("cut-a", "cut-b"), project.deliverables.map { it.id }.sorted())

        projects.approveDeliverable(mine, "cut-a").getOrThrow()
        projects.requestRevision(mine, "Please brighten the opening shot", deliverableId = "cut-b").getOrThrow()
        val afterClient = await(projects.getProjectById(mine)) { it?.status == ProjectStatus.REVISION_REQUESTED }!!
        assertEquals(DeliverableStatus.APPROVED, afterClient.deliverables.first { it.id == "cut-a" }.status)
        assertEquals(DeliverableStatus.REVISION_REQUESTED, afterClient.deliverables.first { it.id == "cut-b" }.status)
        assertEquals(clientUid, afterClient.revisions.single().requestedBy)

        signIn(adminEmail)
        projects.publishNewVersion(mine, "cut-b", "Brighter opening").getOrThrow()
        val afterAgency = await(projects.getProjectById(mine)) { it?.status == ProjectStatus.IN_REVIEW }!!
        val cutB = afterAgency.deliverables.first { it.id == "cut-b" }
        assertEquals("v2.0", cutB.version)
        assertEquals(DeliverableStatus.IN_REVIEW, cutB.status)
        assertTrue(afterAgency.revisions.single().resolved)
        assertEquals(3, afterAgency.deliverables.size) // the agency still sees its draft
    }

    @Test
    fun agencyCannotApproveOnTheClientsBehalf() = runBlocking<Unit> {
        signIn(adminEmail)
        val id = seedProject(clientId)
        val result = projects.approveDeliverable(id, "cut-a")
        assertTrue("agency approval must be refused, got $result", result.isFailure)
    }

    @Test
    fun bookingRequest_flowsFromClientToAgencyAndBack() = runBlocking<Unit> {
        val bookings = FirestoreBookingRepository(firestore, user, access, notifications, clock)
        signIn(clientEmail)
        val booking = bookings.createBooking(
            BookingRequest(
                "svc-shoot", null, LocalDate.now(clock).plusDays(14), 50_000,
                "Two-day music video shoot in Mumbai with a small crew.", "Cleo Client", "cleo@test.koach"
            )
        ).getOrThrow()

        signIn(adminEmail)
        await(notifications.observeNotifications()) { list -> list.any { it.targetId == booking.id } }
        assertTrue(bookings.updateStatus(booking.id, BookingStatus.DECLINED).isFailure) // a decline needs a reason
        bookings.updateStatus(booking.id, BookingStatus.CONFIRMED).getOrThrow()

        signIn(clientEmail)
        await(bookings.observeBooking(booking.id)) { it?.status == BookingStatus.CONFIRMED }
        await(notifications.observeNotifications()) { list -> list.any { it.targetId == booking.id && !it.isRead } }
    }

    @Test
    fun messages_arriveWithUnreadCounts_andCanBeMarkedRead() = runBlocking<Unit> {
        val messages = FirestoreMessageRepository(firestore, user, access, notifications, clock)
        signIn(adminEmail)
        val threadId = "th-" + UUID.randomUUID().toString().take(8)
        firestore.collection("threads").document(threadId)
            .set(FirestoreMappers.threadFields(MessageThread(threadId, "p-1", "Launch video", listOf(clientUid, adminUid)))).await()

        signIn(clientEmail)
        await(messages.observeThreads()) { list -> list.any { it.thread.id == threadId } }
        messages.sendMessage(threadId, "Can we move the review to Friday?").getOrThrow()

        signIn(adminEmail)
        await(messages.observeThreads()) { list -> list.firstOrNull { it.thread.id == threadId }?.unreadCount == 1 }
        await(notifications.observeNotifications()) { list -> list.any { it.targetId == threadId } }
        messages.markThreadRead(threadId).getOrThrow()
        await(messages.observeThreads()) { list -> list.firstOrNull { it.thread.id == threadId }?.unreadCount == 0 }
    }

    @Test
    fun clientSeesOnlyOwnInvoices_andCannotMarkThemPaid() = runBlocking<Unit> {
        val payments = FirestorePaymentRepository(firestore, user, access)
        signIn(adminEmail)
        val mine = seedInvoice(clientId)
        val theirs = seedInvoice("cl-other-" + UUID.randomUUID().toString().take(6))

        signIn(clientEmail)
        val visible = await(payments.observePayments()) { list -> list.any { it.id == mine } }
        assertTrue(visible.none { it.id == theirs })
        assertTrue(payments.markPaidSimulated(mine).isFailure)
        val forged = runCatching { firestore.collection("payments").document(mine).update("status", "PAID").await() }
        assertTrue("payment status must never be writable by a client", forged.isFailure)
    }

    private suspend fun signIn(email: String) {
        authRepository.signInWithEmail(email, FirebaseEmulator.PASSWORD).getOrThrow()
    }

    private suspend fun <T> await(flow: Flow<T>, predicate: (T) -> Boolean): T = withTimeout(15_000) { flow.first(predicate) }

    /** Seeds as the signed-in admin, through the rules: two shared deliverables and one draft. */
    private suspend fun seedProject(owner: String): String {
        val id = "p-" + UUID.randomUUID().toString().take(8)
        val project = Project(id, "Launch video", "Test Client", ServiceType.EDITING, ProjectStatus.IN_REVIEW, LocalDate.now(clock).plusDays(30), "", clientId = owner)
        val ref = firestore.collection("projects").document(id)
        ref.set(FirestoreMappers.projectFields(project)).await()
        listOf(
            Deliverable("cut-a", id, "Cut A", "MP4", "1 GB", "v1.0", status = DeliverableStatus.IN_REVIEW),
            Deliverable("cut-b", id, "Cut B", "MP4", "1 GB", "v1.0", status = DeliverableStatus.IN_REVIEW),
            Deliverable("cut-draft", id, "Rough cut", "MP4", "1 GB", "v0.1", status = DeliverableStatus.DRAFT)
        ).forEach { ref.collection("deliverables").document(it.id).set(FirestoreMappers.deliverableFields(it)).await() }
        return id
    }

    private suspend fun seedInvoice(owner: String): String {
        val id = "pay-" + UUID.randomUUID().toString().take(8)
        val today = LocalDate.now(clock)
        val payment = Payment(id, "INV-TEST", owner, null, "Edit", 25_000, today, today.plusDays(14), PaymentStatus.DUE)
        firestore.collection("payments").document(id).set(FirestoreMappers.paymentFields(payment)).await()
        return id
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun connectToEmulators() = FirebaseEmulator.connect()
    }
}
