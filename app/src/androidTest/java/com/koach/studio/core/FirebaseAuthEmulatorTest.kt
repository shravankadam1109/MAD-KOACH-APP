package com.koach.studio.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.koach.studio.core.auth.InvalidCredentialsException
import com.koach.studio.core.firebase.AccountNotProvisionedException
import com.koach.studio.core.firebase.FirebaseAuthRepository
import com.koach.studio.core.model.Role
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

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
        private const val HOST = "10.0.2.2"
        private const val PROJECT = "koach-studio"
        private const val PASSWORD = "test-only-password"

        private val enabled: Boolean
            get() = InstrumentationRegistry.getArguments().getString("firebaseEmulator") == "true"

        private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
        private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

        @BeforeClass
        @JvmStatic
        fun connectToEmulators() {
            if (!enabled) return
            // Must run before the instances are first used.
            auth.useEmulator(HOST, 9099)
            firestore.useEmulator(HOST, 8080)
        }

        /** Unique per call so tests never share accounts; emulator data is discarded when the run ends. */
        private fun email(name: String) = "$name-${java.util.UUID.randomUUID().toString().take(8)}@test.koach"

        private fun createAccount(email: String, password: String): String {
            val body = JSONObject().put("email", email).put("password", password).put("returnSecureToken", true)
            val response = http("POST", "http://$HOST:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=emulator", body.toString())
            return JSONObject(response).getString("localId")
        }

        /** Seeds a profile as the emulator "owner", bypassing rules, like an admin console write. */
        private fun writeProfile(uid: String, role: String, clientId: String? = null) {
            val fields = JSONObject()
                .put("role", JSONObject().put("stringValue", role))
                .put("name", JSONObject().put("stringValue", "Test $role"))
                .put("email", JSONObject().put("stringValue", "$uid@test.koach"))
            if (clientId != null) fields.put("clientId", JSONObject().put("stringValue", clientId))
            http(
                "POST",
                "http://$HOST:8080/v1/projects/$PROJECT/databases/(default)/documents/users?documentId=$uid",
                JSONObject().put("fields", fields).toString(),
                owner = true
            )
        }

        private fun http(method: String, url: String, body: String? = null, owner: Boolean = false): String {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = method
            if (owner) connection.setRequestProperty("Authorization", "Bearer owner")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "$method $url failed: $code $text" }
            return text
        }
    }
}
