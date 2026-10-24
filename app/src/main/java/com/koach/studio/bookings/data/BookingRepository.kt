package com.koach.studio.bookings.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BookingRequest(
    val serviceId: String,
    val packageId: String?,
    val preferredDate: LocalDate,
    val budgetInr: Long?,
    val details: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String = ""
)

interface BookingRepository {
    /** Clients see their own organisation's bookings; agency users see the full inbox. */
    fun observeBookings(): Flow<List<Booking>>
    fun observeBooking(bookingId: String): Flow<Booking?>
    suspend fun createBooking(request: BookingRequest): Result<Booking>
    suspend fun updateStatus(bookingId: String, status: BookingStatus): Result<Unit>
}

class InMemoryBookingRepository(
    initial: List<Booking>,
    private val users: List<User>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : BookingRepository {

    private val store = MutableStateFlow(initial)

    override fun observeBookings(): Flow<List<Booking>> =
        combine(store, currentUser) { all, user ->
            when {
                user == null -> emptyList()
                user.role.isAgency -> all
                else -> all.filter { it.requestedBy == user.id || (it.clientId != null && it.clientId == user.clientId) }
            }.sortedByDescending { it.createdOn }
        }

    override fun observeBooking(bookingId: String): Flow<Booking?> =
        observeBookings().map { list -> list.find { it.id == bookingId } }

    override suspend fun createBooking(request: BookingRequest): Result<Booking> = runCatching {
        val user = access.require(Permission.CREATE_BOOKINGS)
        val problems = BookingValidator.validate(request, LocalDate.now(clock))
        require(problems.isEmpty()) { problems.values.first() }
        val booking = Booking(
            id = "bk-" + UUID.randomUUID().toString().take(8),
            clientId = user.clientId,
            requestedBy = user.id,
            contactName = request.contactName.trim(),
            contactEmail = request.contactEmail.trim(),
            serviceId = request.serviceId,
            packageId = request.packageId,
            preferredDate = request.preferredDate,
            budgetInr = request.budgetInr,
            details = request.details.trim(),
            status = BookingStatus.PENDING,
            createdOn = LocalDate.now(clock)
        )
        store.update { listOf(booking) + it }
        users.filter { it.role == Role.ADMIN }.forEach { admin ->
            notifications.push(
                AppNotification(
                    id = "n-" + UUID.randomUUID().toString().take(8),
                    recipientId = admin.id,
                    type = NotificationType.BOOKING,
                    title = "New booking request",
                    body = "${booking.contactName} sent a booking request.",
                    createdAt = LocalDateTime.now(clock),
                    isRead = false,
                    targetId = booking.id
                )
            )
        }
        booking
    }

    override suspend fun updateStatus(bookingId: String, status: BookingStatus): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_BOOKINGS)
        val booking = store.value.find { it.id == bookingId } ?: throw IllegalArgumentException("Booking not found")
        store.update { list -> list.map { if (it.id == bookingId) it.copy(status = status) else it } }
        booking.requestedBy.takeIf { requester -> users.any { it.id == requester && it.role == Role.CLIENT } }?.let { clientUserId ->
            notifications.push(
                AppNotification(
                    id = "n-" + UUID.randomUUID().toString().take(8),
                    recipientId = clientUserId,
                    type = NotificationType.BOOKING,
                    title = "Booking ${status.displayName.lowercase()}",
                    body = "Your booking for ${booking.preferredDate} is now ${status.displayName.lowercase()}.",
                    createdAt = LocalDateTime.now(clock),
                    isRead = false,
                    targetId = booking.id
                )
            )
        }
    }
}
