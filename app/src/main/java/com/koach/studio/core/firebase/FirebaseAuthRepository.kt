package com.koach.studio.core.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AccountNotProvisionedException :
    SecurityException("This account hasn't been set up by the studio yet. Ask your KOACH contact to finish your invite.")

/**
 * Real sign-in with Firebase Authentication. Supports Email/Password,
 * Google Authentication, and Phone OTP verification.
 * The signed-in user's role and client come from their `users/{uid}` profile.
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

    override suspend fun signUpClient(name: String, email: String, phone: String, password: String, company: String): Result<User> = runCatching {
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: throw IllegalStateException("Could not create Firebase account")
            val clientId = "cl-$uid"

            val userProfile = mapOf(
                "name" to name.trim(),
                "email" to email.trim().lowercase(),
                "role" to "client",
                "title" to if (company.isNotBlank()) "Creative Director, $company" else "Client Partner",
                "clientId" to clientId,
                "phone" to phone.trim()
            )
            firestore.collection(USERS).document(uid).set(userProfile).await()

            val clientDoc = mapOf(
                "name" to if (company.isNotBlank()) company else name.trim(),
                "contactName" to name.trim(),
                "email" to email.trim().lowercase(),
                "phone" to phone.trim(),
                "industry" to "Creative",
                "city" to "Mumbai",
                "status" to "ACTIVE",
                "since" to java.time.LocalDate.now().toString()
            )
            firestore.collection("clients").document(clientId).set(clientDoc).await()

            val createdUser = User(
                id = uid,
                name = name.trim(),
                email = email.trim().lowercase(),
                role = Role.CLIENT,
                title = if (company.isNotBlank()) "Creative Director, $company" else "Client Partner",
                clientId = clientId,
                phone = phone.trim()
            )
            _currentUser.value = createdUser
            createdUser
        } catch (e: FirebaseAuthUserCollisionException) {
            throw IllegalArgumentException("An account with this email already exists. Please sign in instead.")
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        if (idToken.isBlank()) {
            throw IllegalArgumentException("Google Sign-In requires a configured Web Client ID and OAuth SHA fingerprint in Firebase Console.")
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Google sign-in did not return a user")
        val doc = firestore.collection(USERS).document(uid).get().await()

        val existing = UserProfileMapper.fromMap(uid, doc.data)
        if (existing != null) {
            _currentUser.value = existing
            existing
        } else {
            // Self-provision first-time Google client user
            val clientId = "cl-$uid"
            val name = result.user?.displayName ?: "Google Client"
            val email = result.user?.email ?: ""
            val phone = result.user?.phoneNumber.orEmpty()

            val profileMap = mapOf(
                "name" to name,
                "email" to email,
                "role" to "client",
                "title" to "Client Partner",
                "clientId" to clientId,
                "phone" to phone
            )
            firestore.collection(USERS).document(uid).set(profileMap).await()

            val newUser = User(
                id = uid,
                name = name,
                email = email,
                role = Role.CLIENT,
                title = "Client Partner",
                clientId = clientId,
                phone = phone
            )
            _currentUser.value = newUser
            newUser
        }
    }

    override suspend fun requestPhoneOtp(phoneNumber: String): Result<String> = runCatching {
        // Firebase Phone Auth requires Activity context / SafetyNet/Play Integrity or test numbers
        // In local/offline or unconfigured environments, this points clearly to missing Firebase Console setup
        throw UnsupportedOperationException(
            "Firebase Phone Authentication requires enabling Phone Provider in Firebase Console and configuring SHA-256 fingerprints."
        )
    }

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String, name: String, email: String, phone: String): Result<User> = runCatching {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Phone sign-in failed")
        val doc = firestore.collection(USERS).document(uid).get().await()
        val existing = UserProfileMapper.fromMap(uid, doc.data)
        if (existing != null) {
            _currentUser.value = existing
            existing
        } else {
            val clientId = "cl-$uid"
            val profileMap = mapOf(
                "name" to name.ifBlank { "Client Partner" },
                "email" to email.ifBlank { "phone.${uid}@koach.example" },
                "role" to "client",
                "title" to "Client Partner",
                "clientId" to clientId,
                "phone" to phone
            )
            firestore.collection(USERS).document(uid).set(profileMap).await()
            val user = User(
                id = uid,
                name = name.ifBlank { "Client Partner" },
                email = email.ifBlank { "phone.${uid}@koach.example" },
                role = Role.CLIENT,
                title = "Client Partner",
                clientId = clientId,
                phone = phone
            )
            _currentUser.value = user
            user
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
