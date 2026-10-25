package com.koach.studio.core

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shared access to the LOCAL Firebase emulators for instrumented tests. Tests
 * using it are skipped unless the run passes `firebaseEmulator=true`; nothing
 * here can reach the real project.
 */
object FirebaseEmulator {
    const val HOST = "10.0.2.2"
    const val PROJECT = "koach-studio"
    const val PASSWORD = "test-only-password"

    val enabled: Boolean
        get() = InstrumentationRegistry.getArguments().getString("firebaseEmulator") == "true"

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private var connected = false

    /** Points the SDK at the emulators; safe to call from every test class (useEmulator may only run once). */
    @Synchronized
    fun connect() {
        if (!enabled || connected) return
        auth.useEmulator(HOST, 9099)
        firestore.useEmulator(HOST, 8080)
        connected = true
    }

    /** Unique per call so tests never share accounts; emulator data is discarded when the run ends. */
    fun email(name: String) = "$name-${java.util.UUID.randomUUID().toString().take(8)}@test.koach"

    fun createAccount(email: String, password: String = PASSWORD): String {
        val body = JSONObject().put("email", email).put("password", password).put("returnSecureToken", true)
        val response = http("POST", "http://$HOST:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=emulator", body.toString())
        return JSONObject(response).getString("localId")
    }

    /** Seeds a profile as the emulator "owner", bypassing rules, like an admin console write. */
    fun writeProfile(uid: String, role: String, clientId: String? = null, name: String = "Test $role") {
        val fields = JSONObject()
            .put("role", JSONObject().put("stringValue", role))
            .put("name", JSONObject().put("stringValue", name))
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
