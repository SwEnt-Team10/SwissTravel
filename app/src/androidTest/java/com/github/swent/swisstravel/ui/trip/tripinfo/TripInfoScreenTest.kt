package com.github.swent.swisstravel.ui.trip.tripinfo

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.ui.trip.tripinfos.*
import com.google.firebase.Timestamp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripInfoScreenTest {

  @get:Rule val compose = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private fun setContent(
      vm: TripInfoViewModelContract,
      onMyTrips: () -> Unit = {},
      onEditTrip: () -> Unit = {}
  ) {
    compose.setContent {
      TripInfoScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          tripInfoContentCallbacks =
              TripInfoContentCallbacks(onMyTrips = onMyTrips, onEditTrip = onEditTrip))
    }
  }

  @Test
  fun titleIsDisplayedAfterLoad() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setName("My Great Trip")
        }

    setContent(vm)

    compose
        .onNodeWithTag(TripInfoScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("My Great Trip")
  }

  @Test
  fun noLocations_showsMessage_andNoMapCard() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(emptyList())
        }

    setContent(vm)

    compose.onNodeWithTag(TripInfoScreenTestTags.LAZY_COLUMN).assertIsDisplayed()
    compose.onNodeWithTag(TripInfoScreenTestTags.NO_LOCATIONS).assertIsDisplayed()
    // Map card is only shown when locations are present
    compose.onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertDoesNotExist()
  }

  @Test
  fun favoriteButtonToggleUpdatesViewModelState() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setFavorite(false)
        }

    setContent(vm)

    // click favorite button
    compose.onNodeWithTag(TripInfoScreenTestTags.FAVORITE_BUTTON).performClick()

    compose.runOnIdle { assert(vm.uiState.value.isFavorite) }
  }

  @Test
  fun backButtonCallsOnMyTripsCallback() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(emptyList())
        }
    var called = false

    setContent(vm, onMyTrips = { called = true })

    compose.onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).performClick()
    compose.runOnIdle { assert(called) }
  }

  @Test
  fun editButtonCallsOnEditTripCallback() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var editCalled = false

    setContent(vm, onEditTrip = { editCalled = true })

    compose.onNodeWithTag(TripInfoScreenTestTags.EDIT_BUTTON).performClick()
    compose.runOnIdle { assert(editCalled) }
  }

  @Test
  fun fullscreenToggle_showsAndHidesOverlay() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          // minimal data so card appears (locations non-empty)
          setLocations(
              listOf(
                  Location(Coordinate(46.5, 6.6), "Lausanne"),
                  Location(Coordinate(46.95, 7.44), "Bern")))
          // set a profile so schedule computation starts (we assert loading state)
          setTripProfile(
              TripProfile(
                  startDate = Timestamp.now(),
                  endDate = Timestamp.now(),
                  preferences = emptyList()))
        }

    setContent(vm)

    // map card should appear
    compose.onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).assertIsDisplayed()

    // enter fullscreen
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON).performClick()
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_MAP).assertIsDisplayed()

    // exit fullscreen
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_EXIT).performClick()
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_MAP).assertDoesNotExist()
  }

  @Test
  fun mapIsDisplayedAfterComputing() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setTripProfile(
              TripProfile(
                  startDate = Timestamp.now(),
                  endDate = Timestamp.now(),
                  preferences = emptyList()))
          setLocations(
              listOf(
                  Location(Coordinate(46.5, 6.6), "Lausanne"),
                  Location(Coordinate(46.95, 7.44), "Bern")))
          // No activities required; computation will start
        }

    setContent(vm)

    compose.onNodeWithTag(TripInfoScreenTestTags.MAP_CARD).performScrollTo()
  }

  @Test
  fun stepControlsDisabledWhenNoScheduleOrComputing() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setTripProfile(
              TripProfile(
                  startDate = Timestamp.now(),
                  endDate = Timestamp.now(),
                  preferences = emptyList()))
          setLocations(
              listOf(
                  Location(Coordinate(46.5, 6.6), "Lausanne"),
                  Location(Coordinate(46.95, 7.44), "Bern")))
        }

    setContent(vm)

    // Buttons should be disabled while computing (schedule empty)
    compose.onNodeWithText(getString(R.string.previous_step)).assertIsNotEnabled()
    compose.onNodeWithText(getString(R.string.next_step)).assertIsNotEnabled()
  }

  @Test
  fun topBarHidden_inFullscreen_andShownAfterExit() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setName("Trip")
          setLocations(
              listOf(
                  Location(Coordinate(46.5, 6.6), "Lausanne"),
                  Location(Coordinate(46.95, 7.44), "Bern")))
          setTripProfile(TripProfile(Timestamp.now(), Timestamp.now(), emptyList()))
        }
    setContent(vm)

    // Top bar present initially
    compose.onNodeWithTag(TripInfoScreenTestTags.TITLE).assertIsDisplayed()

    // Enter fullscreen
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON).performClick()
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_MAP).assertIsDisplayed()
    // Top bar should be gone in fullscreen
    compose.onNodeWithTag(TripInfoScreenTestTags.TITLE).assertDoesNotExist()

    // Exit fullscreen
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_EXIT).performClick()
    compose.onNodeWithTag(TripInfoScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun noLocations_hidesFullscreenAndMapContainer() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(emptyList())
        }
    setContent(vm)

    compose.onNodeWithTag(TripInfoScreenTestTags.MAP_CONTAINER).assertDoesNotExist()
    compose.onNodeWithTag(TripInfoScreenTestTags.FULLSCREEN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun stepButtons_disabledWhenScheduleEmpty() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          // present locations but no profile -> no schedule computed
          setLocations(listOf(Location(Coordinate(1.0, 1.0), "OnlyOne")))
        }
    setContent(vm)

    compose.onNodeWithText(getString(R.string.previous_step)).assertIsNotEnabled()
    compose.onNodeWithText(getString(R.string.next_step)).assertIsNotEnabled()
  }

  @Test
  fun backButton_callsOnMyTripsOnce() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(emptyList())
        }
    var calls = 0
    setContent(vm, onMyTrips = { calls++ })

    compose.onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).performClick()
    compose.waitForIdle()
    compose.runOnIdle { assert(calls == 1) }
  }

  // Helpers to access strings inside tests
  private fun getString(resId: Int) =
      androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
          .getString(resId)

  @Test
  fun backButtonNotShownOnCurrentTripScreen() {
    compose.setContent { TripInfoScreen(uid = "whatever", isOnCurrentTripScreen = true) }
    compose.onNodeWithTag(TripInfoScreenTestTags.BACK_BUTTON).isNotDisplayed()
  }

  @Test
  fun swipeActivitiesButtonNavigatesToSwipeScreen() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var swipeCalled = false

    compose.setContent {
      TripInfoScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          tripInfoContentCallbacks =
              TripInfoContentCallbacks(
                  onMyTrips = {}, onEditTrip = {}, onSwipeActivities = { swipeCalled = true }))
    }

    compose
        .onNodeWithTag(TripInfoScreenTestTags.SWIPE_ACTIVITIES_BUTTON)
        .assertIsDisplayed()
        .performClick()
    compose.runOnIdle { assert(swipeCalled) }
  }

  @Test
  fun likedActivitiesButtonNavigatesToLikedActivitiesScreen() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var likeCalled = false

    compose.setContent {
      TripInfoScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          tripInfoContentCallbacks =
              TripInfoContentCallbacks(
                  onMyTrips = {}, onEditTrip = {}, onLikedActivities = { likeCalled = true }))
    }

    compose
        .onNodeWithTag(TripInfoScreenTestTags.LIKED_ACTIVITIES_BUTTON)
        .assertIsDisplayed()
        .performClick()
    compose.runOnIdle { assert(likeCalled) }
  }
}
