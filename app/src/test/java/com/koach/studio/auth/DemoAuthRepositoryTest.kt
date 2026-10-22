package com.koach.studio.auth

import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.InMemorySessionStore
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.data.DemoData
import com.koach.studio.projects.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DemoAuthRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = DemoData(LocalDate.of(2026, 10, 23)).users
    private val password = DemoAuthRepository.DEMO_PASSWORD

    @Test
    fun emailSignIn_acceptsDemoPassword_caseInsensitiveEmail() = runTest {
        val auth = DemoAuthRepository(users)
        val user = auth.signInWithEmail("  MAYA@aetheria.example ", password).getOrThrow()
        assertEquals("u-maya", user.id)
        assertEquals(user, auth.currentUser.value)
    }

    @Test
    fun wrongPasswordAndUnknownEmail_failWithTheSameMessage() = runTest {
        val auth = DemoAuthRepository(users)
        val wrongPassword = auth.signInWithEmail("maya@aetheria.example", "nope-nope").exceptionOrNull()
        val unknownEmail = auth.signInWithEmail("nobody@example.com", password).exceptionOrNull()
        assertTrue(wrongPassword is InvalidCredentialsException)
        assertEquals(wrongPassword?.message, unknownEmail?.message)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun session_survivesRecreation_andSignOutClearsIt() = runTest {
        val store = InMemorySessionStore()
        DemoAuthRepository(users, store).signInAsDemo("u-zoya")
        assertEquals("u-zoya", store.load())

        // A new repository over the same store is what happens after process death.
        val restored = DemoAuthRepository(users, store)
        assertEquals("u-zoya", restored.currentUser.value?.id)

        restored.signOut()
        assertNull(store.load())
        assertNull(DemoAuthRepository(users, store).currentUser.value)
    }

    @Test
    fun staleSession_forUnknownAccount_isIgnored() {
        val store = InMemorySessionStore().apply { save("u-deleted") }
        assertNull(DemoAuthRepository(users, store).currentUser.value)
    }

    @Test
    fun passwordReset_isClearlyUnavailableInDemo() = runTest {
        val result = DemoAuthRepository(users).sendPasswordReset("maya@aetheria.example")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Firebase"))
    }
}
