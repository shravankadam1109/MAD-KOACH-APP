package com.koach.studio.core.firebase

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * The foreground activity, held weakly. Firebase Phone Authentication needs an
 * activity to run its app-verification (Play Integrity / reCAPTCHA) step.
 */
object CurrentActivity : Application.ActivityLifecycleCallbacks {
    private var ref: WeakReference<Activity>? = null

    val value: Activity? get() = ref?.get()

    override fun onActivityResumed(activity: Activity) { ref = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) { if (ref?.get() === activity) ref = null }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
