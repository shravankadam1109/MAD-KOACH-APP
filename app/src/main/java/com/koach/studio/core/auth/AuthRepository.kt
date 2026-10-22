package com.koach.studio.core.auth

import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication boundary. The demo build signs in with bundled sample accounts;
 * a Firebase Authentication implementation can provide email sign-in behind the
 * same interface and map its user to a [User] profile stored in Firestore.
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
    private val accounts: List<User>,
    private val session: SessionStore = InMemorySessionStore()
) : AuthRepository {

    private val _currentUser = MutableStateFlow(session.load()?.let { id -> accounts.find { it.id == id } })
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override val isDemo: Boolean = true
    override val demoPassword: String = DEMO_PASSWORD

    override fun demoAccounts(): List<User> = accounts

    override suspend fun signInAsDemo(userId: String): Result<User> {
        val user = accounts.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("Unknown demo account"))
        return Result.success(establish(user))
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val user = accounts.find { it.email.equals(email.trim(), ignoreCase = true) }
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
