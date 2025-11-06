package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.mytrips.tripinfos.FavoriteButton
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoTestTags
import kotlin.test.assertTrue
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

  @Test
  fun favoriteButtonDisplayedAndToggles() {
    var favoriteClicked = false
    var isFavorite = false

    composeRule.setContent {
      FavoriteButton(
          isFavorite = isFavorite,
          onToggleFavorite = {
            favoriteClicked = true
            isFavorite = !isFavorite
          })
    }

    // Verify favorite button is displayed
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).assertIsDisplayed()

    // Click favorite button
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).performClick()

    composeRule.runOnIdle {
      // Callback triggered
      assertTrue("onToggleFavorite should have been called", favoriteClicked)
      // State updated
      assertTrue(isFavorite, "isFavorite should now be true after click")
    }

    // Click again to toggle back
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).performClick()
    composeRule.runOnIdle {
      assertTrue(isFavorite.not(), "isFavorite should now be false after second click")
    }
  }
}
