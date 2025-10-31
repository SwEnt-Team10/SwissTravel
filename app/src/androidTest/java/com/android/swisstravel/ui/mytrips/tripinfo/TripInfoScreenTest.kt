package com.android.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the TripInfoScreen composable. */
class TripInfoScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun uiElementsDisplayedAndCallbacksTriggered() {
    var pastClicked = false
    var editClicked = false

    composeRule.setContent {
      TripInfoScreen(
          uid = null,
          onMyTrips = { pastClicked = true },
          onFullscreenClick = {},
          onEditTrip = { editClicked = true })
    }

    // Verify that UI elements are displayed
    composeRule.onNodeWithTag(TripInfoTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.TRIP_CARD).assertIsDisplayed()

    // Click buttons and verify callbacks
    composeRule.onNodeWithTag(TripInfoTestTags.BACK_BUTTON).performClick()
    composeRule.runOnIdle { assertTrue("onPastTrips should have been called", pastClicked) }

    composeRule.onNodeWithTag(TripInfoTestTags.EDIT_BUTTON).performClick()
    composeRule.runOnIdle { assertTrue("onEditTrip should have been called", editClicked) }
  }
}
