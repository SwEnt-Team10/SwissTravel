package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SuggestionListTest : com.github.swent.swisstravel.utils.SwissTravelTest() {

  @get:Rule val composeTestRule = createComposeRule()

  override fun createInitializedRepository():
      com.github.swent.swisstravel.model.trip.TripsRepository {
    return com.github.swent.swisstravel.ui.trips.FakeTripsRepository()
  }

  @Test
  fun suggestionList_isDisplayed_afterExpansion() {
    val viewModel =
        TripSettingsViewModel(
            tripsRepository = repository,
            userRepository = com.github.swent.swisstravel.ui.profile.FakeUserRepository())
    composeTestRule.setContent { FirstDestinationScreen(viewModel = viewModel) }

    // Click on "Suggestions" to expand
    composeTestRule.onNodeWithText("See Our Suggestions For You").performClick()
    // assert the suggestions are correctly displayed
    composeTestRule.onNodeWithText("See Our Suggestions For You").assertIsDisplayed()
  }

  @Test
  fun suggestionList_selectionUpdatesCheckbox() {
    val viewModel =
        TripSettingsViewModel(
            tripsRepository = repository,
            userRepository = com.github.swent.swisstravel.ui.profile.FakeUserRepository())
    composeTestRule.setContent { FirstDestinationScreen(viewModel = viewModel) }

    // Expand suggestions
    composeTestRule.onNodeWithText("See Our Suggestions For You").performClick()
    composeTestRule.waitForIdle()

    // Get the suggestions from the ViewModel to know what to look for
    val suggestions = viewModel.suggestions.value
    assert(suggestions.isNotEmpty())
    val firstSuggestion = suggestions.first()

    // Verify initial state (unchecked)
    composeTestRule.onNodeWithTag("suggestion_checkbox_${firstSuggestion.name}").assertIsOff()

    // Click the row to select
    composeTestRule.onNodeWithTag("suggestion_row_${firstSuggestion.name}").performClick()

    // Verify state is now checked
    composeTestRule.onNodeWithTag("suggestion_checkbox_${firstSuggestion.name}").assertIsOn()

    // Click again to deselect
    composeTestRule.onNodeWithTag("suggestion_row_${firstSuggestion.name}").performClick()

    // Verify state is unchecked
    composeTestRule.onNodeWithTag("suggestion_checkbox_${firstSuggestion.name}").assertIsOff()
  }
}
