package com.koach.studio.core.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.koach.studio.core.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Realtime query results as a Flow. Permission errors (e.g. right after sign-out)
 * end in an empty list rather than crashing the screen; other errors are
 * delivered the same way and logged by Firestore itself.
 */
fun <T> Query.observe(map: (QuerySnapshot) -> List<T>): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        when {
            error != null -> trySend(emptyList())
            snapshot != null -> trySend(map(snapshot))
        }
    }
    awaitClose { registration.remove() }
}

fun <T> DocumentReference.observe(map: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        trySend(if (error != null || snapshot == null || !snapshot.exists()) null else map(snapshot))
    }
    awaitClose { registration.remove() }
}

/**
 * Re-runs [query] whenever the signed-in user changes, so each user only
 * listens to data their rules allow (a client query must filter by client).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> StateFlow<User?>.whenSignedIn(empty: T, query: (User) -> Flow<T>): Flow<T> =
    flatMapLatest { user -> if (user == null) flowOf(empty) else query(user) }

/** True for errors that mean "the rules said no", so callers can show a clear message. */
val Throwable.isPermissionDenied: Boolean
    get() = this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED
