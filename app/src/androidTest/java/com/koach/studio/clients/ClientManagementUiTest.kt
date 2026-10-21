package com.koach.studio.clients

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.FreshDemoData
import com.koach.studio.MainActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientManagementUiTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun agency_addsClient_withValidation_thenSeesDetail() {
        rule.onNodeWithTag("demo_account_u-zoya").performScrollTo().performClick()
        rule.onNodeWithTag("tab_clients").performClick()
        rule.onNodeWithTag("add_client").performClick()

        // Saving an empty form shows validation instead of saving.
        rule.onNodeWithTag("save_client").performScrollTo().performClick()
        rule.onNodeWithText("Enter the client's name").assertIsDisplayed()

        rule.onNodeWithTag("client_name").performScrollTo().performTextInput("Monsoon Films")
        rule.onNodeWithTag("client_industry").performScrollTo().performTextInput("Film production")
        rule.onNodeWithTag("client_city").performScrollTo().performTextInput("Mumbai")
        rule.onNodeWithTag("client_contact").performScrollTo().performTextInput("Ria Shah")
        rule.onNodeWithTag("client_email").performScrollTo().performTextInput("ria@monsoon.example")
        rule.onNodeWithTag("client_phone").performScrollTo().performTextInput("+91 98200 33333")
        rule.onNodeWithTag("save_client").performScrollTo().performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithTagExists("edit_client") }
        rule.onAllNodesWithText("Monsoon Films").onFirst().assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
