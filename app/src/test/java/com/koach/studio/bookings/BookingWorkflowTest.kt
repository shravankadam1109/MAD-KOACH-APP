package com.koach.studio.bookings

import com.koach.studio.bookings.data.BookingRequest
import com.koach.studio.bookings.data.BookingValidator
import com.koach.studio.bookings.data.BookingValidator.Field
import com.koach.studio.bookings.data.InMemoryBookingRepository
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
    fun validator_phoneValidation_handlesOptionalAndInvalidFormats() {
        val validReq = request().copy(contactPhone = "+91 98200 12345")
        assertTrue(BookingValidator.validate(validReq, today).isEmpty())

        val blankReq = request().copy(contactPhone = "")
        assertTrue(BookingValidator.validate(blankReq, today).isEmpty())

        val shortPhoneReq = request().copy(contactPhone = "12345")
        val errors1 = BookingValidator.validate(shortPhoneReq, today)
        assertTrue(Field.PHONE in errors1)

        val alphaPhoneReq = request().copy(contactPhone = "+91 98200 abcde")
        val errors2 = BookingValidator.validate(alphaPhoneReq, today)
        assertTrue(Field.PHONE in errors2)
    }

    @Test
    fun newBookingViewModel_handlesBudgetRecommendation_andPhoneInput() = runTest {
        auth.signInAsDemo("u-maya")
        val vm = NewBookingViewModel(auth.currentUser.value!!, "svc-film", "film-2", catalog, bookings, clock)

        vm.onBudgetChanged("75000")
        assertEquals("75000", vm.uiState.value.budget)

        vm.onPhoneChanged("+91 98200 33445")
        assertEquals("+91 98200 33445", vm.uiState.value.contactPhone)

        vm.onBudgetChanged("")
        assertEquals("", vm.uiState.value.budget)
    }
}
