package com.github.swent.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.ui.mytrips.FakeTripsRepository
import com.github.swent.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryScreen
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for the Trip Summary screen. */
class TripSummaryTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(
      viewModel: TripSettingsViewModel =
          TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository()),
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

  @Test
  fun enteringTripNameAloneDoesNotTriggerOnNext() {
    var nextCalled = false
    setContent(onNext = { nextCalled = true })
    composeTestRule.onNodeWithTag(TripSummaryTestTags.TRIP_NAME_FIELD).performTextInput("My trip")
    composeTestRule.onNodeWithTag(TripSummaryTestTags.CREATE_TRIP_BUTTON).performClick()
    assertFalse(
        "onNext must not be called when departure/arrival locations are missing", nextCalled)
  }
}
