package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.utils.FakeTripsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for the Trip Summary screen. */
class TripSummaryTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(
      viewModel: TripSettingsViewModel =
          TripSettingsViewModel(
              tripsRepository = FakeTripsRepository(),
              userRepository = FakeUserRepository(),
              activityRepository = FakeActivityRepository()),
      onNext: () -> Unit = {},
      onPrevious: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      TripSummaryScreen(viewModel = viewModel, onNext = onNext, onPrevious = onPrevious)
    }
  }

  @Test
  fun screenIsDisplayedCorrectly() {
    setContent()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.DESTINATIONS_EMPTY_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).assertIsDisplayed()
  }

  @Test
  fun previousButtonTriggersOnPrevious() {
    var previousCalled = false
    setContent(onPrevious = { previousCalled = true })
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_SUMMARY_TITLE).performClick()
    assertTrue("The onPrevious callback must be called", previousCalled)
  }

  @Test
  fun datesAndTravelerInfoAreDisplayed() {
    setContent()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.FROM_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TO_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.NUMBER_OF_TRAVELERS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.ADULTS_COUNT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CHILDREN_COUNT).assertIsDisplayed()
  }

  @Test
  fun createButtonDoesNotTriggerOnNextWhenRequiredFieldsMissing() {
    var nextCalled = false
    setContent(onNext = { nextCalled = true })
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()
    assertFalse("onNext must not be called when required fields are missing", nextCalled)
  }
  /** A fake implementation of ActivityRepository for UI testing. Avoids real API calls. */
  private class FakeActivityRepository : ActivityRepository {
    override suspend fun getMostPopularActivities(
        limit: Int,
        page: Int,
        activityBlackList: List<String>,
        cachedActivities: MutableList<Activity>
    ): List<Activity> = emptyList()

    override suspend fun getActivitiesNear(
        coordinate: Coordinate,
        radiusMeters: Int,
        limit: Int,
        activityBlackList: List<String>,
        cachedActivities: MutableList<Activity>
    ): List<Activity> = emptyList()

    override suspend fun getActivitiesByPreferences(
        preferences: List<Preference>,
        limit: Int,
        activityBlackList: List<String>,
        cachedActivities: MutableList<Activity>
    ): List<Activity> = emptyList()

    override suspend fun searchDestinations(query: String, limit: Int): List<Activity> = emptyList()

    override suspend fun getActivitiesNearWithPreference(
        preferences: List<Preference>,
        coordinate: Coordinate,
        radiusMeters: Int,
        limit: Int,
        activityBlackList: List<String>,
        cachedActivities: MutableList<Activity>
    ): List<Activity> = emptyList()
  }
}
