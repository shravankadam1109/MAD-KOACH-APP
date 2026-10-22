package com.koach.studio.core.auth

import android.content.Context
import androidx.core.content.edit

/** Keeps the signed-in account id in private app storage so a killed app reopens signed in. */
class SharedPreferencesSessionStore(private val context: Context) : SessionStore {
    private val prefs by lazy { context.getSharedPreferences("koach_session", Context.MODE_PRIVATE) }

    override fun load(): String? = prefs.getString(KEY_USER_ID, null)
    override fun save(userId: String) = prefs.edit { putString(KEY_USER_ID, userId) }
    override fun clear() = prefs.edit { remove(KEY_USER_ID) }

    private companion object {
        const val KEY_USER_ID = "user_id"
    }
}
