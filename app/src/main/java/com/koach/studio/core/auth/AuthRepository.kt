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

    /** Sample accounts offered on the sign-in screen. Empty for a real backend. */
    fun demoAccounts(): List<User>

    suspend fun signInAsDemo(userId: String): Result<User>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    suspend fun signOut()
}

class DemoAuthRepository(private val accounts: List<User>) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override val isDemo: Boolean = true

    override fun demoAccounts(): List<User> = accounts

    override suspend fun signInAsDemo(userId: String): Result<User> {
        val user = accounts.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("Unknown demo account"))
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        Result.failure(UnsupportedOperationException("Email sign-in needs Firebase Authentication, which isn't connected in this demo build."))

    override suspend fun signOut() {
        _currentUser.value = null
    }
}
