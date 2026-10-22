package com.koach.studio.bookings

import com.koach.studio.bookings.data.BookingRequest
import com.koach.studio.bookings.data.BookingValidator
import com.koach.studio.bookings.data.BookingValidator.Field
import com.koach.studio.bookings.data.InMemoryBookingRepository
import com.koach.studio.bookings.ui.BookingDetailViewModel
import com.koach.studio.bookings.ui.NewBookingViewModel
import com.koach.studio.catalog.data.InMemoryCatalogRepository
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.notifications.data.InMemoryNotificationRepository
import com.koach.studio.projects.MainDispatcherRule
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
class BookingWorkflowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(zone).plusHours(10).toInstant(), zone)
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val notifications = InMemoryNotificationRepository(data.notifications, auth.currentUser, access)
    private val bookings = InMemoryBookingRepository(data.bookings, data.users, auth.currentUser, access, notifications, clock)
    private val catalog = InMemoryCatalogRepository(data.services, data.portfolio)

    private fun request(date: LocalDate = today.plusDays(10), details: String = "Rooftop music video with a small dance crew.") =
        BookingRequest("svc-film", "film-2", date, 70_000, details, "Maya Rao", "maya@aetheria.example")

    @Test
    fun validator_rejectsShortNotice_shortDetails_andBadEmail() {
        val errors = BookingValidator.validate(
            request(date = today.plusDays(1), details = "Too short").copy(contactEmail = "not-an-email"),
            today
        )
        assertEquals(setOf(Field.DATE, Field.DETAILS, Field.EMAIL), errors.keys)
        assertTrue(BookingValidator.validate(request(), today).isEmpty())
    }

    @Test
    fun client_createsBooking_seesIt_andAdminIsNotified() = runTest {
        auth.signInAsDemo("u-maya")
        val created = bookings.createBooking(request()).getOrThrow()
        assertEquals(BookingStatus.PENDING, created.status)
        assertEquals("cl-aetheria", created.clientId)
        assertTrue(bookings.observeBookings().first().any { it.id == created.id })

        auth.signInAsDemo("u-arjun")
        assertTrue(notifications.observeNotifications().first().any { it.targetId == created.id })
    }

    @Test
    fun clients_onlySeeTheirOwnBookings_andCannotConfirm() = runTest {
        auth.signInAsDemo("u-dev")
        val visible = bookings.observeBookings().first()
        assertTrue(visible.all { it.clientId == "cl-apex" })
        val result = bookings.updateStatus("bk-3", BookingStatus.CONFIRMED)
        assertTrue(result.exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun agency_cannotCreateBookings_butCanConfirmThem() = runTest {
        auth.signInAsDemo("u-zoya")
        assertTrue(bookings.createBooking(request()).isFailure)
        assertTrue(bookings.updateStatus("bk-1", BookingStatus.CONFIRMED).isSuccess)
        assertEquals(BookingStatus.CONFIRMED, bookings.observeBooking("bk-1").first()?.status)
    }

    @Test
    fun form_blocksSubmitUntilValid_thenSubmits() = runTest {
        auth.signInAsDemo("u-maya")
        val vm = NewBookingViewModel(auth.currentUser.value!!, "svc-film", "film-2", catalog, bookings, clock)

        vm.submit()
        assertTrue(Field.DATE in vm.uiState.value.errors)
        assertTrue(Field.DETAILS in vm.uiState.value.errors)
        assertNull(vm.uiState.value.submitted)

        vm.onDateSelected(today.plusDays(14))
        vm.onDetailsChanged("Daytime rooftop shoot for our second single.")
        vm.submit()
        assertNotNull(vm.uiState.value.submitted)
        assertEquals("Maya Rao", vm.uiState.value.contactName)
    }

    @Test
    fun agency_transitionsFollowRules_andDeclineNeedsANote() = runTest {
        auth.signInAsDemo("u-arjun")
        // bk-2 is completed: nothing can change it.
        assertTrue(bookings.updateStatus("bk-2", BookingStatus.CONFIRMED).isFailure)
        // Pending can't jump straight to completed.
        assertTrue(bookings.updateStatus("bk-1", BookingStatus.COMPLETED).isFailure)
        assertTrue(bookings.updateStatus("bk-1", BookingStatus.DECLINED, "  ").isFailure)
        assertTrue(bookings.updateStatus("bk-1", BookingStatus.DECLINED, "Fully booked that week").isSuccess)

        auth.signInAsDemo("u-maya")
        val declined = bookings.observeBooking("bk-1").first()!!
        assertEquals(BookingStatus.DECLINED, declined.status)
        assertEquals("Fully booked that week", declined.statusNote)
        assertTrue(notifications.observeNotifications().first().any { it.targetId == "bk-1" && it.body == "Fully booked that week" })
    }

    @Test
    fun confirmedBooking_canBeCompletedByAgency() = runTest {
        auth.signInAsDemo("u-zoya")
        assertTrue(bookings.updateStatus("bk-3", BookingStatus.COMPLETED).isSuccess)
    }

    @Test
    fun client_cancelsOwnPendingRequest_only() = runTest {
        auth.signInAsDemo("u-dev")
        assertTrue(bookings.cancelBooking("bk-3").isFailure) // confirmed
        auth.signInAsDemo("u-maya")
        assertTrue(bookings.cancelBooking("bk-1").isSuccess)
        assertEquals(BookingStatus.CANCELLED, bookings.observeBooking("bk-1").first()?.status)
        auth.signInAsDemo("u-zoya")
        assertTrue(bookings.cancelBooking("bk-4").isFailure) // agency can't use the client action
    }

    @Test
    fun detailViewModel_offersActionsForRoleAndStatus() = runTest {
        auth.signInAsDemo("u-maya")
        val clientVm = BookingDetailViewModel("bk-1", auth.currentUser.value!!, bookings, catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { clientVm.uiState.collect {} }
        val clientState = clientVm.uiState.first { !it.isLoading }
        assertTrue(clientState.canCancel)
        assertTrue(clientState.agencyActions.isEmpty())

        auth.signInAsDemo("u-arjun")
        val agencyVm = BookingDetailViewModel("bk-1", auth.currentUser.value!!, bookings, catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { agencyVm.uiState.collect {} }
        val agencyState = agencyVm.uiState.first { !it.isLoading }
        assertEquals(setOf(BookingStatus.CONFIRMED, BookingStatus.DECLINED), agencyState.agencyActions)
        assertEquals(false, agencyState.canCancel)
        agencyVm.setStatus(BookingStatus.CONFIRMED)
        assertEquals(setOf(BookingStatus.COMPLETED), agencyVm.uiState.first { it.booking?.status == BookingStatus.CONFIRMED }.agencyActions)
    }
}
