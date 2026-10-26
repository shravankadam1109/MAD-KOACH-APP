package com.koach.studio.core.auth

import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Authentication boundary. The demo build signs in with bundled sample accounts;
 * a Firebase Authentication implementation provides email, Google, and Phone OTP
 * sign-in behind the same interface and maps users to a [User] profile.
 */
interface AuthRepository {
    val currentUser: StateFlow<User?>

    /** True when sign-in is simulated and no real accounts exist. */
    val isDemo: Boolean

    /** Sample accounts offered on the sign-in screens. Empty for a real backend. */
    fun demoAccounts(): List<User>

    /** The shared password for sample accounts, shown on screen; null for a real backend. */
    val demoPassword: String?

    suspend fun signInAsDemo(userId: String): Result<User>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signUpClient(name: String, email: String, phone: String, password: String, company: String = ""): Result<User>

    suspend fun signInWithGoogle(idToken: String = ""): Result<User>

    suspend fun requestPhoneOtp(phoneNumber: String): Result<String>

    suspend fun verifyPhoneOtp(verificationId: String, otpCode: String, name: String = "", email: String = "", phone: String = ""): Result<User>

    suspend fun signOut()
}

class InvalidCredentialsException : SecurityException("Email or password is incorrect")

/**
 * Remembers who is signed in across process death. Only an account id is stored,
 * never a password.
 */
interface SessionStore {
    fun load(): String?
    fun save(userId: String)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private var userId: String? = null
    override fun load(): String? = userId
    override fun save(userId: String) {
        this.userId = userId
    }
    override fun clear() {
        userId = null
    }
}

/**
 * Simulated sign-in against bundled sample accounts. Every sample account uses
 * [DEMO_PASSWORD]; it unlocks fictional demo data only and is not a credential
 * for any real system.
 */
class DemoAuthRepository(
    accounts: List<User>,
    private val session: SessionStore = InMemorySessionStore()
) : AuthRepository {

    private val accountList = accounts.toMutableList()
    private val _currentUser = MutableStateFlow(session.load()?.let { id -> accountList.find { it.id == id } })
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override val isDemo: Boolean = true
    override val demoPassword: String = DEMO_PASSWORD

    override fun demoAccounts(): List<User> = accountList.toList()

    override suspend fun signInAsDemo(userId: String): Result<User> {
        val user = accountList.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("Unknown demo account"))
        return Result.success(establish(user))
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val user = accountList.find { it.email.equals(email.trim(), ignoreCase = true) }
        // Same message for unknown email and wrong password, so accounts can't be probed.
        if (user == null || password != DEMO_PASSWORD) return Result.failure(InvalidCredentialsException())
        return Result.success(establish(user))
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        Result.failure(
            UnsupportedOperationException(
                "Password reset emails need Firebase Authentication, which isn't connected in this demo. Demo accounts all use the demo password shown below."
            )
        )

    override suspend fun signUpClient(name: String, email: String, phone: String, password: String, company: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        if (accountList.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("An account with this email already exists. Please sign in instead."))
        }
        val cleanName = name.trim().ifBlank { "Client Partner" }
        val newId = "u-client-" + UUID.randomUUID().toString().take(6)
        val newClient = User(
            id = newId,
            name = cleanName,
            email = cleanEmail,
            role = Role.CLIENT,
            title = if (company.isNotBlank()) "Creative Director, $company" else "Client Partner",
            clientId = "cl-$newId",
            phone = phone.trim()
        )
        accountList.add(newClient)
        return Result.success(establish(newClient))
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        val googleUser = accountList.find { it.email == "google.client@koach.example" }
            ?: run {
                val newId = "u-google-client"
                val created = User(
                    id = newId,
                    name = "Google Client",
                    email = "google.client@koach.example",
                    role = Role.CLIENT,
                    title = "Partner",
                    clientId = "cl-aetheria",
                    phone = "+91 98765 00000"
                )
                accountList.add(created)
                created
            }
        return Result.success(establish(googleUser))
    }

    override suspend fun requestPhoneOtp(phoneNumber: String): Result<String> {
        val clean = phoneNumber.trim().replace(" ", "").replace("-", "")
        if (clean.length < 10) return Result.failure(IllegalArgumentException("Please enter a valid phone number with country code."))
        return Result.success("sim-otp-token-" + UUID.randomUUID().toString().take(6))
    }

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String, name: String, email: String, phone: String): Result<User> {
        if (otpCode.trim().length != 6) {
            return Result.failure(IllegalArgumentException("Invalid verification code. Please enter the 6-digit code."))
        }
        val existing = accountList.find { it.phone.isNotBlank() && it.phone.replace(" ", "") == phone.replace(" ", "") }
        val user = existing ?: run {
            val newId = "u-phone-" + UUID.randomUUID().toString().take(6)
            val created = User(
                id = newId,
                name = name.ifBlank { "Phone Client" },
                email = email.ifBlank { "client.${newId}@koach.example" },
                role = Role.CLIENT,
                title = "Client",
                clientId = "cl-$newId",
                phone = phone
            )
            accountList.add(created)
            created
        }
        return Result.success(establish(user))
    }

    override suspend fun signOut() {
        session.clear()
        _currentUser.value = null
    }

    private fun establish(user: User): User {
        session.save(user.id)
        _currentUser.value = user
        return user
    }

    companion object {
        const val DEMO_PASSWORD = "koachdemo"
    }
}
