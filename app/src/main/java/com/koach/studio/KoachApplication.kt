package com.koach.studio

import android.app.Application
import com.koach.studio.core.di.AppContainer
import com.koach.studio.core.di.DemoAppContainer

class KoachApplication : Application() {
    /** Replaced only by instrumented tests, before any screen is launched. */
    var container: AppContainer = DemoAppContainer()
}
