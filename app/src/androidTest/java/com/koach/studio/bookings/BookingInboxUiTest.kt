package com.koach.studio.bookings

import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
class BookingInboxUiTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(rule)

    @Test
    fun admin_declinesBookingWithReason_fromDetail() {
        rule.signInAs("u-arjun", shellTag = "tab_agency_home")
        rule.onNodeWithTag("tab_account").performClick()
        rule.onNodeWithTag("menu_screen").performScrollToNode(hasTestTag("menu_bookings"))
        rule.onNodeWithTag("menu_bookings").performClick()
        rule.awaitTag("booking_bk-1")
        rule.onNodeWithTag("booking_bk-1").performClick()

        rule.awaitTag("detail_decline")
        rule.onNodeWithTag("detail_decline").performScrollTo().performClick()
        rule.onNodeWithTag("decline_reason").performTextInput("Fully booked that week")
        rule.onNodeWithTag("confirm_decline").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodes(hasTestTag("booking_status") and hasAnyDescendant(hasText("Declined", ignoreCase = true)))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
