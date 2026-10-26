package com.koach.studio.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.core.FirebaseEmulator.PASSWORD
import com.koach.studio.core.FirebaseEmulator.auth
import com.koach.studio.core.FirebaseEmulator.createAccount
import com.koach.studio.core.FirebaseEmulator.email
import com.koach.studio.core.FirebaseEmulator.enabled
import com.koach.studio.core.FirebaseEmulator.firestore
import com.koach.studio.core.FirebaseEmulator.writeProfile
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.firebase.AccountNotProvisionedException
import com.koach.studio.core.firebase.FirebaseAuthRepository
import com.koach.studio.core.model.Role
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Firebase Authentication + Firestore rules, against the LOCAL emulators
 * only. Skipped unless run with the emulators up and the opt-in argument:
 *
 *   firebase emulators:exec --project koach-studio --only auth,firestore,storage \
 *     "./gradlew connectedDebugAndroidTest \
 *        -Pandroid.testInstrumentationRunnerArguments.firebaseEmulator=true \
 *        -Pandroid.testInstrumentationRunnerArguments.class=com.koach.studio.core.FirebaseAuthEmulatorTest"
 */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthEmulatorTest {

    private lateinit var repository: FirebaseAuthRepository

    @Before
    fun setUp() {
        assumeTrue("Firebase emulator tests not requested", enabled)
        auth.signOut()
        repository = FirebaseAuthRepository(auth, firestore)
    }

    @Test
    fun provisionedClient_signsIn_withRoleFromTrustedProfile() = runBlocking {
        val clientEmail = email("client")
        val uid = createAccount(clientEmail, PASSWORD)
        writeProfile(uid, role = "client", clientId = "cl-test")

        val user = repository.signInWithEmail(clientEmail, PASSWORD).getOrThrow()
        assertEquals(uid, user.id)
        assertEquals(Role.CLIENT, user.role)
        assertEquals("cl-test", user.clientId)
        repository.signOut()
        assertNull(auth.currentUser)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun accountWithoutProfile_isRefusedAndSignedOut() = runBlocking {
        val noprofileEmail = email("noprofile")
        createAccount(noprofileEmail, PASSWORD)
        val error = repository.signInWithEmail(noprofileEmail, PASSWORD).exceptionOrNull()
        assertTrue("got $error", error is AccountNotProvisionedException)
        assertNull(auth.currentUser)
    }

    @Test
    fun wrongPasswordAndUnknownEmail_giveTheSameGenericError() = runBlocking {
        val teamEmail = email("team")
        val uid = createAccount(teamEmail, PASSWORD)
        writeProfile(uid, role = "team")
        val wrongPassword = repository.signInWithEmail(teamEmail, "wrong-password").exceptionOrNull()
        val unknownEmail = repository.signInWithEmail(email("nobody"), PASSWORD).exceptionOrNull()
        assertTrue("got $wrongPassword", wrongPassword is InvalidCredentialsException)
        assertTrue("got $unknownEmail", unknownEmail is InvalidCredentialsException)
    }

    @Test
    fun signedInClient_cannotPromoteThemselves_underTheDeployedRules() = runBlocking {
        val sneakyEmail = email("sneaky")
        val uid = createAccount(sneakyEmail, PASSWORD)
        writeProfile(uid, role = "client", clientId = "cl-test")
        repository.signInWithEmail(sneakyEmail, PASSWORD).getOrThrow()

        val denied = runCatching { firestore.collection("users").document(uid).update("role", "admin").await() }
        assertTrue("profile self-update must be rejected by firestore.rules", denied.isFailure)
        assertEquals(Role.CLIENT, repository.signInWithEmail(sneakyEmail, PASSWORD).getOrThrow().role)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun connectToEmulators() = FirebaseEmulator.connect()
    }
}
