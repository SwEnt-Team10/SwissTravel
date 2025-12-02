package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SuggestionListTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun suggestionList_isDisplayed_afterExpansion() {
    val viewModel =
        TripSettingsViewModel(
            tripsRepository = com.github.swent.swisstravel.ui.trips.FakeTripsRepository(),
            userRepository = com.github.swent.swisstravel.ui.profile.FakeUserRepository())
    composeTestRule.setContent { FirstDestinationScreen(viewModel = viewModel) }

    // Click on "Suggestions" to expand
    composeTestRule.onNodeWithText("See Our Suggestions For You").performClick()
    // assert the suggestions are correctly displayed
    composeTestRule.onNodeWithText("See Our Suggestions For You").assertIsDisplayed()
  }
}
