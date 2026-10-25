package com.koach.studio.core.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AccountNotProvisionedException :
    SecurityException("This account hasn't been set up by the studio yet. Ask your KOACH contact to finish your invite.")

/**
 * Real sign-in with Firebase Authentication. The signed-in user's role and
 * client come from their `users/{uid}` profile, which only admins can write.
 * Firebase keeps the session across restarts itself.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override val isDemo: Boolean = false
    override val demoPassword: String? = null
    override fun demoAccounts(): List<User> = emptyList()

    private var profileListener: ListenerRegistration? = null

    init {
        // Restores a previous session and follows profile changes (e.g. a role change by an admin).
        auth.addAuthStateListener { firebaseAuth -> watchProfile(firebaseAuth.currentUser?.uid) }
    }

    private fun watchProfile(uid: String?) {
        profileListener?.remove()
        profileListener = null
        if (uid == null) {
            _currentUser.value = null
            return
        }
        profileListener = firestore.collection(USERS).document(uid).addSnapshotListener { snapshot, error ->
            _currentUser.value = if (error != null) null else UserProfileMapper.fromMap(uid, snapshot?.data)
        }
    }

    override suspend fun signInAsDemo(userId: String): Result<User> =
        Result.failure(UnsupportedOperationException("Demo accounts aren't available when signed in with Firebase."))

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: throw InvalidCredentialsException()
            val profile = firestore.collection(USERS).document(uid).get().await()
            val user = UserProfileMapper.fromMap(uid, profile.data)
            if (user == null) {
                auth.signOut()
                throw AccountNotProvisionedException()
            }
            _currentUser.value = user
            user
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw InvalidCredentialsException()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Same message as a wrong password so accounts can't be probed.
            throw InvalidCredentialsException()
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        } catch (e: FirebaseTooManyRequestsException) {
            throw IllegalStateException("Too many attempts. Wait a moment and try again.")
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // Don't reveal whether the email has an account.
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        }
        Unit
    }

    override suspend fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    companion object {
        const val USERS = "users"
    }
}
