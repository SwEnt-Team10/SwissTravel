package com.android.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.ui.geocoding.AddressTextTestTags
import com.github.swent.swisstravel.ui.geocoding.FakeAddressTextFieldViewModel
import com.github.swent.swisstravel.ui.mytrips.FakeTripsRepository
import com.github.swent.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.ui.tripcreation.FirstDestinationScreen
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.ADD_FIRST_DESTINATION
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.FIRST_DESTINATIONS_TITLE
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FirstDestinationsTest {
  @get:Rule val composeTestRule = createComposeRule()

  // A single, controllable fake ViewModel for all generated text fields in a test
  private val fakeAddressVm = FakeAddressTextFieldViewModel()

  private fun setContent(
      viewModel: TripSettingsViewModel =
          TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository()),
      onNext: () -> Unit = {},
      onPrevious: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      FirstDestinationScreen(
          viewModel = viewModel,
          onNext = onNext,
          onPrevious = onPrevious,
          // Inject the factory to return our fake ViewModel
          addressViewModelFactory = { _ -> fakeAddressVm })
    }
  }

  @Test
  fun screenIsDisplayedCorrectly() {
    setContent()
    composeTestRule.onNodeWithTag(FIRST_DESTINATIONS_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NEXT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RETURN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun nextButtonIsEnabledInitially() {
    setContent()
    composeTestRule.onNodeWithTag(NEXT_BUTTON).assertIsEnabled()
  }

  @Test
  fun addDestinationButtonAddsNewTextField() {
    setContent()
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).performClick()
    composeTestRule
        .onAllNodesWithTag(AddressTextTestTags.INPUT_LOCATION)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun addDestinationButtonIsDisabledWhenDestinationIsAddedButEmpty() {
    setContent()
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).assertIsNotEnabled()
  }

  @Test
  fun clickingNextUpdatesViewModelAndTriggersOnNext() {
    var onNextCalled = false
    val tripSettingsViewModel = TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository())
    setContent(viewModel = tripSettingsViewModel, onNext = { onNextCalled = true })

    // 1. Add the UI for the text field
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).performClick()
    composeTestRule.waitForIdle()

    // 2. Find the input field and type text into it.
    // This will trigger the fake ViewModel to produce suggestions.
    val inputNode = composeTestRule.onAllNodesWithTag(AddressTextTestTags.INPUT_LOCATION).onFirst()
    inputNode.performTextInput("Lausanne")
    composeTestRule.waitForIdle() // Let debounce and suggestion UI appear

    // 3. Find the suggestion and click it. This is the step that was missing.
    // This click triggers onLocationSelected in AddressAutocompleteTextField.
    composeTestRule
        .onAllNodesWithTag(AddressTextTestTags.LOCATION_SUGGESTION)
        .onFirst()
        .performClick()
    composeTestRule.waitForIdle() // Let the state update after the click

    // 4. Assert the button is now enabled
    composeTestRule.onNodeWithTag(NEXT_BUTTON).assertIsEnabled()

    // 5. Click the "Next" button
    composeTestRule.onNodeWithTag(NEXT_BUTTON).performClick()

    // 6. Assert the results
    assertTrue("onNext should have been called", onNextCalled)
    val finalDestinations = tripSettingsViewModel.tripSettings.value.destinations
    val expectedLocation =
        fakeAddressVm.addressState.value.locationSuggestions
            .first() // Get the location from the fake

    assertEquals(1, finalDestinations.size)
    assertEquals(expectedLocation, finalDestinations.first()) // This will now pass correctly
  }
}
