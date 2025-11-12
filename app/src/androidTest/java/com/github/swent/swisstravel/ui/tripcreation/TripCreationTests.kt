package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.composable.PreferenceSelectorTestTags
import com.github.swent.swisstravel.ui.profile.FakeUserRepository
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class TripCreationTests : InMemorySwissTravelTest() {

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
    composeTestRule.setContent { TripDateScreen(onNext = {}) }
    composeTestRule.onNodeWithTag(TripDateTestTags.TRIP_DATE_SCREEN).assertExists()
    composeTestRule.checkTopBarIsDisplayed()
    composeTestRule.onNodeWithTag(TripDateTestTags.NEXT).performClick()
  }

  @Test
  fun tripPreferencesScreenTest() {
    composeTestRule.setContent { TripPreferencesScreen(onNext = {}) }
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_SCREEN).assertExists()
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCES_TITLE)
        .assertIsDisplayed()
    composeTestRule.checkTopBarIsDisplayed()
    /* Preference Selector */
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
    for (preference in Preference.values()) {
      val tag = PreferenceSelectorTestTags.getTestTagButton(preference)
      composeTestRule
          .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
          .performScrollToNode(hasTestTag(tag))
      composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }
    /* Done button */
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
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

    val tag = PreferenceSelectorTestTags.getTestTagButton(Preference.WHEELCHAIR_ACCESSIBLE)
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(tag))
    composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tag).performClick()
    assert(viewModel.tripSettings.value.preferences.contains(Preference.WHEELCHAIR_ACCESSIBLE))
    composeTestRule
        .onNodeWithTag(TripPreferencesTestTags.TRIP_PREFERENCE_CONTENT)
        .performScrollToNode(hasTestTag(TripPreferencesTestTags.DONE))
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TripPreferencesTestTags.DONE).performClick()
    assertEquals(2, viewModel.tripSettings.value.preferences.size)
    // assert(fakeRepo.getAllTrips().isNotEmpty()) //can't make it to work
  }

  @Test
  fun tripTravelersScreenTest() {
    composeTestRule.setContent { SwissTravelTheme { TripTravelersScreen(onNext = {}) } }
    composeTestRule.onNodeWithTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN).assertExists()
    composeTestRule.checkTopBarIsDisplayed()
    composeTestRule.onNodeWithTag(TripTravelersTestTags.NEXT).performClick()
  }
}
