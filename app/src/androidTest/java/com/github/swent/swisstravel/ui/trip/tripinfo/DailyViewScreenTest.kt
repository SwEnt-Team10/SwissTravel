package com.github.swent.swisstravel.ui.trip.tripinfo

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenCallbacks
import com.github.swent.swisstravel.ui.trip.tripinfos.DailyViewScreenTestTags
import com.github.swent.swisstravel.ui.trip.tripinfos.TripInfoViewModelContract
import com.google.firebase.Timestamp
import com.mapbox.geojson.Point
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyViewScreenTest {

  @get:Rule val compose = createComposeRule()
  @get:Rule
  val grantPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private fun setContent(
      vm: TripInfoViewModelContract,
      onMyTrips: () -> Unit = {},
      onEditTrip: () -> Unit = {},
      onActivityClick: () -> Unit = {},
      mapContent: @Composable (List<Location>, Boolean, (Point) -> Unit) -> Unit = { _, _, _ ->
        Box(modifier = Modifier.testTag("fakeMap"))
      }
  ) {
    compose.setContent {
      DailyViewScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          callbacks =
              DailyViewScreenCallbacks(
                  onMyTrips = onMyTrips,
                  onEditTrip = onEditTrip,
                  onActivityClick = { onActivityClick() }),
          mapContent = mapContent)
    }
  }

  @Test
  fun titleIsDisplayed() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setName("My Daily Trip")
        }
    setContent(vm)
    compose
        .onNodeWithTag(DailyViewScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextContains("My Daily Trip")
  }

  @Test
  fun noLocations_showsMessage() {
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(emptyList())
        }
    setContent(vm)
    compose.onNodeWithTag(DailyViewScreenTestTags.NO_LOCATIONS).assertIsDisplayed()
    compose.onNodeWithTag(DailyViewScreenTestTags.MAP_CARD).assertDoesNotExist()
  }

  @Test
  fun dayNavigator_showsCurrentDay() {
    val now = Timestamp.now()
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(listOf(Location(Coordinate(0.0, 0.0), "Loc")))
          setTripProfile(TripProfile(startDate = now, endDate = now, preferences = emptyList()))
          // Add a dummy segment to ensure schedule is not empty
          setRouteSegments(
              listOf(
                  RouteSegment(
                      from = Location(Coordinate(0.0, 0.0), "A"),
                      to = Location(Coordinate(1.0, 1.0), "B"),
                      durationMinutes = 10,
                      transportMode = com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
                      startDate = now,
                      endDate = now)))
        }
    setContent(vm)

    // Wait for computation
    compose.waitForIdle()

    compose.onNodeWithTag(DailyViewScreenTestTags.DAY_NAVIGATOR).assertIsDisplayed()
    compose.onNodeWithTag(DailyViewScreenTestTags.CURRENT_DAY_TEXT).assertIsDisplayed()
  }

  @Test
  fun activityClick_callsCallback() {
    val now = Timestamp.now()
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(listOf(Location(Coordinate(0.0, 0.0), "Loc")))
          setTripProfile(TripProfile(startDate = now, endDate = now, preferences = emptyList()))
          setActivities(
              listOf(
                  Activity(
                      startDate = now,
                      endDate = now,
                      location = Location(Coordinate(0.0, 0.0), "Activity"),
                      description = "Desc",
                      imageUrls = listOf("http://img"),
                      estimatedTime = 60)))
        }
    var clicked = false
    setContent(vm, onActivityClick = { clicked = true })

    compose.waitForIdle()

    // Find the step card and click it
    compose.onNodeWithTag(DailyViewScreenTestTags.STEP_CARD).performClick()
    assert(clicked)
  }

  @Test
  fun fullscreenToggle_works() {
    val now = Timestamp.now()
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(listOf(Location(Coordinate(0.0, 0.0), "Loc")))
          setTripProfile(TripProfile(startDate = now, endDate = now, preferences = emptyList()))
          setRouteSegments(
              listOf(
                  RouteSegment(
                      from = Location(Coordinate(0.0, 0.0), "A"),
                      to = Location(Coordinate(1.0, 1.0), "B"),
                      durationMinutes = 10,
                      transportMode = com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
                      startDate = now,
                      endDate = now)))
        }
    setContent(vm)
    compose.waitForIdle()

    compose.onNodeWithTag(DailyViewScreenTestTags.FULLSCREEN_BUTTON).performClick()
    compose.onNodeWithTag(DailyViewScreenTestTags.FULLSCREEN_MAP).assertIsDisplayed()

    compose.onNodeWithTag(DailyViewScreenTestTags.FULLSCREEN_EXIT).performClick()
    compose.onNodeWithTag(DailyViewScreenTestTags.FULLSCREEN_MAP).assertDoesNotExist()
  }

  @Test
  fun backButton_callsCallback() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var backCalled = false
    setContent(vm, onMyTrips = { backCalled = true })

    compose.onNodeWithTag(DailyViewScreenTestTags.BACK_BUTTON).performClick()
    assert(backCalled)
  }

  @Test
  fun dayNavigator_updatesViewModel() {
    val now = Timestamp.now()
    val tomorrow = Timestamp(now.seconds + 86400, 0)
    val vm =
        FakeTripInfoViewModel().apply {
          loadTripInfo("TEST")
          setLocations(listOf(Location(Coordinate(0.0, 0.0), "Loc")))
          setTripProfile(
              TripProfile(startDate = now, endDate = tomorrow, preferences = emptyList()))
          setRouteSegments(
              listOf(
                  RouteSegment(
                      from = Location(Coordinate(0.0, 0.0), "A"),
                      to = Location(Coordinate(1.0, 1.0), "B"),
                      durationMinutes = 10,
                      transportMode = com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
                      startDate = now,
                      endDate = now),
                  RouteSegment(
                      from = Location(Coordinate(0.0, 0.0), "A"),
                      to = Location(Coordinate(1.0, 1.0), "B"),
                      durationMinutes = 10,
                      transportMode = com.github.swent.swisstravel.model.trip.TransportMode.WALKING,
                      startDate = tomorrow,
                      endDate = tomorrow)))
        }
    setContent(vm)
    compose.waitForIdle()

    compose.onNodeWithTag(DailyViewScreenTestTags.NEXT_DAY_BUTTON).performClick()
    assert(vm.uiState.value.currentDayIndex == 1)

    compose.onNodeWithTag(DailyViewScreenTestTags.PREV_DAY_BUTTON).performClick()
    assert(vm.uiState.value.currentDayIndex == 0)
  }

  @Test
  fun swipeActivitiesButtonWorks() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var swipeCalled = false

    compose.setContent {
      DailyViewScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          callbacks =
              DailyViewScreenCallbacks(
                  onMyTrips = {}, onEditTrip = {}, onSwipeActivities = { swipeCalled = true }))
    }

    compose
        .onNodeWithTag(DailyViewScreenTestTags.SWIPE_ACTIVITIES_BUTTON)
        .assertIsDisplayed()
        .performClick()
    compose.runOnIdle { assert(swipeCalled) }
  }

  @Test
  fun likedActivitiesButtonWorks() {
    val vm = FakeTripInfoViewModel().apply { loadTripInfo("TEST") }
    var likeCalled = false

    compose.setContent {
      DailyViewScreen(
          uid = "TEST",
          tripInfoViewModel = vm,
          callbacks =
              DailyViewScreenCallbacks(
                  onMyTrips = {}, onEditTrip = {}, onLikedActivities = { likeCalled = true }))
    }

    compose
        .onNodeWithTag(DailyViewScreenTestTags.LIKED_ACTIVITIES_BUTTON)
        .assertIsDisplayed()
        .performClick()
    compose.runOnIdle { assert(likeCalled) }
  }
}
