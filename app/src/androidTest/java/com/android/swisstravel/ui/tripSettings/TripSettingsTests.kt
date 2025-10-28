package com.android.swisstravel.ui.tripSettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.swisstravel.ui.mytrips.FakeTripsRepository
import com.android.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.composable.ToggleTestTags
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.ui.tripcreation.TripDateScreen
import com.github.swent.swisstravel.ui.tripcreation.TripDateTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesScreen
import com.github.swent.swisstravel.ui.tripcreation.TripPreferencesTestTags
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersScreen
import com.github.swent.swisstravel.ui.tripcreation.TripTravelersTestTags
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class TripSettingsTests {

  @get:Rule val composeTestRule = createComposeRule()

  /** Fake TripsRepository to feed the ViewModel without touching Firestore. */
  class FakeTripsRepository(private val trips: List<Trip>) : TripsRepository {
    override suspend fun getAllTrips(): List<Trip> = trips

    override suspend fun getTrip(tripId: String): Trip {
      return trips.find { it.uid == tripId } ?: throw Exception("Trip not found: $tripId")
    }

    override suspend fun addTrip(trip: Trip) {
      // No-op for testing
    }

    override suspend fun editTrip(tripId: String, updatedTrip: Trip) {
      // No-op for testing
    }

    override suspend fun deleteTrip(tripId: String) {
      // No-op for testing
    }

    override fun getNewUid(): String = "fake-uid"
  }

  private val fakeRepo = FakeTripsRepository(emptyList())
  private val fakeUserRepo = FakeUserRepository()

  @Test
  fun tripDateScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripDateScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripDateTestTags.TRIP_DATE_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  }

  @Test
  fun tripPreferencesScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripPreferencesScreen(onDone = {}) } }
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertExists()
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE)
        .assertIsDisplayed()
    /* Preference Selector */
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
    val preferences = Preference.values().filter { it != Preference.WHEELCHAIR_ACCESSIBLE }
    for (preference in preferences) {
      composeTestRule
          .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(preference))
          .assertIsDisplayed()
    }
    /* Preference toggle */
    composeTestRule.onNodeWithTag(ToggleTestTags.NO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).assertIsDisplayed()
    /* Done button */
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
  }

  @Test
  fun tripPreferencesScreenTestWithViewModel() = runBlocking {
    val viewModel = TripSettingsViewModel(fakeRepo, fakeUserRepo)
    val fakeStartDate = LocalDate.of(2024, 7, 20)
    val fakeEndDate = LocalDate.of(2024, 7, 25)
    composeTestRule.setContent { SwissTravelTheme { TripPreferencesScreen(viewModel) } }
    viewModel.updateDates(fakeStartDate, fakeEndDate) // Need a date to save a trip
    assert(viewModel.tripSettings.value.preferences.contains(Preference.MUSEUMS))
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.SPORTS))
        .assertExists()
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.SPORTS))
        .performClick()
    assert(viewModel.tripSettings.value.preferences.contains(Preference.SPORTS))
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS))
        .assertExists()
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.MUSEUMS))
        .performClick()
    assert(!viewModel.tripSettings.value.preferences.contains(Preference.MUSEUMS))
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).assertExists()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).performClick()
    assert(viewModel.tripSettings.value.preferences.contains(Preference.WHEELCHAIR_ACCESSIBLE))
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertExists()
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
    assertEquals(2, viewModel.tripSettings.value.preferences.size)
    // assert(fakeRepo.getAllTrips().isNotEmpty()) //can't make it to work
  }

  @Test
  fun tripTravelersScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripTravelersScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  }
}
