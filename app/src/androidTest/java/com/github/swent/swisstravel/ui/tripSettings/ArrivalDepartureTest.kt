package com.github.swent.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.FakeAddressTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.LocationTextTestTags
import com.github.swent.swisstravel.ui.mytrips.FakeTripsRepository
import com.github.swent.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureScreen
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        .onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION)
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

  @Test
  fun arrivalAndDepartureAreMandatory() {
    val fakeArrivalVm = FakeAddressTextFieldViewModel()
    val fakeDepartureVm = FakeAddressTextFieldViewModel()
    // Define the locations that will be "selected"
    val arrivalLocation = Location(Coordinate(46.5197, 6.6323), "Lausanne")
    val departureLocation = Location(Coordinate(46.2044, 6.1432), "Geneva")
    var onNextCalled = false

    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = { onNextCalled = true },
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }

    // should not trigger on next if both are empty
    fakeArrivalVm.setLocation(null)
    fakeDepartureVm.setLocation(null)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should not trigger on next if one is empty
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(null)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should not trigger on next if the other is empty
    fakeArrivalVm.setLocation(null)
    fakeDepartureVm.setLocation(departureLocation)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should trigger on next if both are filled
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(departureLocation)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertTrue(onNextCalled)
  }
}
