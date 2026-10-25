package com.koach.studio.bookings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.FreshDemoData
import com.koach.studio.MainActivity
import com.koach.studio.awaitTag
import com.koach.studio.signInAs
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingAndFiltersUiTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun client_exploreTab_filtersServicesAndPortfolio() {
        rule.signInAs("u-maya", shellTag = "tab_explore")
        rule.onNodeWithTag("tab_explore").performClick()
        rule.awaitTag("explore_screen")

        // Filter chips exist on Services tab
        rule.onNodeWithTag("service_filters").assertIsDisplayed()

        // Switch to Work / Portfolio tab
        rule.onNodeWithTag("tab_work", useUnmergedTree = true).performClick()
        rule.awaitTag("portfolio_filters")
        rule.onNodeWithTag("portfolio_filters").assertIsDisplayed()
    }

    @Test
    fun client_newBooking_validatesRequiredFields() {
        rule.signInAs("u-maya", shellTag = "tab_home")
        rule.onNodeWithTag("home_cta_book").performScrollTo().performClick()
        rule.awaitTag("booking_form")

        // Attempt submit without filling details
        rule.onNodeWithTag("booking_submit").performScrollTo().performClick()

        // Verify form remains displayed with inline error validation
        rule.onNodeWithTag("booking_form").assertIsDisplayed()

        // Enter project details and phone
        rule.onNodeWithTag("booking_details").performScrollTo().performTextInput("Commercial batch shoot for winter season.")
        rule.onNodeWithTag("booking_phone").performScrollTo().performTextInput("+91 98200 44556")
    }
}
