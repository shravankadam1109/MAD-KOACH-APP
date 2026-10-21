package com.koach.studio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
class RoleNavigationTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun client_seesClientTabs_andCanSignOut() {
        rule.signInAs("u-maya", shellTag = "tab_home")
        rule.onNodeWithTag("tab_home").assertIsDisplayed()
        rule.onNodeWithTag("tab_explore").assertIsDisplayed()
        rule.onNodeWithTag("tab_clients").assertDoesNotExist()

        rule.onNodeWithTag("tab_account").performClick()
        rule.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        rule.onNodeWithTag("sign_in_screen").assertIsDisplayed()
    }

    @Test
    fun agency_seesStudioTabs() {
        rule.signInAs("u-zoya", shellTag = "tab_agency_home")
        rule.onNodeWithTag("tab_agency_home").assertIsDisplayed()
        rule.onNodeWithTag("tab_clients").assertIsDisplayed()
        rule.onNodeWithTag("tab_tasks").assertIsDisplayed()
        rule.onNodeWithTag("tab_explore").assertDoesNotExist()
    }

    @Test
    fun client_paysInvoiceWithSimulatedPayment() {
        rule.signInAs("u-maya", shellTag = "tab_home")
        rule.onNodeWithTag("tab_account").performClick()
        rule.onNodeWithTag("menu_payments").performScrollTo().performClick()
        rule.onNodeWithTag("payments_list").performScrollToNode(hasTestTag("pay_pay-2"))
        rule.onNodeWithTag("pay_pay-2").performClick()
        rule.onNodeWithTag("confirm_simulated_payment").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("pay_pay-2")).fetchSemanticsNodes().isEmpty() }
    }
}
