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
import com.koach.studio.core.firebase.InMemoryPendingSignUpStore
import com.koach.studio.core.firebase.PendingSignUp
import com.koach.studio.core.firebase.ProvisioningIncompleteException
import com.koach.studio.core.model.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
    fun newClientSignUp_provisionsItsOwnClientProfile_underTheRules() = runBlocking {
        val newEmail = email("signup")
        val user = repository.signUpClient("Ria Shah", newEmail, "+91 98200 33333", PASSWORD, company = "Monsoon Films").getOrThrow()
        assertEquals(Role.CLIENT, user.role)
        assertEquals("cl-${user.id}", user.clientId)

        val profile = firestore.collection("users").document(user.id).get().await()
        assertEquals("client", profile.getString("role"))
        val org = firestore.collection("clients").document("cl-${user.id}").get().await()
        assertEquals("Monsoon Films", org.getString("name"))

        // Signing out and back in finds the same profile; the role can't be raised from the device.
        repository.signOut()
        assertEquals(Role.CLIENT, repository.signInWithEmail(newEmail, PASSWORD).getOrThrow().role)
        val raised = runCatching { firestore.collection("users").document(user.id).update("role", "admin").await() }
        assertTrue("self-provisioned clients must not raise their role", raised.isFailure)
    }

    @Test
    fun interruptedSignUp_isFinishedOnTheNextSignIn() = runBlocking {
        // An account whose profile was never saved, with the sign-up remembered on this device.
        val pendingStore = InMemoryPendingSignUpStore()
        val interruptedEmail = email("interrupted")
        val uid = createAccount(interruptedEmail, PASSWORD)
        pendingStore.save(PendingSignUp(uid, "Ria Shah", interruptedEmail, "+91 98200 44444", "Monsoon Films"))
        val recovering = FirebaseAuthRepository(auth, firestore, pendingStore)

        val user = recovering.signInWithEmail(interruptedEmail, PASSWORD).getOrThrow()
        assertEquals(Role.CLIENT, user.role)
        assertEquals("cl-$uid", user.clientId)
        assertEquals("client", firestore.collection("users").document(uid).get().await().getString("role"))
        assertEquals("Monsoon Films", firestore.collection("clients").document("cl-$uid").get().await().getString("name"))
        assertNull("the pending sign-up is cleared once finished", pendingStore.load(uid))
    }

    @Test
    fun stuckSetup_reportsAnError_keepsTheAccount_andFinishesOnSignIn() = runBlocking {
        val slowStore = InMemoryPendingSignUpStore()
        val slowEmail = email("slow")
        // A 1 ms limit makes the profile write "too slow" every time.
        val impatient = FirebaseAuthRepository(auth, firestore, slowStore, provisioningTimeoutMillis = 1)
        val error = impatient.signUpClient("Kabir Rao", slowEmail, "+91 98200 55555", PASSWORD).exceptionOrNull()
        assertTrue("got $error", error is ProvisioningIncompleteException)
        assertNull("not left signed in half-way", auth.currentUser)

        // The account and the remembered sign-up were kept. Hand the record to a normal
        // repository (the app has exactly one; this test briefly has two on one sign-in).
        val uid = createdUid(slowEmail)
        val record = slowStore.load(uid)!!
        slowStore.clear(uid)
        val normalStore = InMemoryPendingSignUpStore().apply { save(record) }
        val user = FirebaseAuthRepository(auth, firestore, normalStore).signInWithEmail(slowEmail, PASSWORD).getOrThrow()
        assertEquals(Role.CLIENT, user.role)
        assertEquals("Kabir Rao", firestore.collection("users").document(user.id).get().await().getString("name"))
    }

    @Test
    fun profileNotYetSavedByTheServer_doesNotSignTheUserIn() = runBlocking {
        val pendingEmail = email("pending")
        val uid = createAccount(pendingEmail, PASSWORD)
        auth.signInWithEmailAndPassword(pendingEmail, PASSWORD).await()
        withTimeout(10_000) { while (auth.currentUser?.uid != uid) delay(50) }

        // Written on the device only: Firestore shows it locally with pending writes.
        firestore.disableNetwork().await()
        firestore.collection("users").document(uid)
            .set(mapOf("role" to "client", "name" to "Pending", "email" to pendingEmail, "clientId" to "cl-$uid", "title" to "", "phone" to ""))
        delay(1_500)
        assertNull("must not sign in on a profile the server hasn't saved", repository.currentUser.value)

        // Once the server has it, the user is let in.
        firestore.enableNetwork().await()
        val user = withTimeout(15_000) { repository.currentUser.first { it != null } }
        assertEquals(uid, user?.id)
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

    /** The uid of an existing emulator account, via a plain sign-in that doesn't touch Firestore. */
    private suspend fun createdUid(email: String): String {
        val uid = auth.signInWithEmailAndPassword(email, PASSWORD).await().user!!.uid
        auth.signOut()
        return uid
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun connectToEmulators() = FirebaseEmulator.connect()
    }
}
