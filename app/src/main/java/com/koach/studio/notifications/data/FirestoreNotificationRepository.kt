package com.koach.studio.notifications.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.koach.studio.core.auth.NotSignedInException
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** In-app notifications in `notifications/{id}`, scoped to the signed-in recipient. */
class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>
) : NotificationRepository {

    private val collection get() = firestore.collection(COLLECTION)

    override fun observeNotifications(): Flow<List<AppNotification>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            collection.whereEqualTo("recipientId", user.id)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .observe { snap -> snap.documents.mapNotNull { FirestoreMappers.notification(it.id, it.data.orEmpty()) } }
        }

    override fun observeUnreadCount(): Flow<Int> = observeNotifications().map { list -> list.count { !it.isRead } }

    override suspend fun markRead(notificationId: String): Result<Unit> = runCatching {
        currentUser.value ?: throw NotSignedInException()
        collection.document(notificationId).update("isRead", true).await()
        Unit
    }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        val user = currentUser.value ?: throw NotSignedInException()
        val unread = collection.whereEqualTo("recipientId", user.id).whereEqualTo("isRead", false).get().await()
        if (unread.isEmpty) return@runCatching
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "isRead", true) }
        batch.commit().await()
        Unit
    }

    /** Best effort: a failed notification must never fail the action that triggered it. */
    override suspend fun push(notification: AppNotification) {
        runCatching { collection.document().set(FirestoreMappers.notificationFields(notification.copy(isRead = false))).await() }
    }

    companion object {
        const val COLLECTION = "notifications"
    }
}
