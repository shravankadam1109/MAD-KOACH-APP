package com.koach.studio

import android.app.Application
import com.koach.studio.core.auth.SharedPreferencesSessionStore
import com.koach.studio.core.di.AppContainer
import com.koach.studio.core.di.DemoAppContainer

class KoachApplication : Application() {
    /**
     * Created on first use. The session store remembers who was signed in; the
     * demo data itself is in memory and resets when the process dies.
     * Replaced only by instrumented tests, before any screen is launched.
     */
    var container: AppContainer = DemoAppContainer(sessionStore = SharedPreferencesSessionStore(this))
}
