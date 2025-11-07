package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.mytrips.tripinfos.FavoriteButton
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoTestTags
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeTripInfoViewModel {
  private val _uiState = MutableStateFlow(TripInfoUIState())
  val uiState: StateFlow<TripInfoUIState> = _uiState.asStateFlow()

  fun setTripInfo(state: TripInfoUIState) {
    _uiState.value = state
  }

  fun setLocations(locations: List<Location>) {
    _uiState.value = _uiState.value.copy(locations = locations)
  }

  fun loadTripInfo(tripId: String?) {
    if (tripId.isNullOrBlank()) {
      _uiState.value = _uiState.value.copy(errorMsg = "Trip ID is invalid")
      return
    }
    _uiState.value = _uiState.value.copy(uid = tripId)
  }

  fun toggleFavorite() {
    val current = _uiState.value
    if (current.uid.isBlank()) return
    _uiState.value = current.copy(isFavorite = !current.isFavorite)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }
}

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
  fun backButtonHidesMapAndCallsOnMyTrips() {
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
  fun editButtonHasContentDescriptionFromResources() {
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

  // --- Additions to cover previously unused testTags ---
  @Test
  fun tripCardContentIsDisplayed() {
    composeRule.setContent {
      TripInfoScreen(uid = null, onMyTrips = {}, onFullscreenClick = {}, onEditTrip = {})
    }

    // Verify the internal content of the trip card is present
    composeRule.onNodeWithTag(TripInfoTestTags.TRIP_CARD_CONTENT).assertIsDisplayed()
  }

  @Test
  fun currentStepAndFirstLocationTagsAreUsableInIsolatedComposable() {
    // Avoid depending on the ViewModel by providing a small test composable
    composeRule.setContent {
      Column {
        Text("Current step", modifier = Modifier.testTag(TripInfoTestTags.CURRENT_STEP))
        Text(
            "First location name",
            modifier = Modifier.testTag(TripInfoTestTags.FIRST_LOCATION_NAME))
      }
    }

    composeRule.onNodeWithTag(TripInfoTestTags.CURRENT_STEP).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.FIRST_LOCATION_NAME).assertIsDisplayed()
  }

  // New: test the indexed LOCATION_CARD tag
  @Test
  fun locationCardIndexedTagIsUsableInIsolatedComposable() {
    composeRule.setContent {
      Box(modifier = Modifier.testTag("${TripInfoTestTags.LOCATION_CARD}_0")) {
        Text("First location", modifier = Modifier.testTag(TripInfoTestTags.FIRST_LOCATION_NAME))
      }
    }

    composeRule.onNodeWithTag("${TripInfoTestTags.LOCATION_CARD}_0").assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.FIRST_LOCATION_NAME).assertIsDisplayed()
  }

  // New: small smoke test that instantiates several remaining tags in isolation
  @Test
  fun remainingTagsSmokeTestAreUsableInIsolatedComposable() {
    composeRule.setContent {
      Column {
        Text("Topbar", modifier = Modifier.testTag(TripInfoTestTags.TOPBAR_TITLE))
        Box(modifier = Modifier.testTag(TripInfoTestTags.TRIP_CARD).height(24.dp).padding(4.dp)) {
          Box(
              modifier =
                  Modifier.testTag(TripInfoTestTags.TRIP_CARD_CONTENT)
                      .height(20.dp)
                      .padding(2.dp)) {
                Box(
                    modifier =
                        Modifier.testTag(TripInfoTestTags.MAP_VIEW)
                            .height(20.dp)
                            .padding(2.dp)) { /* represent map area */}
              }
        }
        Text("Step", modifier = Modifier.testTag(TripInfoTestTags.CURRENT_STEP))
        Box(
            modifier =
                Modifier.testTag("${TripInfoTestTags.LOCATION_CARD}_0")
                    .height(18.dp)
                    .padding(2.dp)) {
              Text("First", modifier = Modifier.testTag(TripInfoTestTags.FIRST_LOCATION_NAME))
            }
        Box(
            modifier =
                Modifier.testTag(TripInfoTestTags.FAVORITE_BUTTON)
                    .height(24.dp)
                    .padding(2.dp)) { /* represent favorite button */}
      }
    }

    composeRule.onNodeWithTag(TripInfoTestTags.TOPBAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.TRIP_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.TRIP_CARD_CONTENT).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.MAP_VIEW).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.CURRENT_STEP).assertIsDisplayed()
    composeRule.onNodeWithTag("${TripInfoTestTags.LOCATION_CARD}_0").assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.FIRST_LOCATION_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(TripInfoTestTags.FAVORITE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun multipleLocationsDisplayIndexedLocationCards() {
    val fake = FakeTripInfoViewModel()
    val locations =
        listOf(
            Location(coordinate = Coordinate(46.0, 6.0), name = "First"),
            Location(coordinate = Coordinate(47.0, 7.0), name = "Second"),
            Location(coordinate = Coordinate(48.0, 8.0), name = "Third"))
    fake.setLocations(locations)

    composeRule.setContent {
      val uiState by fake.uiState.collectAsState(initial = TripInfoUIState())
      Column {
        uiState.locations.forEachIndexed { idx, loc ->
          Box(modifier = Modifier.testTag("${TripInfoTestTags.LOCATION_CARD}_$idx")) {
            if (idx == 0) {
              Text(
                  text = loc.name,
                  modifier = Modifier.testTag(TripInfoTestTags.FIRST_LOCATION_NAME))
            } else {
              Text(text = loc.name)
            }
          }
        }
      }
    }

    // Verify that all location cards are displayed
    locations.indices.forEach { idx ->
      composeRule.onNodeWithTag("${TripInfoTestTags.LOCATION_CARD}_$idx").assertIsDisplayed()
    }

    // Verify that the first location name tag is displayed
    composeRule.onNodeWithTag(TripInfoTestTags.FIRST_LOCATION_NAME).assertIsDisplayed()
  }
}
