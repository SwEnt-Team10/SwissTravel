package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.model.map.LocationRepository
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.DestinationTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.LocationTextTestTags
import com.github.swent.swisstravel.ui.mytrips.FakeTripsRepository
import com.github.swent.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.ADD_FIRST_DESTINATION
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.FIRST_DESTINATIONS_TITLE
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Fake repository for testing
class FakeLocationRepository : LocationRepository {
  val locations =
      listOf(
          Location(name = "Lausanne", coordinate = Coordinate(46.5197, 6.6323)),
          Location(name = "Geneva", coordinate = Coordinate(46.2044, 6.1432)),
          Location(name = "Zurich", coordinate = Coordinate(47.3769, 8.5417)))

  override suspend fun search(query: String): List<Location> {
    return if (query.isNotEmpty()) {
      locations.filter { it.name.contains(query, ignoreCase = true) }
    } else {
      emptyList()
    }
  }
}

class FirstDestinationsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val fakeLocationRepository = FakeLocationRepository()

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
          destinationViewModelFactory = { _ ->
            DestinationTextFieldViewModel(fakeLocationRepository)
          })
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
        .onAllNodesWithTag(LocationTextTestTags.INPUT_LOCATION)
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
    val destinationViewModel = DestinationTextFieldViewModel(fakeLocationRepository)

    composeTestRule.setContent {
      FirstDestinationScreen(
          viewModel = tripSettingsViewModel,
          onNext = { onNextCalled = true },
          onPrevious = {},
          destinationViewModelFactory = { _ -> destinationViewModel })
    }

    // 1. Add the UI for the text field
    composeTestRule.onNodeWithTag(ADD_FIRST_DESTINATION).performClick()
    composeTestRule.waitForIdle()

    // 2. Find the input field and type text into it.
    val inputNode = composeTestRule.onAllNodesWithTag(LocationTextTestTags.INPUT_LOCATION).onFirst()
    inputNode.performTextInput("Lausanne")

    // Wait until the ViewModel's state contains the suggestions. This is the key fix.
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      destinationViewModel.addressState.value.locationSuggestions.isNotEmpty()
    }

    // 3. Find the suggestion and click it.
    composeTestRule
        .onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION)
        .onFirst()
        .performClick()
    composeTestRule.waitForIdle()

    // 4. Assert the button is now enabled
    composeTestRule.onNodeWithTag(NEXT_BUTTON).assertIsEnabled()

    // 5. Click the "Next" button
    composeTestRule.onNodeWithTag(NEXT_BUTTON).performClick()

    // 6. Assert the results
    assertTrue("onNext should have been called", onNextCalled)
    val finalDestinations = tripSettingsViewModel.tripSettings.value.destinations
    val expectedLocation = fakeLocationRepository.locations.first()

    assertEquals(1, finalDestinations.size)
    assertEquals(expectedLocation.name, finalDestinations.first().name)
  }
}
