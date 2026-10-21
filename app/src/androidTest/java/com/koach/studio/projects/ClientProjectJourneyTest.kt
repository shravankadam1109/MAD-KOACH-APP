package com.koach.studio.projects

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.MainActivity
import com.koach.studio.projects.data.ProjectRepositoryProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientProjectJourneyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ProjectRepositoryProvider.reset()
    }

    @Test
    fun clientProjectJourney_openList_openProject_downloadDeliverable_requestRevision() {
        // Step 1: Open project list - verify project list is displayed
        composeTestRule.onNodeWithTag("project_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Client Projects").assertIsDisplayed()

        // Step 2: Open a project (proj-1: Cyberpunk Indie Music Video)
        composeTestRule.onNodeWithTag("project_item_proj-1").assertIsDisplayed().performClick()

        // Verify project details screen is displayed
        composeTestRule.onNodeWithTag("project_detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cyberpunk Indie Music Video").assertIsDisplayed()

        // Step 3: Download a deliverable (del-101)
        composeTestRule.onNodeWithTag("download_button_del-101").assertIsDisplayed().performClick()

        // Wait until download state progresses to DONE
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("download_done_del-101", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("download_done_del-101", useUnmergedTree = true).assertIsDisplayed()

        // Step 4: Request a revision
        composeTestRule.onNodeWithTag("request_revision_button").assertIsDisplayed().performClick()

        // Verify revision request screen is displayed
        composeTestRule.onNodeWithText("Request Revision").assertIsDisplayed()
        composeTestRule.onNodeWithTag("revision_reason_field").assertIsDisplayed()

        // Enter revision reason
        composeTestRule.onNodeWithTag("revision_reason_field")
            .performTextInput("Please re-grade the street night scenes and boost bass levels.")

        // Submit revision request
        composeTestRule.onNodeWithTag("submit_revision_button").assertIsDisplayed().performClick()

        // Verify navigated back to Project Details screen and project status is updated
        composeTestRule.onNodeWithTag("project_detail_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Revision Requested").assertIsDisplayed()
    }
}
