package com.koach.studio.core.firebase

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.auth.PHONE_AUTO_VERIFIED
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** The account exists but its profile couldn't be saved in time; signing in again finishes it. */
class ProvisioningIncompleteException :
    IllegalStateException("Your account was created, but saving your profile is taking too long. Check your connection, then sign in to finish.")

class AccountNotProvisionedException :
    SecurityException("This account hasn't been set up by the studio yet. Ask your KOACH contact to finish your invite.")

/**
 * Real sign-in with Firebase Authentication. Supports Email/Password,
 * Google Authentication, and Phone OTP verification.
 * The signed-in user's role and client come from their `users/{uid}` profile.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val pendingSignUps: PendingSignUpStore = InMemoryPendingSignUpStore(),
    private val provisioningTimeoutMillis: Long = PROVISIONING_TIMEOUT_MILLIS
) : AuthRepository {

    /**
     * Account setup runs here rather than in the calling screen's scope, so
     * leaving the sign-up screen doesn't cancel a profile write half-way.
     */
    private val setupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** One setup per account at a time, shared by sign-up and the session listener. */
    private val setupsInFlight = mutableMapOf<String, Deferred<User>>()

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
        profileListener = firestore.collection(USERS).document(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            // A profile this device has written but the server hasn't saved yet doesn't
            // count: letting the user in on it makes the first screens query before the
            // rules can see the profile, and those listeners stop on the refusal.
            // (Cached profiles from earlier sessions still count, so offline launch works.)
            if (error == null && snapshot?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
            // A late update for an account that has since signed out must not bring it back.
            if (auth.currentUser?.uid != uid) return@addSnapshotListener
            _currentUser.value = if (error != null) null else UserProfileMapper.fromMap(uid, snapshot?.data)
            // A sign-up on this device that never saved its profile: finish it now.
            if (error == null && snapshot != null && !snapshot.exists()) {
                val pending = pendingSignUps.load(uid) ?: return@addSnapshotListener
                setupScope.launch { runCatching { setUpClient(pending, auth.currentUser, isNewAccount = false).await() } }
            }
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
            if (user != null) {
                _currentUser.value = user
                return@runCatching user
            }
            // An interrupted sign-up from this device is completed; any other
            // account without a profile is still waiting for the studio.
            val pending = pendingSignUps.load(uid)
            if (pending == null) {
                auth.signOut()
                throw AccountNotProvisionedException()
            }
            setUpClient(pending, result.user, isNewAccount = false).await()
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
        // Remember what was entered before writing anything, so an interrupted
        // setup can be finished on the next sign-in from this device.
        val pending = PendingSignUp(firebaseUser.uid, name, email, phone, company)
        pendingSignUps.save(pending)
        setUpClient(pending, firebaseUser, isNewAccount = true).await()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        if (idToken.isBlank()) throw IllegalStateException("Google sign-in didn't return an account. Please try again.")
        completeEvenIfCallerLeaves { googleSignIn(idToken) }
    }

    private suspend fun googleSignIn(idToken: String): User {
        val result = try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("No connection. Check your internet and try again.")
        }
        val firebaseUser = result.user ?: throw IllegalStateException("Google sign-in did not return a user")
        return existingProfile(firebaseUser.uid)
            ?: setUpClient(
                PendingSignUp(firebaseUser.uid, firebaseUser.displayName.orEmpty(), firebaseUser.email.orEmpty(), firebaseUser.phoneNumber.orEmpty(), ""),
                firebaseUser,
                isNewAccount = result.additionalUserInfo?.isNewUser == true
            ).await()
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
        completeEvenIfCallerLeaves { phoneSignIn(verificationId, otpCode, name, email, phone) }
    }

    private suspend fun phoneSignIn(verificationId: String, otpCode: String, name: String, email: String, phone: String): User {
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
        return existingProfile(firebaseUser.uid)
            ?: setUpClient(
                PendingSignUp(firebaseUser.uid, name, email, firebaseUser.phoneNumber ?: phone, ""),
                firebaseUser,
                isNewAccount = result.additionalUserInfo?.isNewUser == true
            ).await()
    }

    /**
     * Google and phone sign-in finish here rather than in the calling screen's
     * scope: Firebase completes the sign-in even if the screen is closed, and the
     * profile step after it must not be cancelled half-way, or the account is left
     * signed in to Firebase with no profile.
     */
    private suspend fun <T> completeEvenIfCallerLeaves(block: suspend () -> T): T = setupScope.async { block() }.await()

    private suspend fun existingProfile(uid: String): User? =
        UserProfileMapper.fromMap(uid, firestore.collection(USERS).document(uid).get().await().data)
            ?.also { _currentUser.value = it }

    /** Starts (or joins) the one setup for this account; it keeps running if the caller goes away. */
    private fun setUpClient(details: PendingSignUp, firebaseUser: FirebaseUser?, isNewAccount: Boolean): Deferred<User> =
        synchronized(setupsInFlight) {
            setupsInFlight[details.uid]?.takeIf { it.isActive } ?: setupScope.async {
                try {
                    provisionClient(details, firebaseUser, isNewAccount)
                } finally {
                    synchronized(setupsInFlight) { setupsInFlight.remove(details.uid) }
                }
            }.also { setupsInFlight[details.uid] = it }
        }

    /**
     * Creates the profile and organisation for a new client account. The security
     * rules only allow a self-created profile with role "client" and organisation
     * "cl-<uid>", so this can never create an agency account. It stores only what
     * the person entered and is safe to repeat: each record is written only if it
     * is missing, because clients may create but never update them.
     *
     * - Too slow (over [provisioningTimeoutMillis]) or offline: the account is kept
     *   and the next sign-in on this device finishes the setup.
     * - Refused: a brand-new account is removed so the person can start again.
     */
    private suspend fun provisionClient(details: PendingSignUp, firebaseUser: FirebaseUser?, isNewAccount: Boolean): User {
        val uid = details.uid
        val user = User(
            id = uid,
            name = details.name.trim().ifBlank { details.phone.trim() }.ifBlank { "New client" },
            email = details.email.trim().lowercase(),
            role = Role.CLIENT,
            title = details.company.trim(),
            clientId = "cl-$uid",
            phone = details.phone.trim()
        )
        try {
            withTimeout(provisioningTimeoutMillis) { writeMissingRecords(details, user) }
        } catch (e: TimeoutCancellationException) {
            auth.signOut()
            throw ProvisioningIncompleteException()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Account setup failed: ${e.javaClass.simpleName}: ${e.message}")
            // A write from an earlier, timed-out attempt may have landed in the
            // meantime, making ours a refused update: then the setup did succeed.
            if (runCatching { isProvisioned(uid) }.getOrDefault(false)) {
                pendingSignUps.clear(uid)
                _currentUser.value = user
                return user
            }
            auth.signOut()
            if (e.isOffline()) throw ProvisioningIncompleteException()
            if (isNewAccount) {
                runCatching { firebaseUser?.delete()?.await() }
                pendingSignUps.clear(uid)
            }
            throw IllegalStateException("We couldn't finish setting up your account. Please try again.")
        }
        pendingSignUps.clear(uid)
        _currentUser.value = user
        return user
    }

    /** Creates whichever of the profile and organisation records doesn't exist yet. */
    private suspend fun writeMissingRecords(details: PendingSignUp, user: User) {
        val profileRef = firestore.collection(USERS).document(user.id)
        if (!profileRef.get(Source.SERVER).await().exists()) profileRef.set(UserProfileMapper.toMap(user)).await()
        val clientRef = firestore.collection(CLIENTS).document(user.clientId!!)
        if (!clientRef.get(Source.SERVER).await().exists()) {
            clientRef.set(
                mapOf(
                    "name" to details.company.trim().ifBlank { user.name },
                    "contactName" to user.name,
                    "email" to user.email,
                    "phone" to user.phone,
                    "status" to "ACTIVE",
                    "since" to LocalDate.now().toString()
                )
            ).await()
        }
    }

    private suspend fun isProvisioned(uid: String): Boolean =
        firestore.collection(USERS).document(uid).get(Source.SERVER).await().exists() &&
            firestore.collection(CLIENTS).document("cl-$uid").get(Source.SERVER).await().exists()

    private fun Exception.isOffline(): Boolean =
        this is FirebaseNetworkException ||
            (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE)

    private fun phoneErrorMessage(e: Exception): String = when (e) {
        is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_SESSION_EXPIRED") "That code has expired. Request a new one."
            else if (e.errorCode == "ERROR_INVALID_PHONE_NUMBER") "Enter a valid phone number with country code, e.g. +91 98200 12345."
            else "That code isn't right. Check the SMS and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Wait a while and try again."
        is FirebaseNetworkException -> "No connection. Check your internet and try again."
        // Firebase reports a blocked SMS region as "operation not allowed", which reads
        // as if phone sign-in were switched off.
        is FirebaseAuthException if e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            "Phone codes can't be sent to this number's region yet. Use email or Google instead."
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
        profileListener?.remove()
        profileListener = null
        auth.signOut()
        _currentUser.value = null
    }

    companion object {
        const val USERS = "users"
        const val CLIENTS = "clients"
        private const val TAG = "KoachAuth"
        /** Long enough for a slow first Firestore start, short enough that nobody waits on a frozen screen. */
        const val PROVISIONING_TIMEOUT_MILLIS = 20_000L
    }
}
