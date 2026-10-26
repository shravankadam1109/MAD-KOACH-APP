package com.koach.studio

import android.app.Application
import com.google.firebase.FirebaseApp
import com.koach.studio.core.auth.SharedPreferencesSessionStore
import com.koach.studio.core.di.AppContainer
import com.koach.studio.core.di.DemoAppContainer
import com.koach.studio.core.firebase.FirebaseAppContainer
import com.koach.studio.core.settings.SharedPreferencesThemePreferencesStore

class KoachApplication : Application() {
    /**
     * Demo by default. The Firebase backend is used only when the build opted in
     * (`-Pkoach.backend=firebase`) and a google-services.json configured Firebase.
     * Replaced only by instrumented tests, before any screen is launched.
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        if (!::container.isInitialized) {
            val firebaseConfigured = FirebaseApp.getApps(this).isNotEmpty()
            val themeStore = SharedPreferencesThemePreferencesStore(this)
            container = if (BuildConfig.FIREBASE_BACKEND && firebaseConfigured) {
                FirebaseAppContainer(
                    emulatorHost = if (BuildConfig.FIREBASE_EMULATOR) "10.0.2.2" else null,
                    themePreferencesStore = themeStore
                )
            } else {
                DemoAppContainer(
                    sessionStore = SharedPreferencesSessionStore(this),
                    themePreferencesStore = themeStore
                )
            }
        }
    }
}
