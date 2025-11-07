package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.mytrips.tripinfos.FavoriteButton
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Instrumented tests for the TripInfoScreen composable, consolidated into a single file. */
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

    val activity = composeRule.activity
    val backDesc = activity.getString(R.string.back_to_my_trips)
    val editDesc = activity.getString(R.string.edit_trip)

    // Verify presence of elements by tag
    composeRule.onNodeWithTag(TripInfoTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.TRIP_CARD).assertIsDisplayed()

    // Verify contentDescriptions from resources
    composeRule.onNodeWithContentDescription(backDesc).assertIsDisplayed()
    composeRule.onNodeWithContentDescription(editDesc).assertIsDisplayed()

    // Test callbacks for the buttons
    composeRule.onNodeWithTag(TripInfoTestTags.BACK_BUTTON).performClick()
    composeRule.runOnIdle { assertTrue("onMyTrips should have been called", pastClicked) }

    composeRule.onNodeWithTag(TripInfoTestTags.EDIT_BUTTON).performClick()
    composeRule.runOnIdle { assertTrue("onEditTrip should have been called", editClicked) }
  }

  @Test
  fun favoriteButtonDisplayedAndTogglesWithContentDescriptions() {
    var favoriteClicked = false

    // Standalone FavoriteButton test remains here but inside the consolidated file
    composeRule.setContent {
      var isFavorite by remember { mutableStateOf(false) }
      FavoriteButton(
          isFavorite = isFavorite,
          onToggleFavorite = {
            favoriteClicked = true
            isFavorite = !isFavorite
          })
    }

    val activity = composeRule.activity
    val emptyDesc = activity.getString(R.string.favorite_icon_empty)
    val filledDesc = activity.getString(R.string.unfavorite_icon)

    // Initial state
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithContentDescription(emptyDesc).assertIsDisplayed()

    // Click to toggle on
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).performClick()
    composeRule.runOnIdle {
      assertTrue("onToggleFavorite should have been called", favoriteClicked)
    }

    // Check icon changed
    composeRule.onNodeWithContentDescription(filledDesc).assertIsDisplayed()

    // Click again to toggle off
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).performClick()

    // Wait for UI to settle, then assert again
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription(emptyDesc).assertIsDisplayed()
  }

  @Test
  fun favoriteButtonPresentInsideTripInfoScreen() {
    composeRule.setContent {
      TripInfoScreen(uid = null, onMyTrips = {}, onFullscreenClick = {}, onEditTrip = {})
    }

    // Ensure favorite button belongs to TripInfoScreen
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun backButton_hidesMapAndCallsOnMyTrips() {
    var myTripsCalled = false

    composeRule.setContent {
      TripInfoScreen(
          uid = null, onMyTrips = { myTripsCalled = true }, onFullscreenClick = {}, onEditTrip = {})
    }

    // Click back -> should call onMyTrips and hide the map view
    composeRule.onNodeWithTag(TripInfoTestTags.BACK_BUTTON).performClick()
    composeRule.runOnIdle { assertTrue("onMyTrips should have been called", myTripsCalled) }

    // MAP_VIEW should no longer exist when showMap is false
    composeRule.onNodeWithTag(TripInfoTestTags.MAP_VIEW).assertDoesNotExist()
  }

  @Test
  fun noLocationCardDisplayedWhenNoLocations() {
    composeRule.setContent {
      TripInfoScreen(uid = null, onMyTrips = {}, onFullscreenClick = {}, onEditTrip = {})
    }

    // When there are no locations, there should be no LOCATION_CARD nodes and "no locations" text
    // is shown
    composeRule.onNodeWithTag(TripInfoTestTags.LOCATION_CARD).assertDoesNotExist()
    composeRule.onNodeWithTag(TripInfoTestTags.NO_LOCATIONS_TEXT).assertIsDisplayed()
  }

  @Test
  fun editButton_hasContentDescriptionFromResources() {
    composeRule.setContent {
      TripInfoScreen(uid = null, onMyTrips = {}, onFullscreenClick = {}, onEditTrip = {})
    }

    val activity = composeRule.activity
    val editDesc = activity.getString(R.string.edit_trip)

    // Edit button uses the contentDescription from resources
    composeRule.onNodeWithContentDescription(editDesc).assertIsDisplayed()
  }

  @Test
  fun topBarTitleNoLocationsAndMapDisplayed() {
    composeRule.setContent {
      TripInfoScreen(uid = null, onMyTrips = {}, onFullscreenClick = {}, onEditTrip = {})
    }

    // Verify that the top bar title and the "no locations" text appear
    composeRule.onNodeWithTag(TripInfoTestTags.TOPBAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.NO_LOCATIONS_TEXT).assertIsDisplayed()

    // Verify presence of the map view inside the card
    composeRule.onNodeWithTag(TripInfoTestTags.MAP_VIEW).assertIsDisplayed()
  }
}
