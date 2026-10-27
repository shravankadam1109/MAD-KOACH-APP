package com.koach.studio.core.firebase

import android.content.Context
import androidx.core.content.edit

/** What a person entered when signing up, kept until their profile is saved. */
data class PendingSignUp(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val company: String
)

/**
 * Remembers a sign-up whose Firebase account exists but whose profile may not
 * have been saved yet (the app was closed, the screen was left, or Firestore
 * was slow). The next sign-in or launch on this device finishes the setup.
 * Only accounts recorded here are completed automatically; an account with no
 * record (for example agency staff awaiting an admin) is still refused.
 */
interface PendingSignUpStore {
    fun load(uid: String): PendingSignUp?
    fun save(signUp: PendingSignUp)
    fun clear(uid: String)
}

class InMemoryPendingSignUpStore : PendingSignUpStore {
    private val records = mutableMapOf<String, PendingSignUp>()
    override fun load(uid: String): PendingSignUp? = records[uid]
    override fun save(signUp: PendingSignUp) { records[signUp.uid] = signUp }
    override fun clear(uid: String) { records.remove(uid) }
}

/** Kept in the app's private storage; holds at most the one sign-up in progress. */
class SharedPreferencesPendingSignUpStore(context: Context) : PendingSignUpStore {
    private val prefs = context.getSharedPreferences("koach_pending_signup", Context.MODE_PRIVATE)

    override fun load(uid: String): PendingSignUp? {
        if (prefs.getString(KEY_UID, null) != uid) return null
        return PendingSignUp(
            uid = uid,
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            phone = prefs.getString(KEY_PHONE, "").orEmpty(),
            company = prefs.getString(KEY_COMPANY, "").orEmpty()
        )
    }

    // Written synchronously: it must be on disk before the profile write starts,
    // in case the app is closed while that write is in flight.
    override fun save(signUp: PendingSignUp) {
        prefs.edit(commit = true) {
            putString(KEY_UID, signUp.uid)
            putString(KEY_NAME, signUp.name)
            putString(KEY_EMAIL, signUp.email)
            putString(KEY_PHONE, signUp.phone)
            putString(KEY_COMPANY, signUp.company)
        }
    }

    override fun clear(uid: String) {
        if (prefs.getString(KEY_UID, null) == uid) prefs.edit { clear() }
    }

    private companion object {
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_COMPANY = "company"
    }
}
