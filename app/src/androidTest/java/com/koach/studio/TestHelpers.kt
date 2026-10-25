package com.koach.studio

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.ComposeTestRule
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

/**
 * Waits until a node with [tag] exists. Tolerates the brief window, right after
 * the sign-in screen hands over to the app shell, in which Compose reports no
 * hierarchy and lookups throw.
 */
fun ComposeTestRule.awaitTag(tag: String, timeoutMillis: Long = 10_000) {
    waitUntil(timeoutMillis) {
        runCatching { onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    }
}

/**
 * Signs in with a demo account through the right portal and waits for the app
 * shell. The tap is retried if the shell doesn't appear, because emulator focus
 * changes occasionally swallow the first tap after launch.
 */
fun ComposeTestRule.signInAs(userId: String, shellTag: String, attempts: Int = 3) {
    val portalTag = if (shellTag == "tab_agency_home") "enter_team" else "enter_client"
    repeat(attempts) { attempt ->
        runCatching {
            if (onAllNodes(hasTestTag("welcome_screen")).fetchSemanticsNodes().isNotEmpty()) {
                onNode(hasTestTag(portalTag)).performClick()
                awaitTag("demo_account_$userId", timeoutMillis = 5_000)
            }
            onNode(hasTestTag("demo_account_$userId")).performScrollTo().performClick()
        }
        val appeared = runCatching { awaitTag(shellTag, timeoutMillis = 5_000) }.isSuccess
        if (appeared) return
        if (attempt == attempts - 1) awaitTag(shellTag, timeoutMillis = 1)
    }
}
