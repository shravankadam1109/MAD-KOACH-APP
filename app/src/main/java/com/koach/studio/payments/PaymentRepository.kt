package com.koach.studio.payments

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.NotificationType
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import com.koach.studio.core.ui.formatInr
import com.koach.studio.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Invoice records. No payment processor is connected: [markPaidSimulated]
 * only flips the record's status so the flow can be demonstrated.
 */
interface PaymentRepository {
    /** Clients see their organisation's invoices; studio admins see all; other agency users none. */
    fun observePayments(): Flow<List<Payment>>
    suspend fun markPaidSimulated(paymentId: String): Result<Unit>
}

class InMemoryPaymentRepository(
    initial: List<Payment>,
    private val users: List<User>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl,
    private val notifications: NotificationRepository,
    private val clock: Clock
) : PaymentRepository {

    private val store = MutableStateFlow(initial)

    override fun observePayments(): Flow<List<Payment>> =
        combine(store, currentUser) { all, user ->
            when {
                user == null -> emptyList()
                user.can(Permission.VIEW_ALL_PAYMENTS) -> all
                user.role == Role.CLIENT -> all.filter { it.clientId == user.clientId }
                else -> emptyList()
            }.sortedByDescending { it.issuedOn }
        }

    override suspend fun markPaidSimulated(paymentId: String): Result<Unit> = runCatching {
        val user = access.user() ?: throw PermissionDeniedException("Please sign in")
        val payment = store.value.find { it.id == paymentId } ?: throw IllegalArgumentException("Invoice not found")
        if (user.role != Role.CLIENT || payment.clientId != user.clientId) {
            throw PermissionDeniedException("Only the client can pay their invoice")
        }
        check(payment.status != PaymentStatus.PAID) { "This invoice is already paid" }
        val today = LocalDate.now(clock)
        store.update { list -> list.map { if (it.id == paymentId) it.copy(status = PaymentStatus.PAID, paidOn = today) else it } }
        users.filter { it.role == Role.ADMIN }.forEach { admin ->
            notifications.push(
                AppNotification(
                    id = "n-" + UUID.randomUUID().toString().take(8),
                    recipientId = admin.id,
                    type = NotificationType.PAYMENT,
                    title = "Invoice ${payment.invoiceNumber} marked paid",
                    body = "${formatInr(payment.amountInr)} (simulated payment, no money moved).",
                    createdAt = LocalDateTime.now(clock),
                    isRead = false
                )
            )
        }
    }
}
