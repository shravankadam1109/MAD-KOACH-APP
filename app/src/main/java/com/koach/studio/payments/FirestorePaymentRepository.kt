package com.koach.studio.payments

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Invoice records in `payments/{id}`. Read-only from the app: firestore.rules
 * never let a client device change a payment, and real payment confirmation
 * will happen server-side once a payment provider is integrated (Phase 4).
 */
class FirestorePaymentRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl
) : PaymentRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observePayments(): Flow<List<Payment>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val query = when {
                access.can(Permission.VIEW_ALL_PAYMENTS) -> collection
                user.role == Role.CLIENT -> collection.whereEqualTo("clientId", user.clientId)
                else -> return@whenSignedIn flowOf(emptyList())
            }
            query.orderBy("issuedOn", Query.Direction.DESCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.payment(it.id, it.data.orEmpty()) } }
        }

    override suspend fun markPaidSimulated(paymentId: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("Online payment isn't available yet. The studio will confirm payment once it's received.")
    )

    companion object {
        const val COLLECTION = "payments"
    }
}
