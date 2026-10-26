package com.koach.studio.messaging

import com.koach.studio.awaitTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.FreshDemoData
import com.koach.studio.MainActivity
import com.koach.studio.signInAs
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagingUiTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun client_opensProjectThread_andSendsMessage() {
        rule.signInAs("u-maya", shellTag = "tab_home")
        rule.onNodeWithTag("tab_messages").performClick()
        // Messages opens on DMs; project threads are on the Team / Project tab.
        rule.onNode(hasTestTag("segment_1") and hasAnyAncestor(hasTestTag("messages_segmented_tabs"))).performClick()
        rule.awaitTag("thread_th-1")
        rule.onNodeWithTag("thread_th-1").performClick()

        rule.onNodeWithTag("message_input").performTextInput("Can we review the grade on Friday?")
        rule.onNodeWithTag("send_message").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("Can we review the grade on Friday?", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
