package com.koach.studio.projects

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.FreshDemoData
import com.koach.studio.MainActivity
import com.koach.studio.awaitTag
import com.koach.studio.signInAs
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientProjectJourneyTest {

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(FreshDemoData()).around(composeTestRule)

    @Test
    fun clientProjectJourney_openList_openProject_downloadDeliverable_requestRevision() {
        // Step 0: Sign in with the demo client account that owns proj-1
        composeTestRule.signInAs("u-maya", shellTag = "tab_home")
        composeTestRule.onNodeWithTag("tab_projects").performClick()

        // Step 1: Open project list - verify project list is displayed
        composeTestRule.onNodeWithTag("project_list").assertIsDisplayed()

        // Step 2: Open a project (proj-1: Cyberpunk Indie Music Video)
        composeTestRule.onNodeWithTag("project_item_proj-1").assertIsDisplayed().performClick()

        // Verify project details screen is displayed
        composeTestRule.onNodeWithTag("project_detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cyberpunk Indie Music Video").assertIsDisplayed()

        // Step 3: Download a deliverable (del-101)
        composeTestRule.onNodeWithTag("project_detail_content").performScrollToNode(hasTestTag("download_button_del-101"))
        composeTestRule.onNodeWithTag("download_button_del-101").performClick()

        // Wait until download state progresses to DONE
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("download_done_del-101", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("download_done_del-101", useUnmergedTree = true).assertExists()

        // Step 4: Request a revision
        composeTestRule.onNodeWithTag("project_detail_content").performScrollToNode(hasTestTag("request_revision_button"))
        composeTestRule.onNodeWithTag("request_revision_button").performClick()

        // Verify revision request screen is displayed
        composeTestRule.onNodeWithText("Request Revision").assertIsDisplayed()
        composeTestRule.onNodeWithTag("revision_reason_field").assertIsDisplayed()

        // Enter revision reason
        composeTestRule.onNodeWithTag("revision_reason_field")
            .performTextInput("Please re-grade the street night scenes and boost bass levels.")

        // Submit revision request
        composeTestRule.onNodeWithTag("submit_revision_button").assertIsDisplayed().performClick()

        // Verify navigated back to Project Details screen and project status is updated.
        // The screen returns after the "Undo" snackbar closes (a few seconds).
        composeTestRule.awaitTag("project_detail_content", timeoutMillis = 15_000)
        composeTestRule.onNodeWithTag("project_detail_content").performScrollToNode(hasTestTag("project_status"))
        composeTestRule.onNodeWithTag("project_detail_title").assertIsDisplayed()
        composeTestRule.onNode(
            hasTestTag("project_status") and hasAnyDescendant(hasText("Revision Requested", ignoreCase = true)),
            useUnmergedTree = true
        ).assertExists()
    }
}
