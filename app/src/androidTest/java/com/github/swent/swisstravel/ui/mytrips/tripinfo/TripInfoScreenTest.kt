package com.github.swent.swisstravel.ui.mytrips.tripinfo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreen
import com.github.swent.swisstravel.ui.mytrips.tripinfos.TripInfoScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for the TripInfoScreen composable. */
@RunWith(AndroidJUnit4::class)
class TripInfoScreenTest {

  @get:Rule val composeTestRule = createComposeRule() // inferred type: provides setContent

  @Test
  fun titleIsDisplayedAfterLoad() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    fake.loadTripInfo("1")

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "1",
          tripInfoViewModel = vm,
          onMyTrips = {},
          onFullscreenClick = {},
          onEditTrip = {})
    }

    composeTestRule
        .onNodeWithTag(TripInfoScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Test Trip 1")
  }

  @Test
  fun noLocationsShowsNoLocationsAndMapBoxDisplayed() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    fake.loadTripInfo("x")
    fake.setLocations(emptyList())

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "x",
          tripInfoViewModel = vm,
          onMyTrips = {},
          onFullscreenClick = {},
          onEditTrip = {})
    }

    // lazy column always present
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.LAZY_COLUMN).assertIsDisplayed()

    // when no locations: show no locations message
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.NO_LOCATIONS).assertIsDisplayed()

    // map card / container / box should still be present
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_BOX).assertIsDisplayed()
  }

  @Test
  fun favoriteButtonToggleUpdatesViewModelState() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    fake.loadTripInfo("fav")

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "fav",
          tripInfoViewModel = vm,
          onMyTrips = {},
          onFullscreenClick = {},
          onEditTrip = {})
    }

    // safely verify initial state
    composeTestRule.runOnIdle { assert(!fake.uiState.value.isFavorite) }

    // click favorite button
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.FAVORITE_BUTTON).performClick()

    // wait for idle then verify state
    composeTestRule.runOnIdle { assert(fake.uiState.value.isFavorite) }
  }

  @Test
  fun backButtonCallsOnMyTripsCallback() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    fake.loadTripInfo("back")
    var called = false

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "back",
          tripInfoViewModel = vm,
          onMyTrips = { called = true },
          onFullscreenClick = {},
          onEditTrip = {})
    }

    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).performClick()

    // LaunchedEffect triggers navigation; ensure callback invoked
    composeTestRule.runOnIdle { assert(called) }
  }

  @Test
  fun editButtonCallsOnEditTripCallback() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    fake.loadTripInfo("edit")
    var editCalled = false

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "edit",
          tripInfoViewModel = vm,
          onMyTrips = {},
          onFullscreenClick = {},
          onEditTrip = { editCalled = true })
    }

    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.EDIT_BUTTON).performClick()
    composeTestRule.runOnIdle { assert(editCalled) }
  }

  @Test
  fun allTagsPresentWhenTripHasLocations() {
    val fake = FakeTripInfoViewModel()
    val vm = TestTripInfoViewModel(fake)
    // use an id that the fake provides with at least two locations
    fake.loadTripInfoWithLocation("1")

    composeTestRule.setContent {
      TripInfoScreen(
          uid = "1",
          tripInfoViewModel = vm,
          onMyTrips = {},
          onFullscreenClick = {},
          onEditTrip = {})
    }

    // wait for composition/coroutines to settle
    composeTestRule.runOnIdle {}

    // container structures
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.LAZY_COLUMN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.MAP_BOX).assertIsDisplayed()

    // current step and first location box/name
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.CURRENT_STEP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.FIRST_LOCATION_BOX).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.LOCATION_NAME).assertIsDisplayed()

    // the secondary step card may be off-screen; assert it exists in the semantics tree
    composeTestRule.onNodeWithTag(TripInfoScreenTestTags.stepLocationTag(2)).assertExists()
  }
}
