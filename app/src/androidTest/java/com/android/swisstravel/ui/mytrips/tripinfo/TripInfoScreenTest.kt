package com.android.swisstravel.ui.mytrips.tripinfo

import com.github.swent.swisstravel.ui.mytrips.tripinfos.TestTags

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for the TripInfoScreen composable.
 */
class TripInfoScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun uiElementsDisplayedAndCallbacksTriggered() {
        var pastClicked = false
        var editClicked = false

        composeRule.setContent {
            TripInfoScreen(
                uid = null,
                onPastTrips = { pastClicked = true },
                onFullscreenClick = {},
                onEditTrip = { editClicked = true }
            )
        }

        // Verify that UI elements are displayed
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.EDIT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.TRIP_CARD).assertIsDisplayed()

        // Click buttons and verify callbacks
        composeRule.onNodeWithTag(TestTags.BACK_BUTTON).performClick()
        composeRule.runOnIdle { assertTrue("onPastTrips should have been called", pastClicked) }

        composeRule.onNodeWithTag(TestTags.EDIT_BUTTON).performClick()
        composeRule.runOnIdle { assertTrue("onEditTrip should have been called", editClicked) }
    }
}

