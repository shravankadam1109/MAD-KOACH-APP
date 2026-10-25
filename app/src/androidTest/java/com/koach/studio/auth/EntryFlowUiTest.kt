package com.koach.studio.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.koach.studio.core.auth.DemoAuthRepository
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import com.koach.studio.FreshDemoData
import com.koach.studio.MainActivity
import com.koach.studio.awaitTag
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryFlowUiTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun welcome_opensDistinctPortals_andBackReturnsToWelcome() {
        rule.awaitTag("welcome_screen")
        rule.onNodeWithTag("enter_client").performClick()
        rule.awaitTag("client_login_screen")
        rule.onNodeWithText("Client portal", substring = true, ignoreCase = true).assertIsDisplayed()
        rule.onNodeWithTag("demo_account_u-arjun").assertDoesNotExist()

        pressBack()
        rule.awaitTag("welcome_screen")
        rule.onNodeWithTag("enter_team").performClick()
        rule.awaitTag("team_login_screen")
        rule.onNodeWithTag("demo_account_u-maya").assertDoesNotExist()
    }

    @Test
    fun clientLogin_validatesEmptyForm() {
        rule.awaitTag("welcome_screen")
        rule.onNodeWithTag("enter_client").performClick()
        rule.awaitTag("login_submit")
        rule.onNodeWithTag("login_submit").performClick()
        rule.onNodeWithText("Enter your email").assertIsDisplayed()
        rule.onNodeWithText("Enter your password").assertIsDisplayed()
    }

    @Test
    fun teamLogin_withEmailAndDemoPassword_opensStudio() {
        rule.awaitTag("welcome_screen")
        rule.onNodeWithTag("enter_team").performClick()
        rule.awaitTag("login_email")
        rule.onNodeWithTag("login_email").performTextInput("isha@koach.example")
        rule.onNodeWithTag("login_password").performTextInput(DemoAuthRepository.DEMO_PASSWORD)
        rule.onNodeWithTag("login_submit").performClick()
        rule.awaitTag("tab_agency_home")
    }
}
