package com.koach.studio.notifications.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.NotSignedInException
import com.koach.studio.core.model.AppNotification
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Notifications for the signed-in user. A realtime backend (e.g. Firestore
 * listeners plus FCM) can implement the same flows.
 */
interface NotificationRepository {
    /** Newest first, only the current user's. */
    fun observeNotifications(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun markRead(notificationId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    /** Used by other modules to notify a user, e.g. when a booking is made. */
    suspend fun push(notification: AppNotification)
}

class InMemoryNotificationRepository(
    initial: List<AppNotification>,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl
) : NotificationRepository {

    private val store = MutableStateFlow(initial)

    override fun observeNotifications(): Flow<List<AppNotification>> =
        combine(store, currentUser) { all, user ->
            if (user == null) emptyList()
            else all.filter { it.recipientId == user.id }.sortedByDescending { it.createdAt }
        }

    override fun observeUnreadCount(): Flow<Int> = observeNotifications().map { list -> list.count { !it.isRead } }

    override suspend fun markRead(notificationId: String): Result<Unit> = runCatching {
        val user = access.user() ?: throw NotSignedInException()
        store.update { list ->
            list.map { if (it.id == notificationId && it.recipientId == user.id) it.copy(isRead = true) else it }
        }
    }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        val user = access.user() ?: throw NotSignedInException()
        store.update { list -> list.map { if (it.recipientId == user.id) it.copy(isRead = true) else it } }
    }

    override suspend fun push(notification: AppNotification) {
        store.update { listOf(notification) + it }
    }
}
