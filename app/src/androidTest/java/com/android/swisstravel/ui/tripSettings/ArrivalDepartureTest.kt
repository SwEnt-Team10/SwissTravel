package com.android.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.swisstravel.ui.geocoding.FakeAddressTextFieldViewModel
import com.android.swisstravel.ui.mytrips.FakeTripsRepository
import com.android.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.AddressTextTestTags
import com.github.swent.swisstravel.ui.tripsettings.ArrivalDepartureScreen
import com.github.swent.swisstravel.ui.tripsettings.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripsettings.TripSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArrivalDepartureTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val fakeTripSettingsViewModel =
      TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository())
  private val fakeArrivalVm = FakeAddressTextFieldViewModel()
  private val fakeDepartureVm = FakeAddressTextFieldViewModel()

  @Test
  fun arrivalDepartureScreenIsDisplayed() {
    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = {},
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun selectingSuggestionUpdatesState() {
    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = {},
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }
    // First, find the specific arrival input field and type into it
    composeTestRule
        .onNode(hasTestTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD), useUnmergedTree = true)
        .performTextInput("test")
    composeTestRule.waitForIdle()
    // Now, click the first suggestion that appears. Indexing is okay here
    // because the suggestions are in a predictable order within the dropdown.
    composeTestRule
        .onAllNodesWithTag(AddressTextTestTags.LOCATION_SUGGESTION)
        .onFirst()
        .performClick()
    assert(fakeArrivalVm.addressState.value.selectedLocation!!.name == "Lausanne")
  }

  @Test
  fun clickingNextButtonTriggersOnNextAndSavesTripProfile() {
    var onNextCalled = false
    val tripSettingsViewModel = TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository())

    // Define the locations that will be "selected"
    val arrivalLocation = Location(Coordinate(46.5197, 6.6323), "Lausanne")
    val departureLocation = Location(Coordinate(46.2044, 6.1432), "Geneva")

    // Pre-set the state in the fake ViewModels as if the user has selected them
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(departureLocation)

    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = tripSettingsViewModel,
          onNext = { onNextCalled = true },
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }

    // Click the button to trigger saveTrip()
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()

    // Assert that the onNext lambda was called
    assert(onNextCalled)

    // Assert that the ViewModel's internal tripProfile is correctly updated
    val finalTripProfile = tripSettingsViewModel.tripSettings.value.arrivalDeparture
    assertEquals(arrivalLocation, finalTripProfile.arrivalLocation)
    assertEquals(departureLocation, finalTripProfile.departureLocation)
  }
}
