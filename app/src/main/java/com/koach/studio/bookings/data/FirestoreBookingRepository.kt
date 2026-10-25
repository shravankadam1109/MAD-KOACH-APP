package com.koach.studio.bookings.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.User
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Bookings in `bookings/{id}`. Same rules as the in-memory repository, checked
 * here for clear errors and enforced again by firestore.rules.
 */
class FirestoreBookingRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : BookingRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeBookings(): Flow<List<Booking>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val query = if (user.role.isAgency) collection else collection.whereEqualTo("clientId", user.clientId)
            query.orderBy("createdOn", Query.Direction.DESCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.booking(it.id, it.data.orEmpty()) } }
        }

    override fun observeBooking(bookingId: String): Flow<Booking?> =
        observeBookings().map { list -> list.find { it.id == bookingId } }

    override suspend fun createBooking(request: BookingRequest): Result<Booking> = runCatching {
        val user = access.require(Permission.CREATE_BOOKINGS)
        val problems = BookingValidator.validate(request, LocalDate.now(clock))
        require(problems.isEmpty()) { problems.values.first() }
        val ref = collection.document()
        val booking = Booking(
            id = ref.id, clientId = user.clientId, requestedBy = user.id, contactName = request.contactName.trim(),
            contactEmail = request.contactEmail.trim(), serviceId = request.serviceId, packageId = request.packageId,
            preferredDate = request.preferredDate, budgetInr = request.budgetInr, details = request.details.trim(),
            status = BookingStatus.PENDING, createdOn = LocalDate.now(clock)
        )
        ref.set(FirestoreMappers.bookingFields(booking)).await()
        notifyAdmins("New booking request", "${booking.contactName} sent a booking request.", booking.id)
        booking
    }

    override suspend fun updateStatus(bookingId: String, status: BookingStatus, note: String?): Result<Unit> = runCatching {
        access.require(Permission.MANAGE_BOOKINGS)
        val booking = load(bookingId)
        require(status in BookingRules.agencyTransitions(booking.status)) {
            "A ${booking.status.displayName.lowercase()} booking can't be marked ${status.displayName.lowercase()}"
        }
        val trimmedNote = note?.trim()?.ifBlank { null }
        require(status != BookingStatus.DECLINED || trimmedNote != null) { "Add a short reason for the client" }
        collection.document(bookingId).update(mapOf("status" to status.name, "statusNote" to trimmedNote)).await()
        notify(
            booking.requestedBy, "Booking ${status.displayName.lowercase()}",
            trimmedNote ?: "Your booking for ${booking.preferredDate} is now ${status.displayName.lowercase()}.", booking.id
        )
    }

    override suspend fun cancelBooking(bookingId: String): Result<Unit> = runCatching {
        val user = access.require(Permission.CREATE_BOOKINGS)
        val booking = load(bookingId)
        if (booking.requestedBy != user.id) throw PermissionDeniedException("Only the person who made the request can cancel it")
        check(BookingRules.clientCanCancel(booking.status)) { "Only pending requests can be cancelled. Message the studio instead." }
        collection.document(bookingId).update(mapOf("status" to BookingStatus.CANCELLED.name, "statusNote" to null)).await()
        notifyAdmins("Booking cancelled", "${booking.contactName} cancelled a pending request.", booking.id)
    }

    private suspend fun load(bookingId: String): Booking {
        val snap = collection.document(bookingId).get().await()
        return FirestoreMappers.booking(snap.id, snap.data.orEmpty()) ?: throw IllegalArgumentException("Booking not found")
    }

    private suspend fun notifyAdmins(title: String, body: String, targetId: String) {
        val admins = runCatching { firestore.collection("users").whereEqualTo("role", "admin").get().await() }.getOrNull() ?: return
        admins.documents.forEach { notify(it.id, title, body, targetId) }
    }

    private suspend fun notify(recipientId: String, title: String, body: String, targetId: String) =
        notifications.push(AppNotification("", recipientId, NotificationType.BOOKING, title, body, LocalDateTime.now(clock), false, targetId))

    companion object {
        const val COLLECTION = "bookings"
    }
}
