package com.koach.studio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.koach.studio.KoachApplication
import com.koach.studio.MainActivity
import com.koach.studio.core.di.DemoAppContainer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoleNavigationTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        ApplicationProvider.getApplicationContext<KoachApplication>().container = DemoAppContainer()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = scenario.close()

    @Test
    fun client_seesClientTabs_andCanSignOut() {
        rule.onNodeWithTag("demo_account_u-maya").performClick()
        rule.onNodeWithTag("tab_home").assertIsDisplayed()
        rule.onNodeWithTag("tab_explore").assertIsDisplayed()
        rule.onNodeWithTag("tab_clients").assertDoesNotExist()

        rule.onNodeWithTag("tab_account").performClick()
        rule.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        rule.onNodeWithTag("sign_in_screen").assertIsDisplayed()
    }

    @Test
    fun agency_seesStudioTabs() {
        rule.onNodeWithTag("demo_account_u-zoya").performScrollTo().performClick()
        rule.onNodeWithTag("tab_agency_home").assertIsDisplayed()
        rule.onNodeWithTag("tab_clients").assertIsDisplayed()
        rule.onNodeWithTag("tab_tasks").assertIsDisplayed()
        rule.onNodeWithTag("tab_explore").assertDoesNotExist()
    }
}
