package com.koach.studio.account

import com.koach.studio.clients.data.InMemoryClientRepository
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.feedback.FeedbackDraft
import com.koach.studio.feedback.FeedbackValidator
import com.koach.studio.feedback.InMemoryFeedbackRepository
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.payments.InMemoryPaymentRepository
import com.koach.studio.payments.PaymentsViewModel
import com.koach.studio.payments.effectiveStatus
import com.koach.studio.projects.MainDispatcherRule
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
class AccountRecordsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val notifications = InMemoryNotificationRepository(data.notifications, auth.currentUser, access)
    private val payments = InMemoryPaymentRepository(data.payments, data.users, auth.currentUser, access, notifications, clock)
    private val feedback = InMemoryFeedbackRepository(data.feedback, data.users, auth.currentUser, access, notifications, clock)
    private val clients = InMemoryClientRepository(data.clients, auth.currentUser, access, clock) { 0 }

    @Test
    fun payments_areScopedByRole() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(payments.observePayments().first().all { it.clientId == "cl-aetheria" })
        auth.signInAsDemo("u-zoya")
        assertTrue(payments.observePayments().first().isEmpty())
        auth.signInAsDemo("u-arjun")
        assertEquals(data.payments.size, payments.observePayments().first().size)
    }

    @Test
    fun simulatedPayment_marksPaid_onlyForOwningClient_andNotifiesAdmin() = runTest {
        auth.signInAsDemo("u-dev")
        assertTrue(payments.markPaidSimulated("pay-2").exceptionOrNull() is PermissionDeniedException)
        assertTrue(payments.markPaidSimulated("pay-4").isSuccess)
        val paid = payments.observePayments().first().first { it.id == "pay-4" }
        assertEquals(PaymentStatus.PAID, paid.status)
        assertEquals(today, paid.paidOn)
        assertTrue(payments.markPaidSimulated("pay-4").isFailure)

        auth.signInAsDemo("u-arjun")
        assertTrue(notifications.observeNotifications().first().any { it.type == NotificationType.PAYMENT && it.body.contains("simulated") })
    }

    @Test
    fun overdueIsDerivedFromDueDate() {
        val due = data.payments.first { it.id == "pay-2" }
        assertEquals(PaymentStatus.DUE, due.effectiveStatus(today))
        assertEquals(PaymentStatus.OVERDUE, due.effectiveStatus(today.plusDays(30)))
    }

    @Test
    fun paymentsViewModel_totalsOutstandingAndPaid() = runTest {
        auth.signInAsDemo("u-maya")
        val vm = PaymentsViewModel(auth.currentUser.value!!, payments, clients, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertEquals(85_000, state.outstandingInr)
        assertEquals(1_03_000, state.paidInr)
        assertTrue(state.isClient)
    }

    @Test
    fun feedbackValidation_requiresRatingOnlyForFeedback() {
        val complaint = FeedbackDraft(FeedbackType.COMPLAINT, null, "Late delivery", "The files arrived two days late.", null)
        assertTrue(FeedbackValidator.validate(complaint).isEmpty())
        val noRating = complaint.copy(type = FeedbackType.FEEDBACK)
        assertEquals(setOf(FeedbackValidator.Field.RATING), FeedbackValidator.validate(noRating).keys)
    }

    @Test
    fun clientSubmitsComplaint_agencyResponds_clientIsNotified() = runTest {
        auth.signInAsDemo("u-maya")
        val sent = feedback.submit(FeedbackDraft(FeedbackType.COMPLAINT, 4, "Wrong export", "The 9:16 export has black bars.", "proj-1")).getOrThrow()
        assertEquals(null, sent.rating)
        assertTrue(feedback.respond(sent.id, "Fixing it now", FeedbackStatus.IN_PROGRESS).exceptionOrNull() is PermissionDeniedException)

        auth.signInAsDemo("u-zoya")
        assertTrue(feedback.observeFeedback().first().any { it.id == sent.id })
        feedback.respond(sent.id, "Re-exporting without bars today.", FeedbackStatus.RESOLVED)

        auth.signInAsDemo("u-maya")
        val updated = feedback.observeFeedback().first().first { it.id == sent.id }
        assertEquals(FeedbackStatus.RESOLVED, updated.status)
        assertTrue(notifications.observeNotifications().first().any { it.targetId == sent.id })
        assertTrue(feedback.observeFeedback().first().all { it.clientId == "cl-aetheria" })
    }
}
