package com.koach.studio

import androidx.test.core.app.ApplicationProvider
import com.koach.studio.core.di.DemoAppContainer
import org.junit.rules.ExternalResource

/**
 * Installs fresh in-memory demo data before the activity launches. Chain it
 * outside the compose rule:
 * `RuleChain.outerRule(FreshDemoData()).around(createAndroidComposeRule<MainActivity>())`.
 */
class FreshDemoData : ExternalResource() {
    override fun before() {
        ApplicationProvider.getApplicationContext<KoachApplication>().container = DemoAppContainer()
    }
}
