package com.koach.studio.core.firebase

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.auth.PHONE_AUTO_VERIFIED
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val result = try {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            throw IllegalArgumentException("An account with this email already exists. Please sign in instead.")
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        }
        val firebaseUser = result.user ?: throw IllegalStateException("Could not create your account. Please try again.")
        provisionNewClient(firebaseUser, name = name, email = email, phone = phone, company = company)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        if (idToken.isBlank()) throw IllegalStateException("Google sign-in didn't return an account. Please try again.")
        val result = try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        }
        val firebaseUser = result.user ?: throw IllegalStateException("Google sign-in did not return a user")
        existingProfile(firebaseUser.uid)
            ?: provisionNewClient(
                firebaseUser,
                name = firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email.orEmpty(),
                phone = firebaseUser.phoneNumber.orEmpty(),
                deleteOnFailure = result.additionalUserInfo?.isNewUser == true
            )
    }

    /** Resend token from the last code sent, so "Resend" reuses Firebase's session for that number. */
    private var lastPhone: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    /** Set when Android verified the number itself (auto-retrieved SMS); used instead of a typed code. */
    private var autoCredential: PhoneAuthCredential? = null

    override suspend fun requestPhoneOtp(phoneNumber: String): Result<String> = runCatching {
        val e164 = phoneNumber.filter { it.isDigit() || it == '+' }
        val activity = CurrentActivity.value ?: throw IllegalStateException("Open the app and try again.")
        autoCredential = null
        suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    lastPhone = e164
                    resendToken = token
                    if (cont.isActive) cont.resume(verificationId)
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    autoCredential = credential
                    if (cont.isActive) cont.resume(PHONE_AUTO_VERIFIED)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(IllegalStateException(phoneErrorMessage(e)))
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(e164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .apply { if (e164 == lastPhone) resendToken?.let { setForceResendingToken(it) } }
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String, name: String, email: String, phone: String): Result<User> = runCatching {
        val credential = if (verificationId == PHONE_AUTO_VERIFIED) {
            autoCredential ?: throw IllegalStateException("Verification expired. Request a new code.")
        } else {
            PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
        }
        val result = try {
            auth.signInWithCredential(credential).await()
        } catch (e: FirebaseException) {
            throw IllegalStateException(phoneErrorMessage(e))
        }
        autoCredential = null
        val firebaseUser = result.user ?: throw IllegalStateException("Phone sign-in failed. Please try again.")
        existingProfile(firebaseUser.uid)
            ?: provisionNewClient(
                firebaseUser,
                name = name,
                email = email,
                phone = firebaseUser.phoneNumber ?: phone,
                deleteOnFailure = result.additionalUserInfo?.isNewUser == true
            )
    }

    private suspend fun existingProfile(uid: String): User? =
        UserProfileMapper.fromMap(uid, firestore.collection(USERS).document(uid).get().await().data)
            ?.also { _currentUser.value = it }

    /**
     * Creates the profile and organisation for a brand-new client account. The
     * security rules only allow a self-created profile with role "client" and
     * organisation "cl-<uid>", so this can never create an agency account.
     * Only what the person entered is stored. If provisioning fails, a freshly
     * created Firebase account is removed so they can simply try again.
     */
    private suspend fun provisionNewClient(
        firebaseUser: FirebaseUser,
        name: String,
        email: String,
        phone: String,
        company: String = "",
        deleteOnFailure: Boolean = true
    ): User {
        val uid = firebaseUser.uid
        val user = User(
            id = uid,
            name = name.trim().ifBlank { phone.trim() }.ifBlank { "New client" },
            email = email.trim().lowercase(),
            role = Role.CLIENT,
            title = company.trim(),
            clientId = "cl-$uid",
            phone = phone.trim()
        )
        try {
            firestore.collection(USERS).document(uid).set(UserProfileMapper.toMap(user)).await()
            firestore.collection(CLIENTS).document(user.clientId!!).set(
                mapOf(
                    "name" to company.trim().ifBlank { user.name },
                    "contactName" to user.name,
                    "email" to user.email,
                    "phone" to user.phone,
                    "status" to "ACTIVE",
                    "since" to LocalDate.now().toString()
                )
            ).await()
        } catch (e: Exception) {
            if (deleteOnFailure) runCatching { firebaseUser.delete().await() }
            auth.signOut()
            throw IllegalStateException(
                if (e is FirebaseNetworkException) "No connection. Check your internet and try again."
                else "We couldn't finish setting up your account. Please try again."
            )
        }
        _currentUser.value = user
        return user
    }

    private fun phoneErrorMessage(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_SESSION_EXPIRED") "That code has expired. Request a new one."
            else if (e.errorCode == "ERROR_INVALID_PHONE_NUMBER") "Enter a valid phone number with country code, e.g. +91 98200 12345."
            else "That code isn't right. Check the SMS and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Wait a while and try again."
        is FirebaseNetworkException -> "No connection. Check your internet and try again."
        else -> e.message ?: "Phone verification failed. Please try again."
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
        const val CLIENTS = "clients"
    }
}
